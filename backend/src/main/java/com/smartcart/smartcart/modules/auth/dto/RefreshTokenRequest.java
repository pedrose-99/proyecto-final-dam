package com.smartcart.smartcart.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest
{
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
