package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.service.MercadonaScrapingService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
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

        ScrapingResult scrapingResult = mercadonaService.scrapeAll();
        ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
            scrapingResult.getProducts(), "mercadona");

        return ResponseEntity.ok(Map.of(
            "scraped", scrapingResult.getTotalProducts(),
            "scrapingErrors", scrapingResult.getTotalErrors(),
            "created", syncResult.created,
            "updated", syncResult.updated,
            "unchanged", syncResult.unchanged,
            "syncErrors", syncResult.errors,
            "durationSeconds", scrapingResult.getDurationSeconds()
        ));
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
