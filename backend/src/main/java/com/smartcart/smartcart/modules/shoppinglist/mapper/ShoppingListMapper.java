package com.smartcart.smartcart.modules.shoppinglist.mapper;

import com.smartcart.smartcart.modules.shoppinglist.dto.ListItemDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

import java.util.List;

public class ShoppingListMapper
{
    public static ShoppingListDTO toDTO(ShoppingList entity)
    {
        List<ListItemDTO> items = entity.getItems() == null
                ? List.of()
                : entity.getItems().stream()
                    .map(ShoppingListMapper::toItemDTO)
                    .toList();

        return new ShoppingListDTO(
                entity.getListId(),
                entity.getName(),
                entity.getUser() != null ? entity.getUser().getIdUser() : null,
                entity.getUser() != null ? entity.getUser().getRealUsername() : null,
                entity.getGroup() != null ? entity.getGroup().getGroupId() : null,
                entity.getGroup() != null ? entity.getGroup().getName() : null,
                entity.getCreatedAt(),
                items
        );
    }

    public static ListItemDTO toItemDTO(ListItem entity)
    {
        if (entity.getProduct() != null)
        {
            return new ListItemDTO(
                    entity.getItemId(),
                    entity.getProduct().getProductId(),
                    entity.getProduct().getName(),
                    entity.getProduct().getImageUrl(),
                    entity.getQuantity(),
                    entity.getChecked(),
                    false
            );
        }
        else
        {
            return new ListItemDTO(
                    entity.getItemId(),
                    null,
                    entity.getGenericName(),
                    null,
                    entity.getQuantity(),
                    entity.getChecked(),
                    true
            );
        }
    }
}
