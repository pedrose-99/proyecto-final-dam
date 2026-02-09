package com.smartcart.smartcart.modules.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "product_alert")
@Data
public class ProductAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "target_price", nullable = false)
    private Double targetPrice;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "triggered")
    private Boolean triggered;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ProductAlert() {}

    public ProductAlert(Product product, Double targetPrice) {
        this.product = product;
        this.targetPrice = targetPrice;
        this.active = true;
        this.triggered = false;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (triggered == null) triggered = false;
    }
}
