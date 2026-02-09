package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.AlcampoScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    public Map<String, AlcampoScraper.ProductDetail> getProductDetails(List<ProductStore> productStores)
    {
        return alcampoScraper.fetchProductDetails(productStores);
    }

    public boolean isEnabled()
    {
        return alcampoScraper.isEnabled();
    }
}
