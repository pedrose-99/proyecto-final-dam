package com.smartcart.smartcart.modules.product.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.product.dto.BasketOptimizationDTO;
import com.smartcart.smartcart.modules.product.dto.ProductComparisonDTO;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.service.ProductService;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─── CRUD Queries ─────────────────────────────────────

    @QueryMapping
    public List<ProductDTO> allProducts() {
        return productService.findAll();
    }

    @QueryMapping
    public ProductDTO productByEan(@Argument String ean) {
        return productService.findByEan(ean);
    }

    // ─── NAVEGACIÓN: Filtrar por categoría ─────────────────

    @QueryMapping
    public List<ProductDTO> productsByCategory(@Argument Integer categoryId) {
        return productService.findByCategory(categoryId);
    }

    // ─── COMPARADOR: Producto con todos sus precios ────────

    @QueryMapping
    public ProductComparisonDTO compareProduct(@Argument Integer productId) {
        return productService.compareProduct(productId);
    }

    // ─── OPTIMIZACIÓN DE CESTA ─────────────────────────────

    @QueryMapping
    public BasketOptimizationDTO optimizeBasket(@Argument List<Integer> productIds) {
        return productService.optimizeBasket(productIds);
    }

    // ─── CRUD Mutations ───────────────────────────────────

    @MutationMapping
    public ProductDTO createProduct(@Argument String name, @Argument String ean,
                                    @Argument String brand, @Argument Integer categoryId) {
        return productService.create(name, ean, brand, categoryId);
    }

    @MutationMapping
    public ProductDTO updateProduct(@Argument Integer id, @Argument String name,
                                    @Argument String brand, @Argument String image_url) {
        return productService.update(id, name, brand, image_url);
    }

    @MutationMapping
    public Boolean deleteProduct(@Argument Integer id) {
        return productService.delete(id);
    }
}
