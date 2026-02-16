package com.smartcart.smartcart.modules.shoppinglist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;

@Repository
public interface ListItemRepository extends JpaRepository<ListItem, Integer> {
}
