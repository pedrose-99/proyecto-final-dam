package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.service.AlcampoScrapingService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping/alcampo")
@RequiredArgsConstructor
public class AlcampoScrapingController
{

    private final AlcampoScrapingService alcampoService;
    private final ProductSyncService productSyncService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        return ResponseEntity.ok(Map.of(
            "store", "alcampo",
            "enabled", alcampoService.isEnabled()
        ));
    }

    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAll()
    {
        log.info("Sincronizando todos los productos de Alcampo");

        ScrapingResult scrapingResult = alcampoService.scrapeAll();
        ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
            scrapingResult.getProducts(), "alcampo");

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
}
