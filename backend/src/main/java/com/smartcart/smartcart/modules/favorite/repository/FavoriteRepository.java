package com.smartcart.smartcart.modules.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartcart.smartcart.modules.favorite.entity.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer>
{
    List<Favorite> findByUser_IdUser(Integer userId);
    boolean existsByUser_IdUserAndProduct_ProductId(Integer userId, Integer productId);
    void deleteByUser_IdUserAndProduct_ProductId(Integer userId, Integer productId);
    
}
