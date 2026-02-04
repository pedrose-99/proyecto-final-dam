package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.entity.Product;

public class ProductMapper {
    public static ProductDTO toDTO(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setName(entity.getName());
        dto.setBrand(entity.getBrand());
        

        if (entity.getCategoryId() != null) {
        dto.setCategoryName(entity.getCategoryId().getName());
    }
        dto.setImageUrl(entity.getImageUrl());
        return dto;
    }
}
