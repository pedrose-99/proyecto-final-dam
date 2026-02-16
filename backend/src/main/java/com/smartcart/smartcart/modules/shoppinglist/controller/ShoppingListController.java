package com.smartcart.smartcart.modules.shoppinglist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.shoppinglist.dto.OptimizedListDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.service.OptimizerService;
import com.smartcart.smartcart.modules.shoppinglist.service.ShoppingListService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ShoppingListController 
{
    private final ShoppingListService slService;
    private final OptimizerService optimizerService;

    @QueryMapping
    public List<ShoppingListDTO> myShoppingLists()
    {
        return slService.getMyLists();
    }

    @QueryMapping
    public ShoppingListDTO shoppingListById(@Argument Integer listId)
    {
        return slService.getListById(listId);
    }

    @QueryMapping
    public OptimizedListDTO optimizeShoppingList(@Argument Integer listId, @Argument List<Integer> storeIds)
    {
        return optimizerService.optimize(listId, storeIds);
    }

    @MutationMapping
    public ShoppingListDTO createShoppingList(@Argument String name)
    {
        return slService.createList(name);
    }

    @MutationMapping
    public Boolean deleteShoppingList(@Argument Integer listId)
    {
        return slService.deleteList(listId);
    }

    @MutationMapping
    public ShoppingListDTO addItemToList(
            @Argument Integer listId,
            @Argument Integer productId,
            @Argument String genericName,
            @Argument Integer quantity)
    {
        return slService.addItem(listId, productId, genericName, quantity);
    }

    @MutationMapping
    public ShoppingListDTO updateListItem(
            @Argument Integer listId,
            @Argument Integer itemId,
            @Argument Integer quantity,
            @Argument Boolean checked)
    {
        return slService.updateItem(listId, itemId, quantity, checked);
    }

    @MutationMapping
    public ShoppingListDTO removeListItem(@Argument Integer listId, @Argument Integer itemId)
    {
        return slService.removeItem(listId, itemId);
    }

    @MutationMapping
    public List<ShoppingListDTO> createSublists(
            @Argument String originalListName,
            @Argument List<Map<String, Object>> sublists)
    {
        return slService.createSublists(originalListName, sublists);
    }

}
