package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadonaScrapingService
{

    private final MercadonaScraper mercadonaScraper;

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

    public List<ScrapedProduct> getProductsByCategory(String categoryId)
    {
        return mercadonaScraper.scrapeCategory(categoryId);
    }

    public List<ScrapedProduct> searchInCategory(String categoryId, String query)
    {
        List<ScrapedProduct> products = mercadonaScraper.scrapeCategory(categoryId);
        String queryLower = query.toLowerCase();

        return products.stream()
            .filter(p -> p.name() != null && p.name().toLowerCase().contains(queryLower))
            .toList();
    }

    public List<MercadonaScraper.PublicCategoryInfo> getCategories()
    {
        return mercadonaScraper.fetchAllCategories();
    }

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

    public boolean isEnabled()
    {
        return mercadonaScraper.isEnabled();
    }

    public MercadonaScraper.ProductDetail getProductDetail(String productId)
    {
        return mercadonaScraper.fetchProductDetail(productId);
    }

    public java.util.Map<String, MercadonaScraper.ProductDetail> getProductDetails(List<String> productIds)
    {
        return mercadonaScraper.fetchProductDetails(productIds);
    }

    public List<ScrapedProduct> enrichWithEan(List<ScrapedProduct> products)
    {
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

        var details = mercadonaScraper.fetchProductDetails(idsWithoutEan);

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
