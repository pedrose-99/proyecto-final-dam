package com.smartcart.smartcart.modules.group.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;

public record GroupDTO(
    Integer groupId,
    String name,
    String groupCode,
    String ownerUsername,
    Integer ownerId,
    LocalDateTime createdAt,
    List<GroupMemberDTO> members,
    List<ShoppingListDTO> shoppingLists
) {}
