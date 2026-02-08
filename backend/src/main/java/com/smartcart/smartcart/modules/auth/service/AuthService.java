package com.smartcart.smartcart.modules.auth.service;

import com.smartcart.smartcart.modules.auth.dto.AuthResponse;
import com.smartcart.smartcart.modules.auth.dto.LoginRequest;
import com.smartcart.smartcart.modules.auth.dto.RefreshTokenRequest;
import com.smartcart.smartcart.modules.auth.dto.RegisterRequest;
import com.smartcart.smartcart.modules.auth.entity.Token;
import com.smartcart.smartcart.modules.auth.entity.TokenType;
import com.smartcart.smartcart.modules.auth.repository.TokenRepository;
import com.smartcart.smartcart.modules.user.entity.Role;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.RoleRepository;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import com.smartcart.smartcart.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request)
    {
        if (userRepository.existsByEmail(request.email()))
        {
            throw new RuntimeException("El email ya está registrado");
        }

        if (userRepository.existsByUsername(request.username()))
        {
            throw new RuntimeException("El username ya está en uso");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(userRole);

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessTokenFromEmail(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        saveUserToken(savedUser, accessToken, TokenType.ACCESS);
        saveUserToken(savedUser, refreshToken, TokenType.REFRESH);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getRealUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    public AuthResponse login(LoginRequest request)
    {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        revokeAllUserTokens(user);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        saveUserToken(user, accessToken, TokenType.ACCESS);
        saveUserToken(user, refreshToken, TokenType.REFRESH);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getRealUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request)
    {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken))
        {
            throw new RuntimeException("Refresh token inválido o expirado");
        }

        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token no encontrado"));

        if (storedToken.isRevoked() || storedToken.isExpired())
        {
            throw new RuntimeException("Refresh token revocado o expirado");
        }

        if (storedToken.getTokenType() != TokenType.REFRESH)
        {
            throw new RuntimeException("Token proporcionado no es un refresh token");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        revokeAllUserTokens(user);

        String newAccessToken = jwtTokenProvider.generateAccessTokenFromEmail(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        saveUserToken(user, newAccessToken, TokenType.ACCESS);
        saveUserToken(user, newRefreshToken, TokenType.REFRESH);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getRealUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    private void saveUserToken(User user, String jwtToken, TokenType tokenType)
    {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user)
    {
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getIdUser());

        if (validUserTokens.isEmpty())
        {
            return;
        }

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }
}
