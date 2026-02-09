package com.smartcart.smartcart.modules.product.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PriceHistoryDTO {
    private Integer priceHistoryId;
    private Double price;
    private Double originalPrice;
    private Boolean isOnSale;
    private LocalDateTime recordedAt;
    private String storeName;
    private String productName;
}
