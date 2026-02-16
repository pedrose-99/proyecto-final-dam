package com.smartcart.smartcart.modules.admin.dto;

public record StoreProductCountDTO(
    String storeName,
    String storeSlug,
    long count
) {}
