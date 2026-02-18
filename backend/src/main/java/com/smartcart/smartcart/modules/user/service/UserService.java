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

        if (request.username() != null && !request.username().isBlank())
        {
            if (userRepository.existsByUsername(request.username()) && !user.getRealUsername().equals(request.username()))
            {
                throw new RuntimeException("El username ya está en uso");
            }
            user.setUsername(request.username());
        }

        if (request.newPassword() != null && !request.newPassword().isBlank())
        {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        userRepository.save(user);

        return mapToDTO(user);
    }

    private UserDTO mapToDTO(User user)
    {
        return new UserDTO(
            user.getIdUser(),
            user.getEmail(),
            user.getRealUsername(),
            user.getRole().getName(),
            user.getCreatedAt()
        );
    }
}
