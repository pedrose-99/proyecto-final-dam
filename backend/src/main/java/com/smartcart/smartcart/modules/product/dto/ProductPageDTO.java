package com.smartcart.smartcart.modules.product.dto;

import java.util.List;

public record ProductPageDTO(
    List<ProductDTO> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    boolean first,
    boolean last
) {}
