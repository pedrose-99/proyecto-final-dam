package com.smartcart.smartcart.modules.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.dto.PriceHistoryDTO;
import com.smartcart.smartcart.modules.product.dto.PriceUpdateDTO;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductAlert;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.mapper.PriceHistoryMapper;
import com.smartcart.smartcart.modules.product.repository.PriceHistoryRepository;
import com.smartcart.smartcart.modules.product.repository.ProductAlertRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;

import jakarta.transaction.Transactional;

@Service
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductStoreRepository productStoreRepository;
    private final ProductAlertRepository productAlertRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository,
                               ProductStoreRepository productStoreRepository,
                               ProductAlertRepository productAlertRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.productStoreRepository = productStoreRepository;
        this.productAlertRepository = productAlertRepository;
    }

    @Transactional
    public PriceHistoryDTO register(PriceUpdateDTO input) {
        ProductStore ps = productStoreRepository
                .findByProductId_ProductIdAndStoreId_StoreId(input.productId(), input.storeId())
                .orElseThrow(() -> new RuntimeException("Relación producto-tienda no encontrada"));

        boolean priceChanged = ps.getCurrentPrice() == null
                || !ps.getCurrentPrice().equals(input.price());

        ps.setCurrentPrice(input.price());
        ps.setAvailable(input.price() > 0);
        if (input.stock() != null) ps.setStock(input.stock());
        if (input.externaId() != null) ps.setExternaId(input.externaId());
        productStoreRepository.save(ps);

        if (priceChanged) {
            PriceHistory history = PriceHistoryMapper.toEntity(input, ps);
            PriceHistoryDTO dto = PriceHistoryMapper.toDTO(priceHistoryRepository.save(history));

            checkAlerts(ps.getProductId().getProductId(), input.price());

            return dto;
        }
        return null;
    }

    public List<PriceHistoryDTO> findByProductId(Integer productId) {
        return priceHistoryRepository
                .findByProductStoreId_ProductId_ProductIdOrderByRecordedAtDesc(productId)
                .stream()
                .map(PriceHistoryMapper::toDTO)
                .toList();
    }

    public List<PriceHistoryDTO> findByProductAndStore(Integer productId, Integer storeId) {
        return priceHistoryRepository
                .findByProductStoreId_ProductId_ProductIdAndProductStoreId_StoreId_StoreIdOrderByRecordedAtDesc(
                        productId, storeId)
                .stream()
                .map(PriceHistoryMapper::toDTO)
                .toList();
    }

    private void checkAlerts(Integer productId, Double newPrice) {
        List<ProductAlert> activeAlerts = productAlertRepository
                .findByProduct_ProductIdAndActiveTrue(productId);

        for (ProductAlert alert : activeAlerts) {
            if (newPrice <= alert.getTargetPrice()) {
                alert.setTriggered(true);
                productAlertRepository.save(alert);
            }
        }
    }
}
