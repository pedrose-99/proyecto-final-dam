package com.smartcart.smartcart.modules.shoppinglist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> 
{
    List<ShoppingList> findByUserId(Integer userId);
    Optional<ShoppingList> findByListAndUserId(Integer listId, Integer userId);

}
