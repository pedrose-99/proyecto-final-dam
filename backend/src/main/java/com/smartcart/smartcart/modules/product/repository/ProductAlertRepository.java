package com.smartcart.smartcart.modules.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.ProductAlert;

public interface ProductAlertRepository extends JpaRepository<ProductAlert, Integer> {

    List<ProductAlert> findByActiveTrue();

    List<ProductAlert> findByProduct_ProductId(Integer productId);

    List<ProductAlert> findByProduct_ProductIdAndActiveTrue(Integer productId);
}
