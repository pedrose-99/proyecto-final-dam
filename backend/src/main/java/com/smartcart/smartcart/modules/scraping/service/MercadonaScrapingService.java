package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para gestionar el scraping de Mercadona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadonaScrapingService {

    private final MercadonaScraper mercadonaScraper;

    /**
     * Scraping completo de todas las categorias.
     */
    public ScrapingResult scrapeAll() {
        if (!mercadonaScraper.isEnabled()) {
            log.warn("[mercadona] Scraping deshabilitado");
            ScrapingResult result = new ScrapingResult();
            result.setStoreName("mercadona");
            result.addError("config", "Scraping deshabilitado");
            return result;
        }
        return mercadonaScraper.scrape();
    }

    /**
     * Obtiene productos de UNA categoria especifica (rapido).
     * Va directo a la API de esa categoria.
     */
    public List<ScrapedProduct> getProductsByCategory(String categoryId) {
        return mercadonaScraper.scrapeCategory(categoryId);
    }

    /**
     * Busca productos por nombre en UNA categoria.
     */
    public List<ScrapedProduct> searchInCategory(String categoryId, String query) {
        List<ScrapedProduct> products = mercadonaScraper.scrapeCategory(categoryId);
        String queryLower = query.toLowerCase();

        return products.stream()
            .filter(p -> p.name() != null && p.name().toLowerCase().contains(queryLower))
            .toList();
    }

    /**
     * Obtiene lista de todas las categorias disponibles.
     */
    public List<MercadonaScraper.PublicCategoryInfo> getCategories() {
        return mercadonaScraper.fetchAllCategories();
    }

    /**
     * Busca productos por nombre en TODAS las categorias.
     * @param query texto a buscar en el nombre
     * @param categoryName filtro opcional por categoryName (ej: "Aceite de oliva")
     */
    public List<ScrapedProduct> searchAllProducts(String query, String categoryName) {
        ScrapingResult result = scrapeAll();
        String queryLower = query.toLowerCase().trim();

        return result.getProducts().stream()
            .filter(p -> p.name() != null && p.name().toLowerCase().contains(queryLower))
            .filter(p -> categoryName == null ||
                        (p.categoryName() != null && p.categoryName().equalsIgnoreCase(categoryName)))
            .toList();
    }

    /**
     * Verifica si el scraper esta habilitado.
     */
    public boolean isEnabled() {
        return mercadonaScraper.isEnabled();
    }
}
