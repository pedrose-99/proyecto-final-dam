package com.smartcart.smartcart.modules.favorite.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
//evita duplicados
@Table(name = "favorites",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite_user_product", columnNames = {"user_id", "product_id"})
    })
@Data
public class Favorite 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Integer favoriteId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public Favorite(){}
    
}
