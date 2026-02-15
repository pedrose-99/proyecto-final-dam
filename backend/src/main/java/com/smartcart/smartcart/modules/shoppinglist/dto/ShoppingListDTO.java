package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListDTO(
    Integer listId,
    String name,
    Integer userId,
    String username,
    Integer groupId,
    LocalDateTime createdAt,
    List<ListItemDTO> items
) {}
