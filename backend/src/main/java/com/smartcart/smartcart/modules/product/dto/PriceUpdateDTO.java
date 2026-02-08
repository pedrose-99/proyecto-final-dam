package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;


@Data
public class PriceUpdateDTO {
    private Integer productId;
    private Integer storeId;
    private Double price;
    private Double originalPrice;
    private Boolean isOnSale;
    private Integer stock;      
    private String externaId;
}
