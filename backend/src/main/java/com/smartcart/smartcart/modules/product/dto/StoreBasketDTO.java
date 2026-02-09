package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

import lombok.Data;

@Data
public class StoreBasketDTO {
    private Integer storeId;
    private String storeName;
    private String storeLogo;
    private Double totalCost;
    private Integer availableProducts;
    private Integer totalProducts;
    private List<BasketItemDTO> items;
}
