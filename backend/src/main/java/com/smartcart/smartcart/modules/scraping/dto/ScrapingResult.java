package com.smartcart.smartcart.modules.scraping.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de una ejecución de scraping.
 */
@Data
public class ScrapingResult {

    private String storeName;
    private Long storeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalProducts;
    private int totalErrors;
    private List<ScrapedProduct> products = new ArrayList<>();
    private Map<String, String> errors = new HashMap<>();

    public void addError(String url, String message) {
        errors.put(url, message);
    }

    public void addProduct(ScrapedProduct product) {
        products.add(product);
    }

    public long getDurationSeconds() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }

    public boolean isSuccessful() {
        return totalErrors == 0 || (totalProducts > 0 && totalErrors < totalProducts);
    }
}
