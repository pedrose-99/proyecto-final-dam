package com.smartcart.smartcart.modules.scraping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración del módulo de scraping.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "smartcart.scraping")
public class ScrapingConfig {

    private boolean enabled = true;
    private int defaultIntervalHours = 12;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long requestDelayMs = 2000;
    private List<String> userAgents = new ArrayList<>();
    private Map<String, StoreConfig> stores = new HashMap<>();

    @Data
    public static class StoreConfig {
        private boolean enabled = true;
        private String baseUrl;
        private String apiUrl;
        private int intervalHours = 12;
        private String postalCode;
        private boolean useApi = true;
        private boolean useSelenium = false;
        private Map<String, String> selectors = new HashMap<>();
    }

    /**
     * Obtiene la configuración de una tienda específica.
     */
    public StoreConfig getStoreConfig(String storeName) {
        return stores.get(storeName);
    }

    /**
     * Verifica si una tienda está habilitada.
     */
    public boolean isStoreEnabled(String storeName) {
        StoreConfig config = stores.get(storeName);
        return config != null && config.isEnabled();
    }
}
