package com.smartcart.smartcart.modules.scraping.dto;

import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeStatus;

import java.time.LocalDateTime;

public record ScrapeLogDTO(
    Long id,
    String storeName,
    String storeSlug,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer productsFound,
    Integer productsCreated,
    Integer productsUpdated,
    Integer productsUnchanged,
    Integer errorCount,
    ScrapeStatus status,
    String errorMessage,
    Long durationSeconds
) {
    public static ScrapeLogDTO fromEntity(ScrapeLog entity) {
        return new ScrapeLogDTO(
            entity.getId(),
            entity.getStore() != null ? entity.getStore().getName() : null,
            entity.getStore() != null ? entity.getStore().getSlug() : null,
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getProductsFound(),
            entity.getProductsCreated(),
            entity.getProductsUpdated(),
            entity.getProductsUnchanged(),
            entity.getErrorCount(),
            entity.getStatus(),
            entity.getErrorMessage(),
            entity.getDurationSeconds()
        );
    }
}
