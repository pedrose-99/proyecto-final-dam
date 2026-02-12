package com.smartcart.smartcart.modules.shoppinglist.dto;

import java.util.List;

import lombok.Data;

@Data
public class ShoppingListDTO 
{
    private Integer listId;
    private String name;
    private String createdAt;
    private String updatedAt;
    private List<ListItemDTO> items;

}
