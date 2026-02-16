package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.entity.Product;

public class ProductMapper {

    public static ProductDTO toDTO(Product entity) {
        return new ProductDTO(
            entity.getProductId(),
            entity.getName(),
            entity.getBrand(),
            entity.getEan(),
            entity.getDescription(),
            entity.getImageUrl(),
            entity.getQuantity(),
            entity.getUnit(),
            entity.getCategoryId() != null ? entity.getCategoryId().getName() : null,
            entity.getCategoryId() != null ? entity.getCategoryId().getCategoryId() : null
        );
    }
}
