package com.smartcart.smartcart.modules.admin.controller;

import com.smartcart.smartcart.modules.admin.dto.ChangeRoleRequest;
import com.smartcart.smartcart.modules.admin.dto.CreateUserRequest;
import com.smartcart.smartcart.modules.admin.dto.UserAdminDTO;
import com.smartcart.smartcart.modules.admin.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<Page<UserAdminDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(
                userManagementService.getUsers(role, search, PageRequest.of(page, size)));
    }

    @PostMapping
    public ResponseEntity<UserAdminDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(userManagementService.createUser(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserAdminDTO> changeRole(
            @PathVariable Integer id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(userManagementService.changeUserRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
