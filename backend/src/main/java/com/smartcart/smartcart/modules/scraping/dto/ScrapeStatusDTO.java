package com.smartcart.smartcart.modules.scraping.dto;

import java.time.LocalDateTime;

public record ScrapeStatusDTO(
    String storeName,
    String storeSlug,
    boolean enabled,
    boolean healthy,
    Long productCount,
    LocalDateTime lastScrapeTime,
    String lastScrapeStatus,
    Integer lastProductsFound
) {}
