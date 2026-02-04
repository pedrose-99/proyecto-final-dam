package com.smartcart.smartcart.modules.product.mapper;



import com.smartcart.smartcart.modules.product.dto.PriceHistoryDTO;
import com.smartcart.smartcart.modules.product.dto.PriceUpdateDTO;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import java.time.LocalDateTime;

public class PriceHistoryMapper {

    // PARA EL FRONT: De Entidad a DTO
    public static PriceHistoryDTO toDTO(PriceHistory entity) {
        PriceHistoryDTO dto = new PriceHistoryDTO();
        dto.setPriceHistoryId(entity.getPriceHistoryId());
        dto.setPrice(entity.getPrice());
        dto.setOriginalPrice(entity.getOriginalPrice());
        dto.setIsOnSale(entity.getIsOnSale());
        dto.setRecordedAt(entity.getRecordedAt());
        
        // Navegamos por las relaciones para sacar nombres
        if (entity.getProductStoreId() != null) {
            dto.setStoreName(entity.getProductStoreId().getStoreId().getName());
            dto.setProductName(entity.getProductStoreId().getProductId().getName());
        }
        return dto;
    }

    // PARA EL SCRAPER: De DTO a Entidad (Para crear el registro)
    public static PriceHistory toEntity(PriceUpdateDTO dto, ProductStore ps) {
        PriceHistory history = new PriceHistory();
        history.setProductStoreId(ps);
        history.setStoreId(ps.getStoreId());
        history.setPrice(dto.getPrice());
        history.setOriginalPrice(dto.getOriginalPrice());
        history.setIsOnSale(dto.getIsOnSale());
        history.setRecordedAt(LocalDateTime.now()); // Fecha de ahora
        return history;
    }
}
