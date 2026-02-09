package com.smartcart.smartcart.modules.product.entity;


import com.smartcart.smartcart.modules.category.entity.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "id_category")
    private Category categoryId;

    @Column(name = "name")
    private String name;

    @Column(name = "brand")
    private String brand;

    @Column(name = "description")
    private String description;

    @Column(name = "ean") //código de barras
    private String ean;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit") // unidad de medida
    private String unit;

    //constructors
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
