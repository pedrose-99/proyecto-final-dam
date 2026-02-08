package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.CarrefourScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrefourScrapingService
{

    private final CarrefourScraper carrefourScraper;

    public ScrapingResult scrapeAll()
    {
        if (!carrefourScraper.isEnabled())
        {
            log.warn("[carrefour] Scraping deshabilitado");
            ScrapingResult result = new ScrapingResult();
            result.setStoreName("carrefour");
            result.addError("config", "Scraping deshabilitado");
            return result;
        }
        return carrefourScraper.scrape();
    }

    public Map<String, CarrefourScraper.ProductDetail> getProductDetails(List<ProductStore> productStores)
    {
        return carrefourScraper.fetchProductDetails(productStores);
    }

    public boolean isEnabled()
    {
        return carrefourScraper.isEnabled();
    }
}
