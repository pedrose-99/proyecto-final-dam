package com.smartcart.smartcart.modules.store.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;

@Service
public class StoreService {
    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<Store> findAll() { 
        return storeRepository.findAll(); 
    }

    public Store findById(Integer id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada con ID: " + id));
    }

    public Store create(String name, String website, String logo) {
        Store s = new Store();
        s.setName(name);
        s.setWebsite(website);
        s.setLogo(logo);
        s.setActive(true);
        //nombre en minúsculas y sin espacios
        s.setSlug(name.toLowerCase().trim().replace(" ", "-"));
        return storeRepository.save(s);
    }

    public Store update(Integer id, String name, Boolean active) {
        Store s = findById(id);
        if (name != null) {
            s.setName(name);
            s.setSlug(name.toLowerCase().trim().replace(" ", "-"));
        }
        if (active != null) s.setActive(active);
        return storeRepository.save(s);
    }

    public Boolean delete(Integer id) {
        storeRepository.deleteById(id);
        return true;
    }
}
