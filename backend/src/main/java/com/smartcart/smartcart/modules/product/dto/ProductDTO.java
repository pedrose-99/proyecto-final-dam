package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Integer id;
    private String name;
    private String brand;
    private String categoryName;
    private String imageUrl;
}
