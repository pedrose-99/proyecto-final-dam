package com.smartcart.smartcart.modules.admin.dto;

import com.smartcart.smartcart.modules.user.entity.User;

import java.time.LocalDateTime;

public record UserAdminDTO(
    Integer id,
    String username,
    String email,
    String roleName,
    LocalDateTime createdAt
) {
    public static UserAdminDTO fromEntity(User user) {
        return new UserAdminDTO(
            user.getIdUser(),
            user.getRealUsername(),
            user.getEmail(),
            user.getRole() != null ? user.getRole().getName() : null,
            user.getCreatedAt()
        );
    }
}
