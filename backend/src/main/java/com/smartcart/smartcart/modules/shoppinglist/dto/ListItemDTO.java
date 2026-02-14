package com.smartcart.smartcart.modules.shoppinglist.dto;

public record ListItemDTO
(
    Integer listId,
    Integer productId,
    String displayName,
    String imageUrl,
    Integer quantity,
    Boolean checked,
    Boolean isGeneric
){}

    

