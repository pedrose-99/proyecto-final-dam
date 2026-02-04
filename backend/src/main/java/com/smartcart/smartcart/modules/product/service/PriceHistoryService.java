package com.smartcart.smartcart.modules.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.dto.PriceHistoryDTO;
import com.smartcart.smartcart.modules.product.dto.PriceUpdateDTO;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.mapper.PriceHistoryMapper;
import com.smartcart.smartcart.modules.product.repository.PriceHistoryRepository;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;

import jakarta.transaction.Transactional;

@Service
public class PriceHistoryService {
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductStoreRepository productStoreRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository, 
                               ProductStoreRepository productStoreRepository,
                               ProductRepository productRepository,
                               StoreRepository storeRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.productStoreRepository = productStoreRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

   @Transactional 
    public PriceHistoryDTO register(PriceUpdateDTO input) {
        ProductStore ps = productStoreRepository.findByProductId_ProductIdAndStoreId_StoreId(input.getProductId(), input.getStoreId())
            .orElseThrow(() -> new RuntimeException("Relación no encontrada"));

        boolean priceChanged = ps.getCurrentPrice() == null || !ps.getCurrentPrice().equals(input.getPrice());

        ps.setCurrentPrice(input.getPrice());
        ps.setAvailable(input.getPrice() > 0);
        ps.setStock(input.getStock()); 
        ps.setExternaId(input.getExternaId()); 
        productStoreRepository.save(ps);

        if (priceChanged) {
            PriceHistory history = PriceHistoryMapper.toEntity(input, ps);
            return PriceHistoryMapper.toDTO(priceHistoryRepository.save(history));
        }
        return null; 
    }


    //Consulta por producto
   public List<PriceHistoryDTO> findByProductId(Integer productId) {
        return priceHistoryRepository.findByProductStoreId_StoreProductId(productId).stream()
                .map(PriceHistoryMapper::toDTO)
                .toList();
    }

    
}
