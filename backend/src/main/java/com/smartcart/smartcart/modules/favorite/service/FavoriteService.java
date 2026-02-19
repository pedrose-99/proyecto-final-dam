package com.smartcart.smartcart.modules.favorite.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcart.smartcart.modules.favorite.entity.Favorite;
import com.smartcart.smartcart.modules.favorite.repository.FavoriteRepository;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.mapper.ProductMapper;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService 
{
    private final UserRepository userRepository;
    private final FavoriteRepository favRepository;
    private final ProductRepository productRepository;

    private Optional<User> getCurrentUser()
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try
        {
           return userRepository.findByEmail(email);
        }
        catch(RuntimeException e)
        {
            log.error("Usuario no encontrado", e.getMessage());
        }
        return Optional.empty();
    }

    public List<ProductDTO> getMyFavorites()
    {
        Optional<User> user = getCurrentUser();

        if (user.isEmpty())
        {
            return List.of();
        }

        try
        {
            Integer userId = user.get().getIdUser();
            List<Favorite> favList = favRepository.findByUser_IdUser(userId);
            
            Set<ProductDTO> newList = new LinkedHashSet<ProductDTO>();

            for(Favorite fav : favList)
            {
                ProductDTO productDTO = ProductMapper.toDTO(fav.getProduct(), true);
                newList.add(productDTO);
            }
            return new java.util.ArrayList<>(newList);
        }
        catch(RuntimeException e)
        {
            log.error("No se encontró la lista", e.getMessage());
            return List.of();
        }
    }

    public boolean addFavorite(Integer productId)
    {
        Optional<User> user = getCurrentUser();

        if (user.isEmpty())
        {
            return false;
        }

        try
        {
            Optional<Product> product = productRepository.findById(productId);
            if (product.isEmpty())
            {
                return false;
            }

            Integer userId = user.get().getIdUser();
            
            // Verificar si ya existe el favorito
            if (favRepository.existsByUser_IdUserAndProduct_ProductId(userId, productId))
            {
                return true;
            }
            
            Favorite favorite = new Favorite();
            favorite.setUser(user.get());
            favorite.setProduct(product.get());
            
            favRepository.save(favorite);
            return true;
        }
        catch(RuntimeException e)
        {
            log.error("No se pudo añadir a favoritos", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean removeFavorite(Integer productId)
    {
        Optional<User> user = getCurrentUser();

        if (user.isEmpty())
        {
            return false;
        }

        try
        {
            favRepository.deleteByUser_IdUserAndProduct_ProductId(
                user.get().getIdUser(), 
                productId
            );
            return true;
        }
        catch(RuntimeException e)
        {
            log.error("No se pudo eliminar de favoritos", e.getMessage());
            return false;
        }
    }

    public boolean isFavorite(Integer productId)
    {
        Optional<User> user = getCurrentUser();

        if (user.isEmpty())
        {
            return false;
        }

        try
        {
            return favRepository.existsByUser_IdUserAndProduct_ProductId(
                user.get().getIdUser(), 
                productId
            );
        }
        catch(RuntimeException e)
        {
            log.error("Error al comprobar favorito", e.getMessage());
            return false;
        }
    }


    
}
