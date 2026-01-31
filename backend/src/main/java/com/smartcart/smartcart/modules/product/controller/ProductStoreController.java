package com.smartcart.smartcart.modules.product.controller;



import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.service.ProductStoreService;



@Controller
public class ProductStoreController {

    private final ProductStoreService productStoreService;

    public ProductStoreController(ProductStoreService productStoreService) {
        this.productStoreService = productStoreService;
    }

    @QueryMapping
    public List<ProductStore> storesByProduct(@Argument Integer productId) {
        return productStoreService.findStoresByProductId(productId);
    }
    
    @MutationMapping
    public ProductStore linkProductToStore(
            @Argument Integer productId, 
            @Argument Integer storeId, 
            @Argument String url, 
            @Argument Integer stock) {
        return productStoreService.link(productId, storeId, url, stock);
    }
}