package com.smartcart.smartcart.modules.store.dto;

public record StoreDTO(
    Integer storeId,
    String name,
    String slug,
    String logo,
    String website,
    Boolean active,
    String scrapingUrl,
    String scrapingConf,
    Long productCount
) {}
