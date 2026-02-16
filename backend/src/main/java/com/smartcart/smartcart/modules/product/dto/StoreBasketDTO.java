package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

public record StoreBasketDTO(
    Integer storeId,
    String storeName,
    String storeLogo,
    Double totalCost,
    Integer availableProducts,
    Integer totalProducts,
    List<BasketItemDTO> items
) {}
