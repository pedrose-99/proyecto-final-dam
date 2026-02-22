package com.smartcart.smartcart.modules.group.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcart.smartcart.common.enums.MemberStatus;
import com.smartcart.smartcart.common.enums.NotificationType;
import com.smartcart.smartcart.common.exception.BadRequestException;
import com.smartcart.smartcart.common.exception.ResourceNotFoundException;
import com.smartcart.smartcart.common.exception.UnauthorizedException;
import com.smartcart.smartcart.modules.group.dto.GroupDTO;
import com.smartcart.smartcart.modules.group.dto.GroupMemberDTO;
import com.smartcart.smartcart.modules.group.entity.Group;
import com.smartcart.smartcart.modules.group.entity.GroupMember;
import com.smartcart.smartcart.modules.group.repository.GroupMemberRepository;
import com.smartcart.smartcart.modules.group.repository.GroupRepository;
import com.smartcart.smartcart.modules.notification.dto.NotificationDTO;
import com.smartcart.smartcart.modules.notification.entity.Notification;
import com.smartcart.smartcart.modules.notification.repository.NotificationRepository;
import com.smartcart.smartcart.modules.shoppinglist.dto.ListItemDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.group.kafka.EmailInviteEvent;
import com.smartcart.smartcart.modules.group.kafka.ListChangeProducer;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollaborationService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ListChangeProducer listChangeProducer;

    @Transactional
    public GroupDTO createGroup(String name) {
        User currentUser = getAuthenticatedUser();

        Group group = new Group();
        group.setName(name);
        group.setOwner(currentUser);

        String code;
        do {
            code = Group.generateGroupCode();
        } while (groupRepository.existsByGroupCode(code));
        group.setGroupCode(code);

        group = groupRepository.save(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroup(group);
        ownerMember.setUser(currentUser);
        ownerMember.setStatus(MemberStatus.ACCEPTED);
        groupMemberRepository.save(ownerMember);

        return toGroupDTO(group);
    }

    @Transactional
    public boolean inviteToGroup(Integer groupId, String target) {
        User currentUser = getAuthenticatedUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        groupMemberRepository.findAcceptedMember(groupId, currentUser.getIdUser())
                .orElseThrow(() -> new UnauthorizedException("No tienes permisos para invitar a este grupo"));

        if (target.contains("@")) {
            return inviteByEmail(group, target);
        } else {
            return inviteByUsername(group, target);
        }
    }

    private boolean inviteByUsername(Group group, String username) {
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cuenta con el usuario '" + username + "'"));

        if (groupMemberRepository.existsByGroupAndUser(group, targetUser)) {
            throw new BadRequestException("El usuario ya es miembro o tiene una invitación pendiente");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(targetUser);
        member.setStatus(MemberStatus.PENDING);
        groupMemberRepository.save(member);

        Notification notification = new Notification();
        notification.setRecipient(targetUser);
        notification.setMessage("Te han invitado al grupo '" + group.getName() + "'");
        notification.setType(NotificationType.INVITE);
        notification.setRelatedGroup(group);
        notificationRepository.save(notification);

        return true;
    }

    private boolean inviteByEmail(Group group, String email) {
        var optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User targetUser = optionalUser.get();
            if (groupMemberRepository.existsByGroupAndUser(group, targetUser)) {
                throw new BadRequestException("El usuario ya es miembro o tiene una invitación pendiente");
            }

            GroupMember member = new GroupMember();
            member.setGroup(group);
            member.setUser(targetUser);
            member.setStatus(MemberStatus.PENDING);
            groupMemberRepository.save(member);

            Notification notification = new Notification();
            notification.setRecipient(targetUser);
            notification.setMessage("Te han invitado al grupo '" + group.getName() + "'");
            notification.setType(NotificationType.INVITE);
            notification.setRelatedGroup(group);
            notificationRepository.save(notification);

            return true;
        } else {
            throw new ResourceNotFoundException("No existe una cuenta con el correo '" + email + "'");
        }
    }

    @Transactional
    public GroupDTO joinGroupByCode(String code) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findByGroupCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código de grupo inválido"));

        if (groupMemberRepository.existsByGroupAndUser(group, currentUser)) {
            throw new BadRequestException("Ya eres miembro de este grupo");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(currentUser);
        member.setStatus(MemberStatus.ACCEPTED);
        groupMemberRepository.save(member);

        Notification notification = new Notification();
        notification.setRecipient(group.getOwner());
        notification.setMessage(currentUser.getRealUsername() + " se ha unido al grupo '" + group.getName() + "'");
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedGroup(group);
        notificationRepository.save(notification);

        return toGroupDTO(group);
    }

    @Transactional
    public boolean deleteGroup(Integer groupId) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (!group.getOwner().getIdUser().equals(currentUser.getIdUser())) {
            throw new UnauthorizedException("Solo el propietario puede eliminar el grupo");
        }

        groupMemberRepository.deleteByGroup(group);

        notificationRepository.deleteByRelatedGroup(group);

        groupRepository.delete(group);

        return true;
    }

    @Transactional
    public boolean leaveGroup(Integer groupId) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (group.getOwner().getIdUser().equals(currentUser.getIdUser())) {
            throw new BadRequestException("El propietario no puede salir del grupo");
        }

        GroupMember membership = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("No eres miembro de este grupo"));

        if (membership.getStatus() != MemberStatus.ACCEPTED) {
            throw new BadRequestException("No eres miembro aceptado del grupo");
        }

        groupMemberRepository.delete(membership);
        return true;
    }

    @Transactional
    public boolean removeGroupMember(Integer groupId, Integer userId) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (!group.getOwner().getIdUser().equals(currentUser.getIdUser())) {
            throw new UnauthorizedException("Solo el propietario puede eliminar miembros del grupo");
        }

        if (group.getOwner().getIdUser().equals(userId)) {
            throw new BadRequestException("El propietario no puede eliminarse a si mismo");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        GroupMember membership = groupMemberRepository.findByGroupAndUser(group, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no pertenece a este grupo"));

        groupMemberRepository.delete(membership);

        Notification notification = new Notification();
        notification.setRecipient(targetUser);
        notification.setMessage("Has sido eliminado del grupo '" + group.getName() + "'");
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedGroup(group);
        notificationRepository.save(notification);

        return true;
    }

    @Transactional
    public boolean deleteNotification(Integer notificationId) {
        User currentUser = getAuthenticatedUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.getRecipient().getIdUser().equals(currentUser.getIdUser())) {
            throw new UnauthorizedException("Esta notificación no te pertenece");
        }

        notificationRepository.delete(notification);
        return true;
    }

    @Transactional
    public boolean respondToInvite(Integer notificationId, boolean accept) {
        User currentUser = getAuthenticatedUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.getRecipient().getIdUser().equals(currentUser.getIdUser())) {
            throw new UnauthorizedException("Esta notificación no te pertenece");
        }

        if (notification.getType() != NotificationType.INVITE) {
            throw new BadRequestException("Esta notificación no es una invitación");
        }

        Group group = notification.getRelatedGroup();
        if (group == null) {
            throw new BadRequestException("Grupo asociado no encontrado");
        }

        GroupMember membership = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una invitación a este grupo"));

        if (accept) {
            membership.setStatus(MemberStatus.ACCEPTED);
            groupMemberRepository.save(membership);

            Notification acceptNotif = new Notification();
            acceptNotif.setRecipient(group.getOwner());
            acceptNotif.setMessage(currentUser.getRealUsername() + " aceptó la invitación al grupo '" + group.getName() + "'");
            acceptNotif.setType(NotificationType.SYSTEM);
            acceptNotif.setRelatedGroup(group);
            notificationRepository.save(acceptNotif);
        } else {
            groupMemberRepository.delete(membership);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        return true;
    }

    @Transactional
    public boolean markNotificationAsRead(Integer notificationId) {
        User currentUser = getAuthenticatedUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.getRecipient().getIdUser().equals(currentUser.getIdUser())) {
            throw new UnauthorizedException("Esta notificación no te pertenece");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public boolean markAllNotificationsAsRead() {
        User currentUser = getAuthenticatedUser();
        notificationRepository.markAllAsReadByRecipient(currentUser);
        return true;
    }

    public List<GroupDTO> getMyGroups() {
        User currentUser = getAuthenticatedUser();
        List<GroupMember> memberships = groupMemberRepository.findAcceptedMembershipsByUserId(currentUser.getIdUser());

        return memberships.stream()
                .map(gm -> toGroupDTO(gm.getGroup()))
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getNotifications() {
        User currentUser = getAuthenticatedUser();
        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(currentUser);

        return notifications.stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getNotificationsPaginated(int page, int size) {
        User currentUser = getAuthenticatedUser();
        Page<Notification> notifPage = notificationRepository.findByRecipientOrderByCreatedAtDesc(
                currentUser, PageRequest.of(page, size));

        List<NotificationDTO> content = notifPage.getContent().stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", notifPage.getTotalElements());
        result.put("totalPages", notifPage.getTotalPages());
        result.put("number", notifPage.getNumber());
        result.put("size", notifPage.getSize());
        return result;
    }

    public GroupDTO getGroupDetails(Integer groupId) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        groupMemberRepository.findAcceptedMember(groupId, currentUser.getIdUser())
                .orElseThrow(() -> new UnauthorizedException("No tienes acceso a este grupo"));

        return toGroupDTOWithDetails(group);
    }

    public void validateGroupMembership(Integer groupId, Integer userId) {
        groupMemberRepository.findAcceptedMember(groupId, userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "No tienes permisos para editar listas de este grupo"));
    }

    private GroupDTO toGroupDTO(Group group) {
        List<GroupMemberDTO> memberDTOs = group.getMembers() != null
                ? group.getMembers().stream().map(this::toGroupMemberDTO).collect(Collectors.toList())
                : Collections.emptyList();

        return new GroupDTO(
                group.getGroupId(),
                group.getName(),
                group.getGroupCode(),
                group.getOwner().getRealUsername(),
                group.getOwner().getIdUser(),
                group.getCreatedAt(),
                memberDTOs,
                Collections.emptyList()
        );
    }

    private GroupDTO toGroupDTOWithDetails(Group group) {
        List<GroupMemberDTO> memberDTOs = group.getMembers() != null
                ? group.getMembers().stream().map(this::toGroupMemberDTO).collect(Collectors.toList())
                : Collections.emptyList();

        var lists = shoppingListRepository.findByGroup_GroupId(group.getGroupId());
        List<ShoppingListDTO> listDTOs = lists.stream()
                .map(sl -> new ShoppingListDTO(
                        sl.getListId(),
                        sl.getName(),
                        sl.getUser().getIdUser(),
                        sl.getUser().getRealUsername(),
                        sl.getGroup() != null ? sl.getGroup().getGroupId() : null,
                        sl.getGroup() != null ? sl.getGroup().getName() : null,
                        sl.getCreatedAt(),
                        sl.getItems() != null
                                ? sl.getItems().stream()
                                    .map(item -> new ListItemDTO(
                                            item.getItemId(),
                                            item.getProduct() != null ? item.getProduct().getProductId() : null,
                                            item.getProduct() != null ? item.getProduct().getName() : item.getGenericName(),
                                            item.getProduct() != null ? item.getProduct().getImageUrl() : null,
                                            item.getQuantity(),
                                            item.getChecked(),
                                            item.getProduct() == null,
                                            (String) null
                                    ))
                                    .collect(Collectors.toList())
                                : Collections.<ListItemDTO>emptyList()
                ))
                .collect(Collectors.toList());

        return new GroupDTO(
                group.getGroupId(),
                group.getName(),
                group.getGroupCode(),
                group.getOwner().getRealUsername(),
                group.getOwner().getIdUser(),
                group.getCreatedAt(),
                memberDTOs,
                listDTOs
        );
    }

    private GroupMemberDTO toGroupMemberDTO(GroupMember gm) {
        return new GroupMemberDTO(
                gm.getId(),
                gm.getUser() != null ? gm.getUser().getIdUser() : null,
                gm.getUser() != null ? gm.getUser().getRealUsername() : null,
                gm.getUser() != null ? gm.getUser().getEmail() : null,
                gm.getStatus().name()
        );
    }

    private NotificationDTO toNotificationDTO(Notification n) {
        return new NotificationDTO(
                n.getNotificationId(),
                n.getMessage(),
                n.getType().name(),
                n.getIsRead(),
                n.getRelatedGroup() != null ? n.getRelatedGroup().getGroupId() : null,
                n.getRelatedGroup() != null ? n.getRelatedGroup().getName() : null,
                n.getCreatedAt()
        );
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
