package com.smartcart.smartcart.modules.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByEan(String ean);

}
