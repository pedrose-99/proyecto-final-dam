package com.smartcart.smartcart.modules.shoppinglist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> {

    List<ShoppingList> findByUser_IdUser(Integer userId);

    List<ShoppingList> findByUser_IdUserOrderByCreatedAtDesc(Integer userId);

    Optional<ShoppingList> findByListIdAndUser_IdUser(Integer listId, Integer userId);

    List<ShoppingList> findByGroup_GroupId(Integer groupId);

    List<ShoppingList> findByGroup_GroupIdInOrderByCreatedAtDesc(List<Integer> groupIds);
}
