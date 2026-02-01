package com.smartcart.smartcart.modules.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {

    List<PriceHistory> findByProductStoreIdList(Integer productId);

    List<PriceHistory> findByProductStoreId(Integer productId, Integer storeId);
    
}
