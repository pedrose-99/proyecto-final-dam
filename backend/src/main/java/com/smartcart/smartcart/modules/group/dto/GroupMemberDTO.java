package com.smartcart.smartcart.modules.group.dto;

public record GroupMemberDTO(
    Integer id,
    Integer userId,
    String username,
    String email,
    String status
) {}
