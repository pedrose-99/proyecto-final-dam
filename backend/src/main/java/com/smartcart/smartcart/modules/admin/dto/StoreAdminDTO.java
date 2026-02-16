package com.smartcart.smartcart.modules.admin.dto;

import com.smartcart.smartcart.modules.store.entity.Store;

import java.time.LocalDateTime;

public record StoreAdminDTO(
    Integer id,
    String name,
    String slug,
    String logo,
    String website,
    Boolean active,
    String scrapingUrl,
    Long productCount,
    LocalDateTime lastScrapeDate,
    String lastScrapeStatus
) {
    public static StoreAdminDTO fromEntity(Store store, Long productCount,
                                           LocalDateTime lastScrapeDate, String lastScrapeStatus) {
        return new StoreAdminDTO(
            store.getStoreId(),
            store.getName(),
            store.getSlug(),
            store.getLogo(),
            store.getWebsite(),
            store.getActive(),
            store.getScrapingUrl(),
            productCount,
            lastScrapeDate,
            lastScrapeStatus
        );
    }
}
