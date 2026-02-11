package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.service.CsvImportService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class CsvImportController
{

    private final CsvImportService csvImportService;

    @PostMapping("/csv")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("filePath") String filePath,
            @RequestParam("storeSlug") String storeSlug)
    {
        log.info("CSV import requested: file={}, store={}", filePath, storeSlug);

        long start = System.currentTimeMillis();
        ProductSyncService.SyncResult result = csvImportService.importFromFile(filePath, storeSlug);
        long durationSeconds = (System.currentTimeMillis() - start) / 1000;

        return ResponseEntity.ok(Map.of(
                "store", storeSlug,
                "created", result.created,
                "updated", result.updated,
                "unchanged", result.unchanged,
                "errors", result.errors,
                "total", result.getTotal(),
                "durationSeconds", durationSeconds
        ));
    }
}
