package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.ProductStoreDTO;
import com.smartcart.smartcart.modules.product.entity.ProductStore;

public class ProductStoreMapper {
    public static ProductStoreDTO toDTO(ProductStore entity) {
        ProductStoreDTO dto = new ProductStoreDTO();
        dto.setStoreProductId(entity.getStoreProductId());
        
        // Datos que sacamos de la relación con Product
        dto.setProductName(entity.getProductId().getName());
        dto.setProductBrand(entity.getProductId().getBrand());
        dto.setEan(entity.getProductId().getEan());
        dto.setUnit(entity.getProductId().getUnit());
        
        // Datos que sacamos de la relación con Store
        dto.setStoreName(entity.getStoreId().getName());
        
        // Datos propios de la relación
        dto.setCurrentPrice(entity.getCurrentPrice());
        dto.setUrl(entity.getUrl());
        dto.setAvailable(entity.getAvailable());
        dto.setStock(entity.getStock());
        dto.setExternaId(entity.getExternaId());
        
        return dto;
    }
}