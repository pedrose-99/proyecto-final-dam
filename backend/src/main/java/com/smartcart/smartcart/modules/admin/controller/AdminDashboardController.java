package com.smartcart.smartcart.modules.admin.controller;

import com.smartcart.smartcart.modules.admin.dto.AdminStatsDTO;
import com.smartcart.smartcart.modules.admin.dto.ServiceHealthDTO;
import com.smartcart.smartcart.modules.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<ServiceHealthDTO> getHealth() {
        return ResponseEntity.ok(adminStatsService.getServiceHealth());
    }
}
