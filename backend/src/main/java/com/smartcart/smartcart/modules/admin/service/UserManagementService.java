package com.smartcart.smartcart.modules.admin.service;

import com.smartcart.smartcart.modules.admin.dto.ChangeRoleRequest;
import com.smartcart.smartcart.modules.admin.dto.CreateUserRequest;
import com.smartcart.smartcart.modules.admin.dto.UserAdminDTO;
import com.smartcart.smartcart.modules.auth.repository.TokenRepository;
import com.smartcart.smartcart.modules.user.entity.Role;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.RoleRepository;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserAdminDTO> getUsers(String role, String search, Pageable pageable) {
        Page<User> users;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasRole = role != null && !role.isBlank();

        if (hasSearch && hasRole) {
            users = userRepository.searchByEmailOrUsernameAndRole(search, role, pageable);
        } else if (hasSearch) {
            users = userRepository.searchByEmailOrUsername(search, pageable);
        } else if (hasRole) {
            users = userRepository.findByRole_NameOrderByCreatedAtDesc(role, pageable);
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return users.map(UserAdminDTO::fromEntity);
    }

    @Transactional
    public UserAdminDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("El username ya está en uso");
        }

        Role role = roleRepository.findByName(request.roleName().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.roleName()));

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User saved = userRepository.save(user);
        log.info("Admin created user: {} with role {}", saved.getEmail(), role.getName());
        return UserAdminDTO.fromEntity(saved);
    }

    @Transactional
    public UserAdminDTO changeUserRole(Integer userId, ChangeRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        Role newRole = roleRepository.findByName(request.roleName().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.roleName()));

        String oldRole = user.getRole().getName();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        revokeAllUserTokens(user);

        log.info("User {} role changed: {} -> {}", user.getEmail(), oldRole, newRole.getName());
        return UserAdminDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new RuntimeException("No puedes eliminarte a ti mismo");
        }

        revokeAllUserTokens(user);

        log.info("Admin deleted user: {}", user.getEmail());
        userRepository.delete(user);
    }

    private void revokeAllUserTokens(User user) {
        var validTokens = tokenRepository.findAllValidTokensByUser(user.getIdUser());
        validTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }
}
