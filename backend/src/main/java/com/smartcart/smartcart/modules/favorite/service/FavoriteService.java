package com.smartcart.smartcart.modules.favorite.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.dto.ProductDTO;
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

    // private List<ProductDTO> getMyFavorites()
    // {
        
    // }

    // private boolean addFavorite(Integer productId)
    // {

    // }

    // private boolean removeFavorite(Integer productId)
    // {

    // }

    // private boolean isFavorite(Integer productId)
    // {

    // }


    
}
