package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.AlcampoScraper;
import com.smartcart.smartcart.modules.scraping.service.AlcampoScrapingService;
import com.smartcart.smartcart.modules.scraping.service.ProductEanEnricherService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping/alcampo")
@RequiredArgsConstructor
public class AlcampoScrapingController
{

    private final AlcampoScrapingService alcampoService;
    private final ProductSyncService productSyncService;
    private final ProductEanEnricherService eanEnricherService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        return ResponseEntity.ok(Map.of(
            "store", "alcampo",
            "enabled", alcampoService.isEnabled()
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<AlcampoScraper.PublicCategoryInfo>> getCategories()
    {
        log.info("Obteniendo lista de categorias de Alcampo");
        return ResponseEntity.ok(alcampoService.getCategories());
    }

    @GetMapping("/category")
    public ResponseEntity<List<ScrapedProduct>> getProductsByCategory(
            @RequestParam("path") String categoryPath)
    {
        log.info("Obteniendo productos de categoria {}", categoryPath);
        return ResponseEntity.ok(alcampoService.getProductsByCategory(categoryPath));
    }

    @GetMapping("/category/search")
    public ResponseEntity<List<ScrapedProduct>> searchInCategory(
            @RequestParam("path") String categoryPath,
            @RequestParam("q") String query)
    {
        log.info("Buscando '{}' en categoria {}", query, categoryPath);
        return ResponseEntity.ok(alcampoService.searchInCategory(categoryPath, query));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ScrapedProduct>> searchAllProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "categoryName", required = false) String categoryName)
    {
        log.info("Buscando '{}' (categoryName={})", query, categoryName);
        return ResponseEntity.ok(alcampoService.searchAllProducts(query, categoryName));
    }

    @PostMapping("/run")
    public ResponseEntity<ScrapingResult> runFullScraping()
    {
        log.info("Iniciando scraping completo de Alcampo");
        return ResponseEntity.ok(alcampoService.scrapeAll());
    }

    @PostMapping("/run/async")
    public CompletableFuture<ResponseEntity<ScrapingResult>> runFullScrapingAsync()
    {
        log.info("Iniciando scraping completo ASYNC de Alcampo");
        return CompletableFuture.supplyAsync(() -> {
            ScrapingResult result = alcampoService.scrapeAll();
            return ResponseEntity.ok(result);
        });
    }

    @PostMapping("/sync/category")
    public ResponseEntity<Map<String, Object>> syncCategory(@RequestParam("path") String categoryPath)
    {
        log.info("Sincronizando categoria {} de Alcampo", categoryPath);

        List<ScrapedProduct> products = alcampoService.getProductsByCategory(categoryPath);
        ProductSyncService.SyncResult result = productSyncService.syncProducts(products, "alcampo");

        return ResponseEntity.ok(Map.of(
            "categoryPath", categoryPath,
            "scraped", products.size(),
            "created", result.created,
            "updated", result.updated,
            "unchanged", result.unchanged,
            "errors", result.errors
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

    @PostMapping("/sync/all/async")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> syncAllAsync()
    {
        log.info("Iniciando sincronizacion ASYNC de todos los productos de Alcampo");

        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    /**
     * Enriquece productos de Alcampo con EAN de Open Food Facts
     */
    @PostMapping("/enrich-ean")
    public ResponseEntity<Map<String, Object>> enrichEan()
    {
        log.info("Iniciando enriquecimiento de EAN para productos de Alcampo");

        // Alcampo es store_id = 4
        ProductEanEnricherService.EnrichmentResult result = eanEnricherService.enrichProductsForStore(4);

        return ResponseEntity.ok(Map.of(
            "store", "alcampo",
            "enriched", result.enriched(),
            "notFound", result.notFound(),
            "errors", result.errors()
        ));
    }

    /**
     * Enriquece productos de Alcampo con EAN de forma asíncrona
     */
    @PostMapping("/enrich-ean/async")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> enrichEanAsync()
    {
        log.info("Iniciando enriquecimiento ASYNC de EAN para productos de Alcampo");

        return CompletableFuture.supplyAsync(() -> {
            ProductEanEnricherService.EnrichmentResult result = eanEnricherService.enrichProductsForStore(4);

            return ResponseEntity.ok(Map.of(
                "store", "alcampo",
                "enriched", result.enriched(),
                "notFound", result.notFound(),
                "errors", result.errors()
            ));
        });
    }
}
