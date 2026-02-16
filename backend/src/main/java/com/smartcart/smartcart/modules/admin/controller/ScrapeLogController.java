package com.smartcart.smartcart.modules.admin.controller;

import com.smartcart.smartcart.modules.scraping.dto.ScrapeErrorDTO;
import com.smartcart.smartcart.modules.scraping.dto.ScrapeLogDTO;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/scrape-logs")
@RequiredArgsConstructor
public class ScrapeLogController {

    private final ScrapeLogService scrapeLogService;

    @GetMapping
    public ResponseEntity<Page<ScrapeLogDTO>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String status) {
        Page<ScrapeLogDTO> logs = scrapeLogService.getLogs(store, status, PageRequest.of(page, size))
                .map(ScrapeLogDTO::fromEntity);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScrapeLogDTO> getLog(@PathVariable Long id) {
        ScrapeLog log = scrapeLogService.getLogById(id)
                .orElseThrow(() -> new RuntimeException("Log no encontrado con id: " + id));
        return ResponseEntity.ok(ScrapeLogDTO.fromEntity(log));
    }

    @GetMapping("/{id}/errors")
    public ResponseEntity<List<ScrapeErrorDTO>> getErrors(@PathVariable Long id) {
        List<ScrapeErrorDTO> errors = scrapeLogService.getErrors(id).stream()
                .map(ScrapeErrorDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(errors);
    }
}
