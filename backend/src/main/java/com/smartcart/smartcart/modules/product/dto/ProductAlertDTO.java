package com.smartcart.smartcart.modules.product.dto;

import java.time.LocalDateTime;

public record ProductAlertDTO(
    Integer alertId,
    Integer productId,
    String productName,
    String productEan,
    Double targetPrice,
    Double currentBestPrice,
    Boolean active,
    Boolean triggered,
    LocalDateTime createdAt
) {}
