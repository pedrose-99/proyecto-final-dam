package com.smartcart.smartcart.modules.product.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.product.dto.PriceHistoryDTO;
import com.smartcart.smartcart.modules.product.dto.PriceUpdateDTO;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.service.PriceHistoryService;

@Controller
public class PriceHistoryController {
    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @QueryMapping
    public List<PriceHistoryDTO> priceHistoryByProduct(@Argument Integer productId) {
        return priceHistoryService.findByProductId(productId);
    }

    @QueryMapping
    public List<PriceHistoryDTO> priceHistoryByProductAndStore(@Argument Integer productId, @Argument Integer storeId) {
        return priceHistoryService.priceHistoryByProductAndStore(productId, storeId);
    }

    @MutationMapping
    public PriceHistoryDTO registerNewPrice(@Argument PriceUpdateDTO input) { // Usamos el DTO como entrada
        return priceHistoryService.register(input);
    }
}