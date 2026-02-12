package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

import lombok.Data;

@Data
public class OptimizedStoreDTO 
{
    private Integer storeId;
    private String storeName;
    private String storeLogo;
    private Double subtotal;
    private List<OptimizedItemDTO> items;
}
