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
public class MercadonaScrapingService
{

    private final MercadonaScraper mercadonaScraper;

    /**
     * Scraping completo de todas las categorias.
     */
    public ScrapingResult scrapeAll()
    {
        if (!mercadonaScraper.isEnabled())
        {
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
    public List<ScrapedProduct> getProductsByCategory(String categoryId)
    {
        return mercadonaScraper.scrapeCategory(categoryId);
    }

    /**
     * Busca productos por nombre en UNA categoria.
     */
    public List<ScrapedProduct> searchInCategory(String categoryId, String query)
    {
        List<ScrapedProduct> products = mercadonaScraper.scrapeCategory(categoryId);
        String queryLower = query.toLowerCase();

        return products.stream()
            .filter(p -> p.name() != null && p.name().toLowerCase().contains(queryLower))
            .toList();
    }

    /**
     * Obtiene lista de todas las categorias disponibles.
     */
    public List<MercadonaScraper.PublicCategoryInfo> getCategories()
    {
        return mercadonaScraper.fetchAllCategories();
    }

    /**
     * Busca productos por nombre en TODAS las categorias.
     * @param query texto a buscar en el nombre
     * @param categoryName filtro opcional por categoryName (ej: "Aceite de oliva")
     */
    public List<ScrapedProduct> searchAllProducts(String query, String categoryName)
    {
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
    public boolean isEnabled()
    {
        return mercadonaScraper.isEnabled();
    }

    // ========== METODOS PARA EAN ==========

    /**
     * Obtiene el detalle de un producto (incluye EAN, origen, etc.)
     * Usa el endpoint /api/products/{id}/
     *
     * @param productId ID del producto en Mercadona
     * @return Detalle del producto o null si hay error
     */
    public MercadonaScraper.ProductDetail getProductDetail(String productId)
    {
        return mercadonaScraper.fetchProductDetail(productId);
    }

    /**
     * Obtiene EAN y detalles para multiples productos.
     * Util para enriquecer productos que no tienen EAN en la BD.
     *
     * @param productIds Lista de IDs de productos sin EAN
     * @return Mapa de productId -> ProductDetail
     */
    public java.util.Map<String, MercadonaScraper.ProductDetail> getProductDetails(List<String> productIds)
    {
        return mercadonaScraper.fetchProductDetails(productIds);
    }

    /**
     * Enriquece una lista de ScrapedProducts con EAN y origen.
     * Solo hace peticiones para productos que no tienen EAN.
     *
     * @param products Lista de productos a enriquecer
     * @return Lista de productos con EAN (los que se pudieron obtener)
     */
    public List<ScrapedProduct> enrichWithEan(List<ScrapedProduct> products)
    {
        // Filtrar productos sin EAN
        List<String> idsWithoutEan = products.stream()
            .filter(p -> p.ean() == null)
            .map(ScrapedProduct::externalId)
            .toList();

        if (idsWithoutEan.isEmpty())
        {
            log.info("[mercadona] Todos los productos ya tienen EAN");
            return products;
        }

        log.info("[mercadona] Obteniendo EAN para {} productos", idsWithoutEan.size());

        // Obtener detalles
        var details = mercadonaScraper.fetchProductDetails(idsWithoutEan);

        // Crear nueva lista con EAN
        return products.stream()
            .map(p -> {
                var detail = details.get(p.externalId());
                if (detail != null && detail.ean() != null)
                {
                    return ScrapedProduct.builder()
                        .externalId(p.externalId())
                        .ean(detail.ean())
                        .name(p.name())
                        .brand(p.brand())
                        .description(p.description())
                        .price(p.price())
                        .originalPrice(p.originalPrice())
                        .onSale(p.onSale())
                        .pricePerUnit(p.pricePerUnit())
                        .unit(p.unit())
                        .imageUrl(p.imageUrl())
                        .productUrl(p.productUrl())
                        .categoryName(p.categoryName())
                        .categoryId(p.categoryId())
                        .origin(detail.origin())
                        .build();
                }
                return p;
            })
            .toList();
    }
}
