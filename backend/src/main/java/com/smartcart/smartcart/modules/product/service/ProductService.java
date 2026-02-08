package com.smartcart.smartcart.modules.product.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;
import com.smartcart.smartcart.modules.product.dto.BasketItemDTO;
import com.smartcart.smartcart.modules.product.dto.BasketOptimizationDTO;
import com.smartcart.smartcart.modules.product.dto.ProductComparisonDTO;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.dto.StoreBasketDTO;
import com.smartcart.smartcart.modules.product.dto.StorePriceDTO;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.mapper.ProductMapper;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductStoreRepository productStoreRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductStoreRepository productStoreRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productStoreRepository = productStoreRepository;
    }

    // ─── CRUD ──────────────────────────────────────────────

    public List<ProductDTO> findAll() {
        return productRepository.findAll().stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    public ProductDTO findByEan(String ean) {
        Product p = productRepository.findByEan(ean)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con EAN: " + ean));
        return ProductMapper.toDTO(p);
    }

    public ProductDTO create(String name, String ean, String brand, Integer categoryId) {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Product p = new Product();
        p.setName(name);
        p.setEan(ean);
        p.setBrand(brand);
        p.setCategoryId(cat);
        return ProductMapper.toDTO(productRepository.save(p));
    }

    public ProductDTO update(Integer id, String name, String brand, String imageUrl) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (name != null) p.setName(name);
        if (brand != null) p.setBrand(brand);
        if (imageUrl != null) p.setImageUrl(imageUrl);
        return ProductMapper.toDTO(productRepository.save(p));
    }

    public Boolean delete(Integer id) {
        productRepository.deleteById(id);
        return true;
    }

    // ─── NAVEGACIÓN: Filtrar productos por categoría ───────

    public List<ProductDTO> findByCategory(Integer categoryId) {
        return productRepository.findByCategoryId_CategoryId(categoryId).stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    // ─── COMPARADOR: Producto con precios en todas las tiendas ─

    public ProductComparisonDTO compareProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<ProductStore> productStores = productStoreRepository.findByProductId_ProductId(productId);

        List<StorePriceDTO> storePrices = productStores.stream()
                .map(ps -> {
                    StorePriceDTO sp = new StorePriceDTO();
                    sp.setStoreId(ps.getStoreId().getStoreId());
                    sp.setStoreName(ps.getStoreId().getName());
                    sp.setStoreLogo(ps.getStoreId().getLogo());
                    sp.setCurrentPrice(ps.getCurrentPrice());
                    sp.setAvailable(ps.getAvailable());
                    sp.setStock(ps.getStock());
                    sp.setUrl(ps.getUrl());
                    return sp;
                })
                .toList();

        // Encontrar el precio más barato entre los disponibles
        StorePriceDTO bestPrice = storePrices.stream()
                .filter(sp -> Boolean.TRUE.equals(sp.getAvailable()) && sp.getCurrentPrice() != null)
                .min(Comparator.comparingDouble(StorePriceDTO::getCurrentPrice))
                .orElse(null);

        ProductComparisonDTO comparison = new ProductComparisonDTO();
        comparison.setProductId(product.getProductId());
        comparison.setName(product.getName());
        comparison.setBrand(product.getBrand());
        comparison.setEan(product.getEan());
        comparison.setImageUrl(product.getImageUrl());
        if (product.getCategoryId() != null) {
            comparison.setCategoryName(product.getCategoryId().getName());
        }
        comparison.setStorePrices(storePrices);
        comparison.setBestPrice(bestPrice);

        return comparison;
    }

    // ─── OPTIMIZACIÓN DE CESTA ─────────────────────────────

    public BasketOptimizationDTO optimizeBasket(List<Integer> productIds) {
        // Mapa: storeId → StoreBasketDTO
        Map<Integer, StoreBasketDTO> storeMap = new HashMap<>();
        int totalProducts = productIds.size();

        for (Integer productId : productIds) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) continue;

            List<ProductStore> productStores = productStoreRepository.findByProductId_ProductId(productId);

            for (ProductStore ps : productStores) {
                Integer storeId = ps.getStoreId().getStoreId();

                StoreBasketDTO basket = storeMap.computeIfAbsent(storeId, k -> {
                    StoreBasketDTO b = new StoreBasketDTO();
                    b.setStoreId(storeId);
                    b.setStoreName(ps.getStoreId().getName());
                    b.setStoreLogo(ps.getStoreId().getLogo());
                    b.setTotalCost(0.0);
                    b.setAvailableProducts(0);
                    b.setTotalProducts(totalProducts);
                    b.setItems(new ArrayList<>());
                    return b;
                });

                BasketItemDTO item = new BasketItemDTO();
                item.setProductId(productId);
                item.setProductName(product.getName());
                item.setPrice(ps.getCurrentPrice());
                item.setAvailable(Boolean.TRUE.equals(ps.getAvailable()));
                basket.getItems().add(item);

                if (Boolean.TRUE.equals(ps.getAvailable()) && ps.getCurrentPrice() != null) {
                    basket.setTotalCost(basket.getTotalCost() + ps.getCurrentPrice());
                    basket.setAvailableProducts(basket.getAvailableProducts() + 1);
                }
            }
        }

        List<StoreBasketDTO> storeOptions = new ArrayList<>(storeMap.values());

        // Ordenar: primero las tiendas que tengan TODOS los productos, luego por precio
        storeOptions.sort((a, b) -> {
            // Priorizar tiendas con más productos disponibles
            int availableCompare = Integer.compare(b.getAvailableProducts(), a.getAvailableProducts());
            if (availableCompare != 0) return availableCompare;
            // A igualdad de productos, la más barata
            return Double.compare(a.getTotalCost(), b.getTotalCost());
        });

        // La tienda más barata que tenga TODOS los productos, o la que más tenga
        StoreBasketDTO cheapest = storeOptions.isEmpty() ? null : storeOptions.get(0);

        BasketOptimizationDTO result = new BasketOptimizationDTO();
        result.setStoreOptions(storeOptions);
        result.setCheapestStore(cheapest);
        result.setTotalProducts(totalProducts);

        return result;
    }
}
