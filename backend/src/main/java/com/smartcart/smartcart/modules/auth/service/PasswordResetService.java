package com.smartcart.smartcart.modules.auth.service;

import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService 
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public void resetPassword(String email)
    {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty())
        {
            log.warn("Intento de reset de contraseña para email no existente");
            return;
        }
        
        User user = userOpt.get();
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, newPassword);
        log.info("Contraseña reseteada para usuario");
    }

    private String generateRandomPassword()
    {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        return new Random().ints(8, 0, chars.length())
            .mapToObj(i -> String.valueOf(chars.charAt(i)))
            .collect(Collectors.joining());
    }
    
}
