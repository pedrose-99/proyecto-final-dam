package com.smartcart.smartcart.modules.product.mapper;

import com.smartcart.smartcart.modules.product.dto.PriceHistoryDTO;
import com.smartcart.smartcart.modules.product.dto.PriceUpdateDTO;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import java.time.LocalDateTime;

public class PriceHistoryMapper {

    public static PriceHistoryDTO toDTO(PriceHistory entity) {
        String storeName = null;
        String productName = null;
        if (entity.getProductStoreId() != null) {
            storeName = entity.getProductStoreId().getStoreId().getName();
            productName = entity.getProductStoreId().getProductId().getName();
        }
        return new PriceHistoryDTO(
            entity.getPriceHistoryId(),
            entity.getPrice(),
            entity.getOriginalPrice(),
            entity.getIsOnSale(),
            entity.getRecordedAt(),
            storeName,
            productName
        );
    }

    public static PriceHistory toEntity(PriceUpdateDTO dto, ProductStore ps) {
        PriceHistory history = new PriceHistory();
        history.setProductStoreId(ps);
        history.setStoreId(ps.getStoreId());
        history.setPrice(dto.price());
        history.setOriginalPrice(dto.originalPrice());
        history.setIsOnSale(dto.isOnSale());
        history.setRecordedAt(LocalDateTime.now());
        return history;
    }
}
