package com.smartcart.smartcart.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
    @NotBlank(message = "El nombre del rol es obligatorio")
    String roleName
) {}
