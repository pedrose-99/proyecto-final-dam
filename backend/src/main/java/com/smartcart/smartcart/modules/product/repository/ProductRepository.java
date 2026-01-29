package com.smartcart.smartcart.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

}
