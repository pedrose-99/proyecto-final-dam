package com.smartcart.smartcart.config;

import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.scraping.service.CsvImportService;
import com.smartcart.smartcart.modules.scraping.service.ProductSyncService;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import com.smartcart.smartcart.modules.user.entity.Role;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.RoleRepository;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;
    private final ProductStoreRepository productStoreRepository;
    private final CsvImportService csvImportService;
    private final MockPriceHistoryGenerator mockPriceHistoryGenerator;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initStores();
        initProducts();
        mockPriceHistoryGenerator.generateIfNeeded();
    }

    private void initRoles() {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setPermissions("READ");
            roleRepository.save(userRole);
            log.info("Rol USER creado");
        }

        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setPermissions("READ,WRITE,DELETE,ADMIN");
            roleRepository.save(adminRole);
            log.info("Rol ADMIN creado");
        }
    }

    private void initAdminUser() {
        if (userRepository.countByRole_Name("ADMIN") == 0) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@admin.com");
            admin.setPassword(passwordEncoder.encode("pass"));
            admin.setRole(adminRole);
            userRepository.save(admin);
            log.info("Usuario ADMIN por defecto creado (admin@admin.com / pass)");
        }
    }

    private void initStores() {
        createOrUpdateStore("mercadona", "Mercadona", "https://www.mercadona.es",
                "/assets/images/stores/mercadona.svg", "https://tienda.mercadona.es/api");
        createOrUpdateStore("dia", "Dia", "https://www.dia.es",
                "/assets/images/stores/dia.svg", null);
        createOrUpdateStore("carrefour", "Carrefour", "https://www.carrefour.es",
                "/assets/images/stores/carrefour.svg", "https://www.carrefour.es/supermercado");
        createOrUpdateStore("ahorramas", "Ahorramas", "https://www.ahorramas.com",
                "/assets/images/stores/ahorramas.svg", null);
        createOrUpdateStore("alcampo", "Alcampo", "https://www.alcampo.es",
                "/assets/images/stores/alcampo.svg", "https://www.compraonline.alcampo.es");
    }

    private void createOrUpdateStore(String slug, String name, String website, String logo, String scrapingUrl) {
        var existing = storeRepository.findBySlug(slug);
        if (existing.isEmpty()) {
            Store store = new Store();
            store.setName(name);
            store.setSlug(slug);
            store.setWebsite(website);
            store.setLogo(logo);
            store.setActive(true);
            store.setScrapingUrl(scrapingUrl);
            storeRepository.save(store);
            log.info("Tienda {} creada", name);
        } else {
            Store store = existing.get();
            if (!logo.equals(store.getLogo())) {
                store.setLogo(logo);
                storeRepository.save(store);
                log.info("Tienda {} - logo actualizado", name);
            }
        }
    }

    private void initProducts() {
        importCsvIfEmpty("carrefour", "data/products_carrefour.csv");
        importCsvIfEmpty("dia", "data/products_dia.csv");
        importCsvIfEmpty("mercadona", "data/products_mercadona.csv");
        importCsvIfEmpty("alcampo", "data/products_alcampo.csv");
        importCsvIfEmpty("ahorramas", "data/products_ahorramas.csv");
    }

    private void importCsvIfEmpty(String storeSlug, String classpathResource) {
        storeRepository.findBySlug(storeSlug).ifPresent(store -> {
            Long count = productStoreRepository.countProductsByStoreId(store.getStoreId());
            if (count == 0) {
                // Verificar que el recurso existe antes de intentar importar
                if (getClass().getClassLoader().getResource(classpathResource) == null) {
                    log.info("[{}] CSV no encontrado en classpath ({}), saltando importacion", storeSlug, classpathResource);
                    return;
                }
                log.info("[{}] No tiene productos, importando desde {}...", storeSlug, classpathResource);
                try {
                    ProductSyncService.SyncResult result = csvImportService.importFromClasspath(classpathResource, storeSlug);
                    log.info("[{}] Importacion completada: {} creados, {} errores",
                            storeSlug, result.created, result.errors);
                } catch (Exception e) {
                    log.error("[{}] Error al importar CSV: {}", storeSlug, e.getMessage());
                }
            } else {
                log.info("[{}] Ya tiene {} productos, saltando importacion", storeSlug, count);
            }
        });
    }
}
