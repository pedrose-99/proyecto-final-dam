package com.smartcart.smartcart.modules.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByEan(String ean);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByCategoryId_CategoryId(Integer categoryId);

    Page<Product> findByCategoryId_CategoryId(Integer categoryId, Pageable pageable);
}
