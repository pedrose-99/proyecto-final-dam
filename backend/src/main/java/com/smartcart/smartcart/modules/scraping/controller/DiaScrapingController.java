package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import com.smartcart.smartcart.modules.scraping.service.PythonScraperService;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping/dia")
@RequiredArgsConstructor
public class DiaScrapingController
{

    private final PythonScraperService pythonScraperService;
    private final ProductSyncService productSyncService;
    private final ScrapeLogService scrapeLogService;
    private final StoreRepository storeRepository;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        return ResponseEntity.ok(Map.of(
            "store", "dia",
            "healthy", pythonScraperService.isHealthy()
        ));
    }

    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll()
    {
        log.info("Sincronizando todos los productos de Dia");

        Store store = storeRepository.findBySlug("dia")
                .orElseThrow(() -> new RuntimeException("Store not found: dia"));
        ScrapeLog scrapeLog = scrapeLogService.startLog(store);

        try {
            ScrapingResult scrapingResult = pythonScraperService.scrapeDia();

            if (!scrapingResult.getErrors().isEmpty()) {
                scrapeLogService.addErrorsFromScrapingResult(scrapeLog, scrapingResult.getErrors());
            }

            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "dia");

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
            log.error("Error during Dia scraping: {}", e.getMessage(), e);
            scrapeLogService.failLog(scrapeLog, e.getMessage());
            throw e;
        }
    }
}
