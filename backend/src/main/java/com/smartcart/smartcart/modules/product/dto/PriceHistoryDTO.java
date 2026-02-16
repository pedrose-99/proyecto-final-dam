package com.smartcart.smartcart.modules.product.dto;

import java.time.LocalDateTime;

public record PriceHistoryDTO(
    Integer priceHistoryId,
    Double price,
    Double originalPrice,
    Boolean isOnSale,
    LocalDateTime recordedAt,
    String storeName,
    String productName
) {}
