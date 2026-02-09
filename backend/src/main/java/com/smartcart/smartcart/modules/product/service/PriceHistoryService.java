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

    // ─── SINCRONIZACIÓN: Mutation para el Scraper ──────────

    @Transactional
    public PriceHistoryDTO register(PriceUpdateDTO input) {
        ProductStore ps = productStoreRepository
                .findByProductId_ProductIdAndStoreId_StoreId(input.getProductId(), input.getStoreId())
                .orElseThrow(() -> new RuntimeException("Relación producto-tienda no encontrada"));

        boolean priceChanged = ps.getCurrentPrice() == null
                || !ps.getCurrentPrice().equals(input.getPrice());

        // Actualizar precio actual en ProductStore
        ps.setCurrentPrice(input.getPrice());
        ps.setAvailable(input.getPrice() > 0);
        if (input.getStock() != null) ps.setStock(input.getStock());
        if (input.getExternaId() != null) ps.setExternaId(input.getExternaId());
        productStoreRepository.save(ps);

        // Crear registro histórico solo si cambió el precio
        if (priceChanged) {
            PriceHistory history = PriceHistoryMapper.toEntity(input, ps);
            PriceHistoryDTO dto = PriceHistoryMapper.toDTO(priceHistoryRepository.save(history));

            // Comprobar alertas activas para este producto
            checkAlerts(ps.getProductId().getProductId(), input.getPrice());

            return dto;
        }
        return null;
    }

    // ─── HISTORIAL: Evolución de precios de un producto ────

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

    // ─── ALERTAS: Comprobar si el precio disparó alguna alerta ─

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
