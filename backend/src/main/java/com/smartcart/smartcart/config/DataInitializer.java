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

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initStores();
        initProducts();
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
        if (storeRepository.findBySlug("mercadona").isEmpty()) {
            Store mercadona = new Store();
            mercadona.setName("Mercadona");
            mercadona.setSlug("mercadona");
            mercadona.setWebsite("https://www.mercadona.es");
            mercadona.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/0/06/Mercadona_Logo.svg/512px-Mercadona_Logo.svg.png");
            mercadona.setActive(true);
            mercadona.setScrapingUrl("https://tienda.mercadona.es/api");
            storeRepository.save(mercadona);
            log.info("Tienda Mercadona creada");
        }

        if (storeRepository.findBySlug("dia").isEmpty()) {
            Store dia = new Store();
            dia.setName("Dia");
            dia.setSlug("dia");
            dia.setWebsite("https://www.dia.es");
            dia.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/Grupo_DIA_logo.svg/512px-Grupo_DIA_logo.svg.png");
            dia.setActive(true);
            storeRepository.save(dia);
            log.info("Tienda Dia creada");
        }

        if (storeRepository.findBySlug("carrefour").isEmpty()) {
            Store carrefour = new Store();
            carrefour.setName("Carrefour");
            carrefour.setSlug("carrefour");
            carrefour.setWebsite("https://www.carrefour.es");
            carrefour.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Logo_Carrefour.svg/512px-Logo_Carrefour.svg.png");
            carrefour.setActive(true);
            carrefour.setScrapingUrl("https://www.carrefour.es/supermercado");
            storeRepository.save(carrefour);
            log.info("Tienda Carrefour creada");
        }

        if (storeRepository.findBySlug("alcampo").isEmpty()) {
            Store alcampo = new Store();
            alcampo.setName("Alcampo");
            alcampo.setSlug("alcampo");
            alcampo.setWebsite("https://www.alcampo.es");
            alcampo.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Logo_Alcampo.svg/512px-Logo_Alcampo.svg.png");
            alcampo.setActive(true);
            alcampo.setScrapingUrl("https://www.compraonline.alcampo.es");
            storeRepository.save(alcampo);
            log.info("Tienda Alcampo creada");
        }
    }

    private void initProducts() {
        importCsvIfEmpty("carrefour", "data/products_carrefour.csv");
        importCsvIfEmpty("dia", "data/products_dia.csv");
    }

    private void importCsvIfEmpty(String storeSlug, String classpathResource) {
        storeRepository.findBySlug(storeSlug).ifPresent(store -> {
            Long count = productStoreRepository.countProductsByStoreId(store.getStoreId());
            if (count == 0) {
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
