package com.smartcart.smartcart.modules.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcart.smartcart.modules.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByEan(String ean);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByCategoryId_CategoryId(Integer categoryId);

    Page<Product> findByCategoryId_CategoryId(Integer categoryId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN ProductStore ps ON p.productId = ps.productId.productId WHERE ps.storeId.storeId = :storeId")
    Page<Product> findByStoreId(@Param("storeId") Integer storeId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN ProductStore ps ON p.productId = ps.productId.productId WHERE ps.storeId.storeId = :storeId")
    List<Product> findByStoreId(@Param("storeId") Integer storeId);

    @Query(value = "SELECT * FROM product WHERE " +
            "LOWER(translate(name, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) LIKE '%' || LOWER(translate(:query, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) || '%' " +
            "OR LOWER(translate(COALESCE(brand, ''), 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) LIKE '%' || LOWER(translate(:query, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) || '%'",
            countQuery = "SELECT count(*) FROM product WHERE " +
            "LOWER(translate(name, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) LIKE '%' || LOWER(translate(:query, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) || '%' " +
            "OR LOWER(translate(COALESCE(brand, ''), 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) LIKE '%' || LOWER(translate(:query, 'áéíóúüÁÉÍÓÚÜ', 'aeiouuAEIOUU')) || '%'",
            nativeQuery = true)
    Page<Product> searchByText(@Param("query") String query, Pageable pageable);
}
