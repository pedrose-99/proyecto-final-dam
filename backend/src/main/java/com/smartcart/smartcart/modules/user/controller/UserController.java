package com.smartcart.smartcart.modules.user.controller;

import com.smartcart.smartcart.modules.user.dto.ProfileUpdateRequest;
import com.smartcart.smartcart.modules.user.dto.UserDTO;
import com.smartcart.smartcart.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController 
{
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() 
    {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) 
    {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
}