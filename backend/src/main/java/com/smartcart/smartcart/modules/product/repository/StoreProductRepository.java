package com.smartcart.smartcart.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.ProductStore;

public interface StoreProductRepository extends JpaRepository<ProductStore, Integer> {


}
