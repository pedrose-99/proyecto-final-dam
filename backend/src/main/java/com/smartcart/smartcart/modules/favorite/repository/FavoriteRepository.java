package com.smartcart.smartcart.modules.favorite.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.favorite.entity.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer>
{
    List<Favorite> findByUser_IdUser(Integer userId);
    boolean existsByUser_IdUserAndProduct_ProductId(Integer userId, Integer productId);

    @Query("SELECT f.product.productId FROM Favorite f WHERE f.user.idUser = :userId")
    Set<Integer> findProductIdsByUserId(@Param("userId") Integer userId);

    @Modifying
    void deleteByUser_IdUserAndProduct_ProductId(Integer userId, Integer productId);

}
