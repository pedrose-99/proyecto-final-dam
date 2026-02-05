package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.AlcampoScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcampoScrapingService
{

    private final AlcampoScraper alcampoScraper;

    public ScrapingResult scrapeAll()
    {
        if (!alcampoScraper.isEnabled())
        {
            log.warn("[alcampo] Scraping deshabilitado");
            ScrapingResult result = new ScrapingResult();
            result.setStoreName("alcampo");
            result.addError("config", "Scraping deshabilitado");
            return result;
        }
        return alcampoScraper.scrape();
    }

    public List<ScrapedProduct> getProductsByCategory(String categoryPath)
    {
        return alcampoScraper.scrapeCategory(categoryPath);
    }

    public List<ScrapedProduct> searchInCategory(String categoryPath, String query)
    {
        List<ScrapedProduct> products = alcampoScraper.scrapeCategory(categoryPath);
        String queryLower = query.toLowerCase();

        return products.stream()
            .filter(p -> p.name() != null && p.name().toLowerCase().contains(queryLower))
            .toList();
    }

    public List<AlcampoScraper.PublicCategoryInfo> getCategories()
    {
        return alcampoScraper.fetchAllCategories();
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
        return alcampoScraper.isEnabled();
    }
}
