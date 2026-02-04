package com.smartcart.smartcart.modules.product.entity;

import java.time.LocalDateTime;

import com.smartcart.smartcart.modules.store.entity.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "product_store")
@Data
public class ProductStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_product_id")
    private Integer storeProductId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product productId;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store storeId;

    @Column(name = "externa_id")
    private String externaId;

    @Column(name = "url")
    private String url;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "available")
    private Boolean available;

    @Column(name = "stock")
    private Integer stock;


    // Constructors
    public ProductStore() {
    }

    public ProductStore(Boolean available, Double currentPrice, String externaId, LocalDateTime lastUpdate, Product productId, Integer stock, Store storeId, Integer storeProductId, String url) {
        this.available = available;
        this.currentPrice = currentPrice;
        this.externaId = externaId;
        this.lastUpdate = lastUpdate;
        this.productId = productId;
        this.stock = stock;
        this.storeId = storeId;
        this.storeProductId = storeProductId;
        this.url = url;
    }

    //Automatic update of lastUpdate field
    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdate = LocalDateTime.now();
    }
}
