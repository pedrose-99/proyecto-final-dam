package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.entity.Product;

public class ProductMapper {

    public static ProductDTO toDTO(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(entity.getProductId());
        dto.setName(entity.getName());
        dto.setBrand(entity.getBrand());
        dto.setEan(entity.getEan());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());

        if (entity.getCategoryId() != null) {
            dto.setCategoryName(entity.getCategoryId().getName());
            dto.setCategoryId(entity.getCategoryId().getCategoryId());
        }
        return dto;
    }
}
