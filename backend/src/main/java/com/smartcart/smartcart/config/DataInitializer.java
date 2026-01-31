package com.smartcart.smartcart.config;

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

    @Override
    public void run(String... args) {
        initRoles();
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
}
