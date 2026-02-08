package com.smartcart.smartcart.modules.product.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.product.dto.ProductAlertDTO;
import com.smartcart.smartcart.modules.product.service.ProductAlertService;

@Controller
public class ProductAlertController {

    private final ProductAlertService productAlertService;

    public ProductAlertController(ProductAlertService productAlertService) {
        this.productAlertService = productAlertService;
    }

    // ─── ALERTAS: Queries ──────────────────────────────────

    @QueryMapping
    public List<ProductAlertDTO> activeAlerts() {
        return productAlertService.getActiveAlerts();
    }

    @QueryMapping
    public List<ProductAlertDTO> alertsByProduct(@Argument Integer productId) {
        return productAlertService.getAlertsByProduct(productId);
    }

    // ─── ALERTAS: Mutations ────────────────────────────────

    @MutationMapping
    public ProductAlertDTO createAlert(@Argument Integer productId, @Argument Double targetPrice) {
        return productAlertService.createAlert(productId, targetPrice);
    }

    @MutationMapping
    public ProductAlertDTO deactivateAlert(@Argument Integer alertId) {
        return productAlertService.deactivateAlert(alertId);
    }
}
