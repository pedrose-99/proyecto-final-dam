package com.smartcart.smartcart.modules.product.entity;


import com.smartcart.smartcart.modules.category.entity.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_ean", columnList = "ean"),
    @Index(name = "idx_product_category", columnList = "id_category")
})
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "id_category")
    private Category categoryId;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "brand")
    private String brand;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "ean")
    private String ean;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit")
    private String unit;

    public Product() {}

    public Product(Integer productId, Category categoryId, String name, String ean, String brand, String description,
            String imageUrl, Double quantity, String unit) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.ean = ean;
        this.brand = brand;
        this.description = description;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.unit = unit;
    }
    

}
