package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;

@Data
public class BasketItemDTO {
    private Integer productId;
    private String productName;
    private Double price;
    private Boolean available;
}
