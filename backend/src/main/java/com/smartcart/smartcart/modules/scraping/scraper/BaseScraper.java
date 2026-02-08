package com.smartcart.smartcart.modules.scraping.scraper;

import com.smartcart.smartcart.modules.scraping.config.ScrapingConfig;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.util.RateLimiter;
import com.smartcart.smartcart.modules.scraping.util.UserAgentRotator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public abstract class BaseScraper
{

    @Autowired
    protected RateLimiter rateLimiter;

    @Autowired
    protected UserAgentRotator userAgentRotator;

    @Autowired
    protected ScrapingConfig scrapingConfig;

    protected int timeoutMs = 30000;
    protected int maxRetries = 3;

    public abstract String getStoreName();

    public abstract Long getStoreId();

    public abstract ScrapingResult scrape();

    protected abstract List<String> getCategoryUrls();

    public boolean isEnabled()
    {
        return scrapingConfig.isEnabled() && scrapingConfig.isStoreEnabled(getStoreName());
    }

    protected BigDecimal parsePrice(String priceText)
    {
        if (priceText == null || priceText.isBlank())
        {
            return null;
        }

        String cleaned = priceText
                .replaceAll("[€$]", "")
                .replaceAll("\\s", "")
                .replace(",", ".")
                .trim();

        try
        {
            return new BigDecimal(cleaned);
        }
        catch (NumberFormatException e)
        {
            log.warn("[{}] No se pudo parsear precio: {}", getStoreName(), priceText);
            return null;
        }
    }

    protected String normalizeName(String name)
    {
        if (name == null) return "";

        return name.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    protected String extractPricePerUnit(String text)
    {
        if (text == null || text.isBlank())
        {
            return null;
        }
        return text.replaceAll("\\s+", "").trim();
    }

    protected void initFromConfig()
    {
        this.timeoutMs = scrapingConfig.getTimeoutSeconds() * 1000;
        this.maxRetries = scrapingConfig.getMaxRetries();
    }
}
