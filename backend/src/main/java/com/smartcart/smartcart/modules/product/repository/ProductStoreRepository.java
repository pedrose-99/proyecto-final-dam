package com.smartcart.smartcart.modules.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcart.smartcart.modules.product.entity.ProductStore;

public interface ProductStoreRepository extends JpaRepository<ProductStore, Integer> {


    List<ProductStore> findByProductId_ProductId(Integer productId);


Optional<ProductStore> findByProductId_ProductIdAndStoreId_StoreId(Integer productId, Integer storeId);

    Optional<ProductStore> findByExternaIdAndStoreId_StoreId(String externaId, Integer storeId);

    @Query("SELECT ps FROM ProductStore ps WHERE ps.storeId.storeId = :storeId AND ps.externaId IS NOT NULL AND (ps.productId.ean IS NULL OR ps.productId.ean = '')")
    List<ProductStore> findByStoreWithoutEan(@Param("storeId") Integer storeId);

    @Query("SELECT COUNT(DISTINCT ps.productId) FROM ProductStore ps WHERE ps.storeId.storeId = :storeId")
    Long countProductsByStoreId(@Param("storeId") Integer storeId);

    @Query("SELECT ps FROM ProductStore ps JOIN FETCH ps.productId p LEFT JOIN FETCH p.categoryId WHERE ps.storeId.storeId = :storeId")
    List<ProductStore> findAllByStoreWithProductAndCategory(@Param("storeId") Integer storeId);
}
