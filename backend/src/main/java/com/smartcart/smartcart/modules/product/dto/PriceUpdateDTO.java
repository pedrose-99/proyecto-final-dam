package com.smartcart.smartcart.modules.product.dto;

public record PriceUpdateDTO(
    Integer productId,
    Integer storeId,
    Double price,
    Double originalPrice,
    Boolean isOnSale,
    Integer stock,
    String externaId
) {}
