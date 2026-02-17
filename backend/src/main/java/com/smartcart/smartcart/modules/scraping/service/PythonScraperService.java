package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PythonScraperService
{

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PythonScraperService(
        RestTemplateBuilder restTemplateBuilder,
        @Value("${smartcart.scraping.python-scraper.base-url}") String baseUrl
    )
    {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(10))
            .build();
    }

    public ScrapingResult scrapeDia()
    {
        return callScraper("dia");
    }

    public ScrapingResult scrapeCarrefour()
    {
        return callScraper("carrefour");
    }

    public ScrapingResult scrapeAhorramas()
    {
        return callScraper("ahorramas");
    }

    private ScrapingResult callScraper(String store)
    {
        ScrapingResult result = new ScrapingResult();
        result.setStoreName(store);
        result.setStartTime(LocalDateTime.now());

        try
        {
            String url = baseUrl + "/scrape/" + store;
            log.info("[{}] Llamando a scraper Python: {}", store, url);

            ResponseEntity<List<ScrapedProduct>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<>() {}
            );

            List<ScrapedProduct> products = response.getBody();
            if (products != null)
            {
                for (ScrapedProduct p : products)
                {
                    result.addProduct(p);
                }
                result.setTotalProducts(products.size());
            }

            log.info("[{}] Scraper Python devolvio {} productos", store,
                     result.getTotalProducts());
        }
        catch (Exception e)
        {
            log.error("[{}] Error llamando a scraper Python: {}", store, e.getMessage(), e);
            result.addError("python-scraper", e.getMessage());
            result.setTotalErrors(1);
        }

        result.setEndTime(LocalDateTime.now());
        return result;
    }

    public boolean isHealthy()
    {
        try
        {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        }
        catch (Exception e)
        {
            log.warn("Python scraper no disponible: {}", e.getMessage());
            return false;
        }
    }
}
