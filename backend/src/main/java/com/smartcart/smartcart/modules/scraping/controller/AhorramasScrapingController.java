package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import com.smartcart.smartcart.modules.scraping.service.PythonScraperService;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import com.smartcart.smartcart.modules.scraping.service.ScrapingJobRegistry;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping/ahorramas")
@RequiredArgsConstructor
public class AhorramasScrapingController
{

    private final PythonScraperService pythonScraperService;
    private final ProductSyncService productSyncService;
    private final ScrapeLogService scrapeLogService;
    private final StoreRepository storeRepository;
    private final ScrapingJobRegistry jobRegistry;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        Store store = storeRepository.findBySlug("ahorramas")
                .orElseThrow(() -> new RuntimeException("Store not found: ahorramas"));
        boolean running = jobRegistry.isRunning("ahorramas");
        Optional<ScrapeLog> lastLog = scrapeLogService.getLastLog(store);

        Map<String, Object> response = new HashMap<>();
        response.put("store", "ahorramas");
        response.put("healthy", pythonScraperService.isHealthy());
        response.put("isRunning", running);
        response.put("lastScrapeStatus", lastLog.map(l -> l.getStatus().name()).orElse(null));
        response.put("lastScrapeTime", lastLog.map(l ->
            l.getEndTime() != null ? l.getEndTime().toString() : l.getStartTime().toString()
        ).orElse(null));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll()
    {
        if (jobRegistry.isRunning("ahorramas")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El scraping ya esta en curso"));
        }

        log.info("Sincronizando todos los productos de Ahorramas");

        Store store = storeRepository.findBySlug("ahorramas")
                .orElseThrow(() -> new RuntimeException("Store not found: ahorramas"));
        ScrapeLog scrapeLog = scrapeLogService.startLog(store);

        jobRegistry.register("ahorramas");
        try {
            ScrapingResult scrapingResult = pythonScraperService.scrapeAhorramas();

            if (!scrapingResult.getErrors().isEmpty()) {
                scrapeLogService.addErrorsFromScrapingResult(scrapeLog, scrapingResult.getErrors());
            }

            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "ahorramas");

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
                log.info("Scraping de Ahorramas cancelado por el administrador");
                scrapeLogService.cancelLog(scrapeLog);
                return ResponseEntity.ok(Map.of("status", "CANCELLED"));
            }
            log.error("Error during Ahorramas scraping: {}", e.getMessage(), e);
            scrapeLogService.failLog(scrapeLog, e.getMessage());
            throw e;
        } finally {
            jobRegistry.deregister("ahorramas");
        }
    }
}
