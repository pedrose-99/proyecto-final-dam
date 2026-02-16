package com.smartcart.smartcart.modules.shoppinglist.dto;

public record ListItemDTO
(
    Integer itemId,
    Integer productId,
    String displayName,
    String imageUrl,
    Integer quantity,
    Boolean checked,
    Boolean isGeneric
){}
