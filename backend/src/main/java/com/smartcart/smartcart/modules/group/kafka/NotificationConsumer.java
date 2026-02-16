package com.smartcart.smartcart.modules.group.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartcart.smartcart.common.enums.NotificationType;
import com.smartcart.smartcart.modules.group.entity.Group;
import com.smartcart.smartcart.modules.group.entity.GroupMember;
import com.smartcart.smartcart.modules.group.repository.GroupMemberRepository;
import com.smartcart.smartcart.modules.group.repository.GroupRepository;
import com.smartcart.smartcart.modules.notification.entity.Notification;
import com.smartcart.smartcart.modules.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;

    @KafkaListener(topics = "list-change-topic", groupId = "smartcart-notification-group")
    @Transactional
    public void handleListChange(ListChangeEvent event) {
        log.info("Recibido evento de cambio en lista: {} (grupo: {})", event.listName(), event.groupId());

        Group group = groupRepository.findById(event.groupId()).orElse(null);
        if (group == null) {
            log.warn("Grupo {} no encontrado, ignorando evento", event.groupId());
            return;
        }

        // Obtener miembros ACCEPTED del grupo, excepto el autor del cambio
        List<GroupMember> members = groupMemberRepository.findAcceptedMembersByGroupId(event.groupId());

        String message = buildNotificationMessage(event);

        for (GroupMember member : members) {
            if (member.getUser() != null
                    && !member.getUser().getIdUser().equals(event.authorUserId())) {

                Notification notification = new Notification();
                notification.setRecipient(member.getUser());
                notification.setMessage(message);
                notification.setType(NotificationType.UPDATE);
                notification.setRelatedGroup(group);
                notificationRepository.save(notification);
            }
        }

        log.info("Notificaciones creadas para {} miembros del grupo {}", members.size() - 1, event.groupId());
    }

    private String buildNotificationMessage(ListChangeEvent event) {
        return switch (event.action()) {
            case "CREATED" -> event.authorUsername() + " creó la lista '" + event.listName() + "'";
            case "UPDATED" -> event.authorUsername() + " editó la lista '" + event.listName() + "'";
            case "DELETED" -> event.authorUsername() + " eliminó la lista '" + event.listName() + "'";
            default -> event.authorUsername() + " modificó la lista '" + event.listName() + "'";
        };
    }
}
