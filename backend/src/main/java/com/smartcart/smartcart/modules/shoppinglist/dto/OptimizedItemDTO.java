package com.smartcart.smartcart.modules.shoppinglist.dto;

import lombok.Data;

@Data
public class OptimizedItemDTO 
{
    private Integer productId;
    private String productName;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
}
