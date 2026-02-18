package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

public record OptimizedStoreDTO
(
    Integer storeId,
    String storeName,
    String storeLogo,
    Double subtotal,
    List<OptimizedItemDTO> items,
    List<String> notFound
)
{
    public OptimizedStoreDTO(Integer storeId, String storeName, String storeLogo, Double subtotal, List<OptimizedItemDTO> items)
    {
        this(storeId, storeName, storeLogo, subtotal, items, null);
    }
}
