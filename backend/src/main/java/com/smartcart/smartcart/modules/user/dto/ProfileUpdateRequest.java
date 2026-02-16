package com.smartcart.smartcart.modules.user.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String newPassword
) {}
