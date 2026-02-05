package com.smartcart.smartcart.modules.store.controller;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import com.smartcart.smartcart.modules.store.dto.StoreDTO;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.service.StoreService;

@Controller
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @QueryMapping
    public List<StoreDTO> allStores() {
        return storeService.findAllWithProductCount();
    }

    @QueryMapping
    public Store storeById(@Argument Integer id) {
        return storeService.findById(id);
    }

    @MutationMapping
    public Store createStore(@Argument String name, @Argument String website, @Argument String logo) {
        return storeService.create(name, website, logo);
    }

    @MutationMapping
    public Store updateStore(@Argument Integer id, @Argument String name, @Argument Boolean active) {
        return storeService.update(id, name, active);
    }

    @MutationMapping
    public Boolean deleteStore(@Argument Integer id) {
        return storeService.delete(id);
    }
}
