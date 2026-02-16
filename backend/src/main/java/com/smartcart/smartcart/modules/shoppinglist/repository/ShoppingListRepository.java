package com.smartcart.smartcart.modules.shoppinglist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> 
{
    List<ShoppingList> findByUser_IdUserOrderByUpdatedAtDesc(Integer userId);
    Optional<ShoppingList> findByListIdAndUser_IdUser(Integer listId, Integer userId);
}
