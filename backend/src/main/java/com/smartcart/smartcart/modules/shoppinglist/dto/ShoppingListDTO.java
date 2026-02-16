package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

public record ShoppingListDTO 
(
    Integer listId,
    String name,
    String createdAt,
    String updatedAt,
    List<ListItemDTO> items
){}
