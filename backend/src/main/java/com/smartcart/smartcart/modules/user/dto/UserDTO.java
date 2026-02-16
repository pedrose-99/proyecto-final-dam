package com.smartcart.smartcart.modules.user.dto;

import java.time.LocalDateTime;

public record UserDTO(
    Integer idUser,
    String email,
    String username,
    String role,
    LocalDateTime createdAt
) {}
