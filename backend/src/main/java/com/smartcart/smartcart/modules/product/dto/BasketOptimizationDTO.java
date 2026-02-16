package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

public record BasketOptimizationDTO(
    List<StoreBasketDTO> storeOptions,
    StoreBasketDTO cheapestStore,
    Integer totalProducts
) {}
