package com.smartcart.smartcart.modules.group.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    // ==================== CREACIÓN DE GRUPO ====================

    @Transactional
    public GroupDTO createGroup(String name) {
        User currentUser = getAuthenticatedUser();

        Group group = new Group();
        group.setName(name);
        group.setOwner(currentUser);

        // Generar código único
        String code;
        do {
            code = Group.generateGroupCode();
        } while (groupRepository.existsByGroupCode(code));
        group.setGroupCode(code);

        group = groupRepository.save(group);

        // El creador se añade como miembro ACCEPTED
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroup(group);
        ownerMember.setUser(currentUser);
        ownerMember.setStatus(MemberStatus.ACCEPTED);
        groupMemberRepository.save(ownerMember);

        return toGroupDTO(group);
    }

    // ==================== INVITACIÓN TRIPLE VÍA ====================

    @Transactional
    public boolean inviteToGroup(Integer groupId, String target) {
        User currentUser = getAuthenticatedUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // Verificar que el usuario actual es miembro ACCEPTED del grupo
        groupMemberRepository.findAcceptedMember(groupId, currentUser.getIdUser())
                .orElseThrow(() -> new UnauthorizedException("No tienes permisos para invitar a este grupo"));

        // Detectar si es email o username
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

        // Crear GroupMember PENDING
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(targetUser);
        member.setStatus(MemberStatus.PENDING);
        groupMemberRepository.save(member);

        // Crear Notificación de invitación
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

            // Usuario existe: igual que por username
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

    // ==================== UNIRSE POR CÓDIGO ====================

    @Transactional
    public GroupDTO joinGroupByCode(String code) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findByGroupCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código de grupo inválido"));

        if (groupMemberRepository.existsByGroupAndUser(group, currentUser)) {
            throw new BadRequestException("Ya eres miembro de este grupo");
        }

        // Unirse al instante con status ACCEPTED
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(currentUser);
        member.setStatus(MemberStatus.ACCEPTED);
        groupMemberRepository.save(member);

        // Notificar al owner
        Notification notification = new Notification();
        notification.setRecipient(group.getOwner());
        notification.setMessage(currentUser.getRealUsername() + " se ha unido al grupo '" + group.getName() + "'");
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedGroup(group);
        notificationRepository.save(notification);

        return toGroupDTO(group);
    }

    // ==================== RESPONDER A INVITACIÓN ====================

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

            // Notificar al owner que se aceptó
            Notification acceptNotif = new Notification();
            acceptNotif.setRecipient(group.getOwner());
            acceptNotif.setMessage(currentUser.getRealUsername() + " aceptó la invitación al grupo '" + group.getName() + "'");
            acceptNotif.setType(NotificationType.SYSTEM);
            acceptNotif.setRelatedGroup(group);
            notificationRepository.save(acceptNotif);
        } else {
            groupMemberRepository.delete(membership);
        }

        // Marcar la notificación como leída
        notification.setIsRead(true);
        notificationRepository.save(notification);

        return true;
    }

    // ==================== QUERIES ====================

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

    public GroupDTO getGroupDetails(Integer groupId) {
        User currentUser = getAuthenticatedUser();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // Verificar que el usuario es miembro ACCEPTED
        groupMemberRepository.findAcceptedMember(groupId, currentUser.getIdUser())
                .orElseThrow(() -> new UnauthorizedException("No tienes acceso a este grupo"));

        return toGroupDTOWithDetails(group);
    }

    // ==================== SEGURIDAD: Validación de pertenencia a grupo ====================

    public void validateGroupMembership(Integer groupId, Integer userId) {
        groupMemberRepository.findAcceptedMember(groupId, userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "No tienes permisos para editar listas de este grupo"));
    }

    // ==================== MAPPERS ====================

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

    // ==================== UTILS ====================

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
