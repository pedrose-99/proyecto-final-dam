package com.smartcart.smartcart.modules.scraping.scraper;

import com.smartcart.smartcart.modules.scraping.config.ScrapingConfig;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.util.RateLimiter;
import com.smartcart.smartcart.modules.scraping.util.UserAgentRotator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

/**
 * Clase base abstracta para todos los scrapers.
 * Proporciona funcionalidad común como rate limiting, rotación de user agents,
 * y métodos de utilidad para parseo de datos.
 */
@Slf4j
public abstract class BaseScraper {

    @Autowired
    protected RateLimiter rateLimiter;

    @Autowired
    protected UserAgentRotator userAgentRotator;

    @Autowired
    protected ScrapingConfig scrapingConfig;

    protected int timeoutMs = 30000;
    protected int maxRetries = 3;

    /**
     * Nombre identificador del scraper (ej: "mercadona", "carrefour")
     */
    public abstract String getStoreName();

    /**
     * ID de la tienda en la base de datos
     */
    public abstract Long getStoreId();

    /**
     * Ejecuta el scraping completo de la tienda
     */
    public abstract ScrapingResult scrape();

    /**
     * Obtiene lista de URLs o IDs de categorias a scrapear
     */
    protected abstract List<String> getCategoryUrls();

    /**
     * Verifica si este scraper está habilitado en la configuración
     */
    public boolean isEnabled() {
        return scrapingConfig.isEnabled() && scrapingConfig.isStoreEnabled(getStoreName());
    }

    /**
     * Limpia y normaliza el precio extraido
     * "8,99 €" -> 8.99
     */
    protected BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) {
            return null;
        }

        // Quitar simbolos de moneda y espacios
        String cleaned = priceText
                .replaceAll("[€$]", "")
                .replaceAll("\\s", "")
                .replace(",", ".")
                .trim();

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("[{}] No se pudo parsear precio: {}", getStoreName(), priceText);
            return null;
        }
    }

    /**
     * Normaliza nombre de producto para comparacion
     */
    protected String normalizeName(String name) {
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

    /**
     * Extrae el precio por unidad de un texto
     * Ejemplo: "2,50 €/kg" -> "2.50€/kg"
     */
    protected String extractPricePerUnit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.replaceAll("\\s+", "").trim();
    }

    /**
     * Inicializa la configuración del scraper desde las propiedades
     */
    protected void initFromConfig() {
        this.timeoutMs = scrapingConfig.getTimeoutSeconds() * 1000;
        this.maxRetries = scrapingConfig.getMaxRetries();
    }
}
