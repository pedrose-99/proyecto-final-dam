package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;

@Data
public class ProductStoreDTO {
    private Integer storeProductId;
    private String productName;
    private String productBrand;
    private String ean;        
    private String storeName;
    private Double currentPrice;
    private String url;
    private Boolean available;
    private Integer stock;     
    private String externaId; 
    private String unit;      
}
