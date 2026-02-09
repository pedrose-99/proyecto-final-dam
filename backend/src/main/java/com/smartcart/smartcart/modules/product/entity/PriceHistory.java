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
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "price_history")
@Data
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_history_id")
    private Integer priceHistoryId;

    @ManyToOne
    @JoinColumn(name = "store_product_id")
    private ProductStore productStoreId;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store storeId;

    @Column(name = "price")
    private Double price;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(name = "is_on_sale")
    private Boolean isOnSale;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public PriceHistory() {
    }

    public PriceHistory(Boolean isOnSale, Double originalPrice, Double price, Integer priceHistoryId, ProductStore productStoreId, LocalDateTime recordedAt, Store storeId) {
        this.isOnSale = isOnSale;
        this.originalPrice = originalPrice;
        this.price = price;
        this.priceHistoryId = priceHistoryId;
        this.productStoreId = productStoreId;
        this.recordedAt = recordedAt;
        this.storeId = storeId;
    }
    
}
