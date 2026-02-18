package com.smartcart.smartcart.modules.shoppinglist.dto;

public record OptimizedItemDTO
(
    Integer productId,
    String productName,
    String imageUrl,
    Double unitPrice,
    Integer quantity,
    Double lineTotal,
    String searchTerm
) {}
