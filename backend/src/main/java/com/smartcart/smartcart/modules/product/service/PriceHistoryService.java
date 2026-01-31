package com.smartcart.smartcart.modules.product.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
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
    public PriceHistory register(Integer productId, Integer storeId, Double price, 
                                 Double originalPrice, Boolean isOnSale) {
    
        ProductStore ps = productStoreRepository.findByProductId(productId, storeId)
        .orElseThrow(() -> new RuntimeException("Relación Producto-Tienda no encontrada"));

        //Actualizamos el precio actual
        ps.setCurrentPrice(price);
        ps.setAvailable(true);
        productStoreRepository.save(ps);

        //Creamos el historial de precios
        PriceHistory history = new PriceHistory();
        history.setProductStoreId(ps);
        history.setStoreId(ps.getStoreId());
        history.setPrice(price);
        history.setOriginalPrice(originalPrice);
        history.setIsOnSale(isOnSale);
        history.setRecordedAt(LocalDateTime.now());

        return priceHistoryRepository.save(history);
    }

    //Consulta por producto
    public List<PriceHistory> findByProductId(Integer productId) {

    return priceHistoryRepository.findByProductStoreIdList(productId);
}
    
    //Consulta por producto y tienda
    public List<PriceHistory> priceHistoryByProductAndStore(Integer productId, Integer storeId) {
        return priceHistoryRepository.findByProductStoreId(productId, storeId);
    }
}
