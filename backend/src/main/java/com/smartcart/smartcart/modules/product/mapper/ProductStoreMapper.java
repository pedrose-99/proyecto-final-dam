package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.ProductStoreDTO;
import com.smartcart.smartcart.modules.product.entity.ProductStore;

public class ProductStoreMapper {
    public static ProductStoreDTO toDTO(ProductStore entity) {
        return new ProductStoreDTO(
            entity.getStoreProductId(),
            entity.getProductId().getName(),
            entity.getProductId().getBrand(),
            entity.getProductId().getEan(),
            entity.getStoreId().getName(),
            entity.getCurrentPrice(),
            entity.getUrl(),
            entity.getAvailable(),
            entity.getStock(),
            entity.getExternaId(),
            entity.getProductId().getUnit()
        );
    }
}
