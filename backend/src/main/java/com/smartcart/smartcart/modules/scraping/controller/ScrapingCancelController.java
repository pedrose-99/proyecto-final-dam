package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.service.ScrapingJobRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/scraping")
@RequiredArgsConstructor
public class ScrapingCancelController {

    private final ScrapingJobRegistry jobRegistry;

    @PostMapping("/{storeSlug}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable String storeSlug) {
        log.info("Cancelando scraping de {}", storeSlug);
        boolean cancelled = jobRegistry.cancel(storeSlug);
        return ResponseEntity.ok(Map.of(
                "store", storeSlug,
                "cancelled", cancelled
        ));
    }
}
