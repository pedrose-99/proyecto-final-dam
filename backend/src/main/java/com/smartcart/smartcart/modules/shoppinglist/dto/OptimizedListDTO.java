package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

import lombok.Data;

@Data
public class OptimizedListDTO 
{
    private Double totalCost;
    private List<OptimizedStoreDTO> storeGroups;
    private List<String> notFound;
}
