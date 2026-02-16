package com.smartcart.smartcart.modules.admin.service;

import com.smartcart.smartcart.modules.admin.dto.AdminStatsDTO;
import com.smartcart.smartcart.modules.admin.dto.ServiceHealthDTO;
import com.smartcart.smartcart.modules.admin.dto.StoreProductCountDTO;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.scraping.dto.ScrapeLogDTO;
import com.smartcart.smartcart.modules.scraping.service.PythonScraperService;
import com.smartcart.smartcart.modules.scraping.service.ScrapeLogService;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ScrapeLogService scrapeLogService;
    private final PythonScraperService pythonScraperService;

    public AdminStatsDTO getStats() {
        List<Store> stores = storeRepository.findAll();

        List<StoreProductCountDTO> productsByStore = stores.stream()
                .map(store -> new StoreProductCountDTO(
                        store.getName(),
                        store.getSlug(),
                        productStoreRepository.countProductsByStoreId(store.getStoreId())
                ))
                .toList();

        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("USER", userRepository.countByRole_Name("USER"));
        usersByRole.put("ADMIN", userRepository.countByRole_Name("ADMIN"));

        List<ScrapeLogDTO> recentLogs = scrapeLogService.getRecentLogs().stream()
                .map(ScrapeLogDTO::fromEntity)
                .toList();

        return new AdminStatsDTO(
                productRepository.count(),
                userRepository.count(),
                storeRepository.count(),
                categoryRepository.count(),
                productsByStore,
                usersByRole,
                recentLogs
        );
    }

    public ServiceHealthDTO getServiceHealth() {
        String dbStatus;
        try {
            userRepository.count();
            dbStatus = "UP";
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        String pythonStatus;
        try {
            pythonStatus = pythonScraperService.isHealthy() ? "UP" : "DOWN";
        } catch (Exception e) {
            pythonStatus = "DOWN";
        }

        return new ServiceHealthDTO("UP", dbStatus, pythonStatus);
    }
}
