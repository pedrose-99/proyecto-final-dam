package com.smartcart.smartcart.modules.shoppinglist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> {

    List<ShoppingList> findByUser_IdUser(Integer userId);

    @Query("SELECT sl FROM ShoppingList sl JOIN FETCH sl.user WHERE sl.user.idUser = :userId ORDER BY sl.createdAt DESC")
    List<ShoppingList> findByUser_IdUserOrderByCreatedAtDesc(@Param("userId") Integer userId);

    @Query("SELECT sl FROM ShoppingList sl JOIN FETCH sl.user WHERE sl.listId = :listId AND sl.user.idUser = :userId")
    Optional<ShoppingList> findByListIdAndUser_IdUser(@Param("listId") Integer listId, @Param("userId") Integer userId);

    List<ShoppingList> findByGroup_GroupId(Integer groupId);

    @Query("SELECT DISTINCT sl FROM ShoppingList sl JOIN FETCH sl.user WHERE sl.group.groupId IN :groupIds ORDER BY sl.createdAt DESC")
    List<ShoppingList> findByGroup_GroupIdInOrderByCreatedAtDesc(@Param("groupIds") List<Integer> groupIds);
    
    @Query("SELECT sl FROM ShoppingList sl JOIN FETCH sl.user WHERE sl.listId = :listId")
    Optional<ShoppingList> findByIdWithUser(@Param("listId") Integer listId);
}
