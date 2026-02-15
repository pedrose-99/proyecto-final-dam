package com.smartcart.smartcart.modules.group.kafka;

import java.io.Serializable;

public record EmailInviteEvent(
    String email,
    String groupCode,
    String groupName
) implements Serializable {}
