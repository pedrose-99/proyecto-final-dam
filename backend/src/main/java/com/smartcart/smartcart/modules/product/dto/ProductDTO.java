package com.smartcart.smartcart.modules.product.dto;

public record ProductDTO(
    Integer productId,
    String name,
    String brand,
    String ean,
    String description,
    String imageUrl,
    Double quantity,
    String unit,
    String categoryName,
    Integer categoryId,
    boolean isFavorite
) {}
