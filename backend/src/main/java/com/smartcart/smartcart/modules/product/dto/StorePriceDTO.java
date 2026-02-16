package com.smartcart.smartcart.modules.product.dto;

public record StorePriceDTO(
    Integer storeId,
    String storeName,
    String storeLogo,
    String storeWebsite,
    Double currentPrice,
    Boolean available,
    Integer stock,
    String url,
    String externaId
) {}
