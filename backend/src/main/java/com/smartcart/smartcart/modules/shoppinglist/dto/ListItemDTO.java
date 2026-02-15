package com.smartcart.smartcart.modules.shoppinglist.dto;

public record ListItemDTO(
    Integer itemId,
    Integer productId,
    String productName,
    String customName,
    Integer quantity,
    Boolean checked
) {}
