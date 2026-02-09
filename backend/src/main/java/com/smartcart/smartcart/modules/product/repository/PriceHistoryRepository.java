package com.smartcart.smartcart.modules.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {

    // Historial por relación ProductStore
    List<PriceHistory> findByProductStoreId_StoreProductId(Integer storeProductId);

    // Historial de un producto en TODAS las tiendas, ordenado por fecha descendente
    List<PriceHistory> findByProductStoreId_ProductId_ProductIdOrderByRecordedAtDesc(Integer productId);

    // Historial de un producto en UNA tienda concreta, ordenado por fecha descendente
    List<PriceHistory> findByProductStoreId_ProductId_ProductIdAndProductStoreId_StoreId_StoreIdOrderByRecordedAtDesc(
            Integer productId, Integer storeId);
}
