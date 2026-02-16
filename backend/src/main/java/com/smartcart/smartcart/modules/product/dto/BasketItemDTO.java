package com.smartcart.smartcart.modules.product.dto;

public record BasketItemDTO(
    Integer productId,
    String productName,
    Double price,
    Boolean available
) {}
