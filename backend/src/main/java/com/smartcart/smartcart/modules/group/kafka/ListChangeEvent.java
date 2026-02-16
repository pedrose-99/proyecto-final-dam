package com.smartcart.smartcart.modules.group.kafka;

import java.io.Serializable;

public record ListChangeEvent(
    Integer listId,
    String listName,
    Integer groupId,
    Integer authorUserId,
    String authorUsername,
    String action // CREATED, UPDATED, DELETED
) implements Serializable {}
