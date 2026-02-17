package com.smartcart.smartcart.modules.auth.controller;

import com.smartcart.smartcart.modules.auth.dto.AuthResponse;
import com.smartcart.smartcart.modules.auth.dto.ForgotPasswordRequest;
import com.smartcart.smartcart.modules.auth.dto.LoginRequest;
import com.smartcart.smartcart.modules.auth.dto.RefreshTokenRequest;
import com.smartcart.smartcart.modules.auth.dto.RegisterRequest;
import com.smartcart.smartcart.modules.auth.service.AuthService;
import com.smartcart.smartcart.modules.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request)
    {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request)
    {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request)
    {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request)
    {
        passwordResetService.resetPassword(request.email());
        return ResponseEntity.ok("Si el email existe, recibirás una nueva contraseña");
    }
}
