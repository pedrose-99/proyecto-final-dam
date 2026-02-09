package com.smartcart.smartcart.modules.scraping.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ScrapedProduct(
    String externalId,
    String ean,
    String name,
    String brand,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    boolean onSale,
    String pricePerUnit,
    String unit,
    String imageUrl,
    String productUrl,
    String categoryName,
    String categoryId,
    String origin
) {}
