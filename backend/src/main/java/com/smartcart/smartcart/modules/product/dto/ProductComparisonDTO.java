package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProductComparisonDTO {
    private Integer productId;
    private String name;
    private String brand;
    private String ean;
    private String imageUrl;
    private String categoryName;
    private List<StorePriceDTO> storePrices;
    private StorePriceDTO bestPrice;
}
