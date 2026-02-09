package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;

@Data
public class StorePriceDTO {
    private Integer storeId;
    private String storeName;
    private String storeLogo;
    private Double currentPrice;
    private Boolean available;
    private Integer stock;
    private String url;
}
