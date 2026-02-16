package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.service.MercadonaScrapingService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import com.smartcart.smartcart.modules.scraping.service.ScrapingJobRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping/mercadona")
@RequiredArgsConstructor
public class ScrapingAdminController
{

    private final MercadonaScrapingService mercadonaService;
    private final ProductSyncService productSyncService;
    private final ScrapeLogService scrapeLogService;
    private final StoreRepository storeRepository;
    private final ScrapingJobRegistry jobRegistry;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        return ResponseEntity.ok(Map.of(
            "store", "mercadona",
            "enabled", mercadonaService.isEnabled()
        ));
    }

    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll()
    {
        log.info("Sincronizando todos los productos de Mercadona");

        Store store = storeRepository.findBySlug("mercadona")
                .orElseThrow(() -> new RuntimeException("Store not found: mercadona"));
        ScrapeLog scrapeLog = scrapeLogService.startLog(store);

        jobRegistry.register("mercadona");
        try {
            ScrapingResult scrapingResult = mercadonaService.scrapeAll();

            if (!scrapingResult.getErrors().isEmpty()) {
                scrapeLogService.addErrorsFromScrapingResult(scrapeLog, scrapingResult.getErrors());
            }

            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "mercadona");

            scrapeLogService.completeLog(scrapeLog, scrapingResult.getTotalProducts(), syncResult);

            return ResponseEntity.ok(Map.of(
                "scraped", scrapingResult.getTotalProducts(),
                "scrapingErrors", scrapingResult.getTotalErrors(),
                "created", syncResult.created,
                "updated", syncResult.updated,
                "unchanged", syncResult.unchanged,
                "syncErrors", syncResult.errors,
                "durationSeconds", scrapingResult.getDurationSeconds()
            ));
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Scraping de Mercadona cancelado por el administrador");
                scrapeLogService.cancelLog(scrapeLog);
                return ResponseEntity.ok(Map.of("status", "CANCELLED"));
            }
            log.error("Error during Mercadona scraping: {}", e.getMessage(), e);
            scrapeLogService.failLog(scrapeLog, e.getMessage());
            throw e;
        } finally {
            jobRegistry.deregister("mercadona");
        }
    }

    @PostMapping("/enrich")
    public ResponseEntity<Map<String, Object>> enrichProductsWithEan(
            @RequestParam(value = "limit", defaultValue = "100") int limit)
    {
        log.info("Enriqueciendo productos sin EAN (limite: {})", limit);

        var productsWithoutEan = productSyncService.findProductsWithoutEan(1);

        if (productsWithoutEan.isEmpty())
        {
            return ResponseEntity.ok(Map.of(
                "message", "No hay productos sin EAN para enriquecer",
                "total", 0
            ));
        }

        var productsToEnrich = productsWithoutEan.stream()
                .limit(limit)
                .toList();

        List<String> externalIds = productsToEnrich.stream()
                .map(ps -> ps.getExternaId())
                .filter(id -> id != null)
                .toList();

        log.info("Obteniendo detalles de {} productos...", externalIds.size());
        var details = mercadonaService.getProductDetails(externalIds);

        ProductSyncService.EnrichResult result = productSyncService.enrichProductsWithEan(productsToEnrich, details);

        return ResponseEntity.ok(Map.of(
            "totalWithoutEan", productsWithoutEan.size(),
            "processed", productsToEnrich.size(),
            "enriched", result.enriched,
            "noEanAvailable", result.noEan,
            "notFound", result.notFound,
            "errors", result.errors,
            "remaining", productsWithoutEan.size() - productsToEnrich.size()
        ));
    }
}
