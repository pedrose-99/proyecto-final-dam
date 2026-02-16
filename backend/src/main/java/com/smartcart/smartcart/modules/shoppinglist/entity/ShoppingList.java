package com.smartcart.smartcart.modules.shoppinglist.entity;

import com.smartcart.smartcart.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_list")
@Data
public class ShoppingList 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_id")
    private Integer listId;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id", nullable = false) 
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at") 
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListItem> items = new ArrayList<>();
    

    private ShoppingList() {}

    //Ciclo de vida;
    @PrePersist
    protected void onCreate()
    {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        this.updatedAt = LocalDateTime.now();
    }
}
