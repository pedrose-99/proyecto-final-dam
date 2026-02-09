package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.AlcampoScraper;
import com.smartcart.smartcart.modules.scraping.service.AlcampoScrapingService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @PostMapping("/enrich-ean")
    public ResponseEntity<Map<String, Object>> enrichEan(
            @RequestParam(value = "limit", defaultValue = "100") int limit)
    {
        log.info("Iniciando enriquecimiento de EAN para productos de Alcampo (limite: {})", limit);

        List<ProductStore> productsWithoutEan = productSyncService.findProductsWithoutEan(4);

        if (productsWithoutEan.isEmpty())
        {
            return ResponseEntity.ok(Map.of(
                "store", "alcampo",
                "message", "No hay productos sin EAN para enriquecer",
                "totalWithoutEan", 0
            ));
        }

        List<ProductStore> productsToEnrich = productsWithoutEan.stream()
            .limit(limit)
            .toList();

        Map<String, AlcampoScraper.ProductDetail> details =
            alcampoService.getProductDetails(productsToEnrich);

        Map<String, String> eanMap = details.entrySet().stream()
            .filter(e -> e.getValue().ean() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().ean()));

        ProductSyncService.EnrichResult result =
            productSyncService.enrichProductsWithEanMap(productsToEnrich, eanMap);

        Map<String, Object> response = new HashMap<>();
        response.put("store", "alcampo");
        response.put("totalWithoutEan", productsWithoutEan.size());
        response.put("processed", productsToEnrich.size());
        response.put("enriched", result.enriched);
        response.put("noEanAvailable", result.noEan);
        response.put("notFound", result.notFound);
        response.put("errors", result.errors);
        response.put("remaining", productsWithoutEan.size() - productsToEnrich.size());

        return ResponseEntity.ok(response);
    }
}
