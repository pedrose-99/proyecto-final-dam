package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

import lombok.Data;

@Data
public class BasketOptimizationDTO {
    private List<StoreBasketDTO> storeOptions;
    private StoreBasketDTO cheapestStore;
    private Integer totalProducts;
}
