package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper;
import com.smartcart.smartcart.modules.scraping.service.MercadonaScrapingService;
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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus()
    {
        return ResponseEntity.ok(Map.of(
            "store", "mercadona",
            "enabled", mercadonaService.isEnabled()
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<MercadonaScraper.PublicCategoryInfo>> getCategories()
    {
        log.info("Obteniendo lista de categorias");
        return ResponseEntity.ok(mercadonaService.getCategories());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ScrapedProduct>> getProductsByCategory(
            @PathVariable String categoryId)
    {
        log.info("Obteniendo productos de categoria {}", categoryId);
        return ResponseEntity.ok(mercadonaService.getProductsByCategory(categoryId));
    }

    @GetMapping("/category/{categoryId}/search")
    public ResponseEntity<List<ScrapedProduct>> searchInCategory(
            @PathVariable String categoryId,
            @RequestParam("q") String query)
    {
        log.info("Buscando '{}' en categoria {}", query, categoryId);
        return ResponseEntity.ok(mercadonaService.searchInCategory(categoryId, query));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ScrapedProduct>> searchAllProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "categoryName", required = false) String categoryName)
    {
        log.info("Buscando '{}' (categoryName={})", query, categoryName);
        return ResponseEntity.ok(mercadonaService.searchAllProducts(query, categoryName));
    }

    @PostMapping("/run")
    public ResponseEntity<ScrapingResult> runFullScraping()
    {
        log.info("Iniciando scraping completo");
        return ResponseEntity.ok(mercadonaService.scrapeAll());
    }
}
