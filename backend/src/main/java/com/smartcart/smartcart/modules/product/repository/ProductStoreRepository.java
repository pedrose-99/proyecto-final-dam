package com.smartcart.smartcart.modules.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.smartcart.smartcart.modules.product.entity.ProductStore;

public interface ProductStoreRepository extends JpaRepository<ProductStore, Integer> {

    
    List<ProductStore> findByProductIdList(Integer productId);

    Optional<ProductStore> findByProductId(Integer productId, Integer storeId);

    
}
