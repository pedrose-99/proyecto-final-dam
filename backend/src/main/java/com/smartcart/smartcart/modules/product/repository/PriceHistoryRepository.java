package com.smartcart.smartcart.modules.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {
    
    // Corregido: Navegamos a la propiedad 'storeProductId' dentro del objeto 'productStoreId'
    // El nombre del método indica a Spring Data: "Busca en el campo productStoreId, su propiedad storeProductId"
    List<PriceHistory> findByProductStoreId_StoreProductId(Integer storeProductId);

}