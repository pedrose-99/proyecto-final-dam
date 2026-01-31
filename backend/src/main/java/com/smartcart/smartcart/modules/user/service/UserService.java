package com.smartcart.smartcart.modules.user.service;

import com.smartcart.smartcart.modules.user.dto.ProfileUpdateRequest;
import com.smartcart.smartcart.modules.user.dto.UserDTO;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO getCurrentUser() 
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapToDTO(user);
    }

    public UserDTO updateProfile(ProfileUpdateRequest request) 
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) 
        {
            if (userRepository.existsByUsername(request.getUsername()) && !user.getUsername().equals(request.getUsername())) 
            {
                throw new RuntimeException("El username ya está en uso");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) 
        {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);

        return mapToDTO(user);
    }

    private UserDTO mapToDTO(User user)
    {
        return UserDTO.builder()
                .idUser(user.getIdUser())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}