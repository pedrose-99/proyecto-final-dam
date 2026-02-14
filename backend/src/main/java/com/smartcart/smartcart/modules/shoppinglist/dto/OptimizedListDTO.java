package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

public record OptimizedListDTO
(
    Double totalCost,
    List<OptimizedStoreDTO> storeGroups,
    List<String> notFound
){}