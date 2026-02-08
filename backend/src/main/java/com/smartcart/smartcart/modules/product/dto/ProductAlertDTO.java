package com.smartcart.smartcart.modules.product.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProductAlertDTO {
    private Integer alertId;
    private Integer productId;
    private String productName;
    private String productEan;
    private Double targetPrice;
    private Double currentBestPrice;
    private Boolean active;
    private Boolean triggered;
    private LocalDateTime createdAt;
}
