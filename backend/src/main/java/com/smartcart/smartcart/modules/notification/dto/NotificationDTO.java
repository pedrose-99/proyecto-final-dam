package com.smartcart.smartcart.modules.notification.dto;

import java.time.LocalDateTime;

public record NotificationDTO(
    Integer notificationId,
    String message,
    String type,
    Boolean isRead,
    Integer relatedGroupId,
    String relatedGroupName,
    LocalDateTime createdAt
) {}
