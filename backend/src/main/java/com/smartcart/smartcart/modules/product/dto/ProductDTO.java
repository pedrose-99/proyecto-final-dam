package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Integer productId;
    private String name;
    private String brand;
    private String ean;
    private String description;
    private String imageUrl;
    private Double quantity;
    private String unit;
    private String categoryName;
    private Integer categoryId;
}
