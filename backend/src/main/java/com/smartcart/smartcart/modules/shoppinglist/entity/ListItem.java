package com.smartcart.smartcart.modules.shoppinglist.entity;

import com.smartcart.smartcart.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "list_item")
@Data
public class ListItem 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peoduct_id")
    private Product product;

    @Column(name = "generic_name")
    private String genericName;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Boolean checked = false;
    

    public ListItem() {}

    public String getDisplayName()
    {
        if (product != null)
        {
            return product.getName();
        }
        return genericName;
    }


}
