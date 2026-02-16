package com.smartcart.smartcart.modules.group.resolver;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.group.dto.GroupDTO;
import com.smartcart.smartcart.modules.group.service.CollaborationService;
import com.smartcart.smartcart.modules.notification.dto.NotificationDTO;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class GroupResolver {

    private final CollaborationService collaborationService;

    // ==================== QUERIES ====================

    @QueryMapping
    public List<GroupDTO> getMyGroups() {
        return collaborationService.getMyGroups();
    }

    @QueryMapping
    public List<NotificationDTO> getNotifications() {
        return collaborationService.getNotifications();
    }

    @QueryMapping
    public GroupDTO getGroupDetails(@Argument Integer groupId) {
        return collaborationService.getGroupDetails(groupId);
    }

    // ==================== MUTATIONS ====================

    @MutationMapping
    public GroupDTO createGroup(@Argument String name) {
        return collaborationService.createGroup(name);
    }

    @MutationMapping
    public Boolean inviteToGroup(@Argument Integer groupId, @Argument String target) {
        return collaborationService.inviteToGroup(groupId, target);
    }

    @MutationMapping
    public GroupDTO joinGroupByCode(@Argument String code) {
        return collaborationService.joinGroupByCode(code);
    }

    @MutationMapping
    public Boolean respondToInvite(@Argument Integer notificationId, @Argument Boolean accept) {
        return collaborationService.respondToInvite(notificationId, accept);
    }
}
