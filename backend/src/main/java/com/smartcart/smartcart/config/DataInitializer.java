package com.smartcart.smartcart.config;

import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import com.smartcart.smartcart.modules.user.entity.Role;
import com.smartcart.smartcart.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;

    @Override
    public void run(String... args) {
        initRoles();
        initStores();
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

        if (storeRepository.findBySlug("carrefour").isEmpty()) {
            Store carrefour = new Store();
            carrefour.setName("Carrefour");
            carrefour.setSlug("carrefour");
            carrefour.setWebsite("https://www.carrefour.es");
            carrefour.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5b/Carrefour_logo.svg/512px-Carrefour_logo.svg.png");
            carrefour.setActive(false);
            storeRepository.save(carrefour);
            log.info("Tienda Carrefour creada");
        }

        if (storeRepository.findBySlug("dia").isEmpty()) {
            Store dia = new Store();
            dia.setName("Dia");
            dia.setSlug("dia");
            dia.setWebsite("https://www.dia.es");
            dia.setLogo("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/Grupo_DIA_logo.svg/512px-Grupo_DIA_logo.svg.png");
            dia.setActive(false);
            storeRepository.save(dia);
            log.info("Tienda Dia creada");
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
}
