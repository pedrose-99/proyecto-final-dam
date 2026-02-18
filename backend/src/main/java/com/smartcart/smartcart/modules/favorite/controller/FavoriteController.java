package com.smartcart.smartcart.modules.favorite.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.favorite.service.FavoriteService;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FavoriteController 
{
    private final FavoriteService favoriteService;

    @QueryMapping
    public List<ProductDTO> myFavorites()
    {
        return favoriteService.getMyFavorites();
    }

    @QueryMapping
    public boolean isFavorite(@Argument Integer productId)
    {
        return favoriteService.isFavorite(productId);
    }

    @MutationMapping
    public boolean addToFavorites(@Argument Integer productId)
    {
        return favoriteService.addFavorite(productId);
    }

    @MutationMapping
    public boolean removeFromFavorites(@Argument Integer productId)
    {
        return favoriteService.removeFavorite(productId);
    }
}
