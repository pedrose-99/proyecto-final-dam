package com.smartcart.smartcart.modules.admin.controller;

import com.smartcart.smartcart.modules.admin.dto.StoreAdminDTO;
import com.smartcart.smartcart.modules.admin.dto.UpdateStoreRequest;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreManagementController {

    private final StoreRepository storeRepository;
    private final ProductStoreRepository productStoreRepository;
    private final ScrapeLogService scrapeLogService;

    @GetMapping
    public ResponseEntity<List<StoreAdminDTO>> getStores() {
        List<Store> stores = storeRepository.findAll();

        List<StoreAdminDTO> result = stores.stream().map(store -> {
            Long productCount = productStoreRepository.countProductsByStoreId(store.getStoreId());
            LocalDateTime lastScrapeDate = null;
            String lastScrapeStatus = null;

            Optional<ScrapeLog> lastLog = scrapeLogService.getLastLog(store);
            if (lastLog.isPresent()) {
                lastScrapeDate = lastLog.get().getEndTime() != null
                    ? lastLog.get().getEndTime()
                    : lastLog.get().getStartTime();
                lastScrapeStatus = lastLog.get().getStatus().name();
            }

            return StoreAdminDTO.fromEntity(store, productCount, lastScrapeDate, lastScrapeStatus);
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreAdminDTO> updateStore(
            @PathVariable Integer id,
            @RequestBody UpdateStoreRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada con id: " + id));

        if (request.active() != null) {
            store.setActive(request.active());
        }
        if (request.scrapingUrl() != null) {
            store.setScrapingUrl(request.scrapingUrl());
        }

        Store saved = storeRepository.save(store);
        Long productCount = productStoreRepository.countProductsByStoreId(saved.getStoreId());

        return ResponseEntity.ok(StoreAdminDTO.fromEntity(saved, productCount, null, null));
    }
}
