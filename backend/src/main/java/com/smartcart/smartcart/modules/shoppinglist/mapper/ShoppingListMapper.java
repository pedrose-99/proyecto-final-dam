package com.smartcart.smartcart.modules.shoppinglist.mapper;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.shoppinglist.dto.ListItemDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

import java.util.Comparator;
import java.util.List;

public class ShoppingListMapper
{
    public static ShoppingListDTO toDTO(ShoppingList entity, ProductStoreRepository productStoreRepo)
    {
        List<ListItemDTO> items = entity.getItems() == null
                ? List.of()
                : entity.getItems().stream()
                    .map(item -> toItemDTO(item, productStoreRepo))
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

    public static ListItemDTO toItemDTO(ListItem entity, ProductStoreRepository productStoreRepo)
    {
        if (entity.getProduct() != null)
        {
            String cheapestStoreName = null;
            List<ProductStore> stores = productStoreRepo.findByProductId_ProductId(entity.getProduct().getProductId());
            if (stores != null && !stores.isEmpty())
            {
                cheapestStoreName = stores.stream()
                        .filter(ps -> ps.getCurrentPrice() != null && ps.getCurrentPrice() > 0)
                        .min(Comparator.comparingDouble(ProductStore::getCurrentPrice))
                        .map(ps -> ps.getStoreId().getName())
                        .orElse(null);
            }

            return new ListItemDTO(
                    entity.getItemId(),
                    entity.getProduct().getProductId(),
                    entity.getProduct().getName(),
                    entity.getProduct().getImageUrl(),
                    entity.getQuantity(),
                    entity.getChecked(),
                    false,
                    cheapestStoreName
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
                    true,
                    null
            );
        }
    }
}
