package com.smartcart.smartcart.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;

public interface PriceRecordRepository extends JpaRepository<PriceHistory, Integer> {

}
