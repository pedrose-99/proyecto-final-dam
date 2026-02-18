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
    Double currentPrice
) {
    public ProductDTO(Integer productId, String name, String brand, String ean,
                      String description, String imageUrl, Double quantity, String unit,
                      String categoryName, Integer categoryId) {
        this(productId, name, brand, ean, description, imageUrl, quantity, unit, categoryName, categoryId, null);
    }
}
