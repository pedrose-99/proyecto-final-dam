package com.smartcart.smartcart.modules.scraping.controller;

import com.smartcart.smartcart.modules.scraping.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
public class CsvExportController
{

    private final CsvExportService csvExportService;

    @GetMapping("/csv/{storeSlug}")
    public ResponseEntity<byte[]> exportCsvByStore(@PathVariable String storeSlug)
    {
        log.info("CSV export requested for store: {}", storeSlug);

        byte[] csv = csvExportService.exportByStore(storeSlug);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products_" + storeSlug + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/csv/all")
    public ResponseEntity<byte[]> exportAllCsv()
    {
        log.info("CSV export requested for all stores (ZIP)");

        byte[] zip = csvExportService.exportAllAsZip();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products_all.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}
