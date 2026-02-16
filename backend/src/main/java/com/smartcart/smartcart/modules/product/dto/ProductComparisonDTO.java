package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

public record ProductComparisonDTO(
    Integer productId,
    String name,
    String brand,
    String ean,
    String imageUrl,
    String description,
    String categoryName,
    List<StorePriceDTO> storePrices,
    StorePriceDTO bestPrice
) {}
