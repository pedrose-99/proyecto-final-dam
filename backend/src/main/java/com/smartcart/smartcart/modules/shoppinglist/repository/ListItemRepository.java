package com.smartcart.smartcart.modules.shoppinglist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;

public interface ListItemRepository extends JpaRepository<ListItem, Integer>
{

}
