package com.smartcart.smartcart.modules.admin.dto;

import com.smartcart.smartcart.modules.scraping.dto.ScrapeLogDTO;

import java.util.List;
import java.util.Map;

public record AdminStatsDTO(
    long totalProducts,
    long totalUsers,
    long totalStores,
    long totalCategories,
    List<StoreProductCountDTO> productsByStore,
    Map<String, Long> usersByRole,
    List<ScrapeLogDTO> recentScrapeLogs
) {}
