package com.smartcart.smartcart.modules.product.dto;

public record ProductStoreDTO(
    Integer storeProductId,
    String productName,
    String productBrand,
    String ean,
    String storeName,
    Double currentPrice,
    String url,
    Boolean available,
    Integer stock,
    String externaId,
    String unit
) {}
