package com.smartcart.smartcart.modules.product.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;
import com.smartcart.smartcart.modules.product.dto.BasketItemDTO;
import com.smartcart.smartcart.modules.product.dto.BasketOptimizationDTO;
import com.smartcart.smartcart.modules.product.dto.ProductComparisonDTO;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.dto.ProductPageDTO;
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
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();
    }

    public ProductPageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductDTO> content = productPage.getContent().stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();

        return new ProductPageDTO(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    public ProductDTO findByEan(String ean) {
        Product p = productRepository.findByEan(ean)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con EAN: " + ean));
        return ProductMapper.toDTO(p, false);
    }

    public List<ProductDTO> findByCategoryId(Integer categoryId) {
        return productRepository.findByCategoryId_CategoryId(categoryId).stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();
    }

    public ProductPageDTO findByCategoryIdPaginated(Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByCategoryId_CategoryId(categoryId, pageable);

        List<ProductDTO> content = productPage.getContent().stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();

        return new ProductPageDTO(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    public ProductPageDTO findByStoreIdPaginated(Integer storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByStoreId(storeId, pageable);

        List<ProductDTO> content = productPage.getContent().stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();

        return new ProductPageDTO(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    public ProductPageDTO searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchByText(query, pageable);

        List<ProductDTO> content = productPage.getContent().stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();

        return new ProductPageDTO(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    public ProductDTO create(String name, String ean, String brand, Integer categoryId) {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Product p = new Product();
        p.setName(name);
        p.setEan(ean);
        p.setBrand(brand);
        p.setCategoryId(cat);
        return ProductMapper.toDTO(productRepository.save(p), false);
    }

    public ProductDTO update(Integer id, String name, String brand, String imageUrl) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (name != null) p.setName(name);
        if (brand != null) p.setBrand(brand);
        if (imageUrl != null) p.setImageUrl(imageUrl);
        return ProductMapper.toDTO(productRepository.save(p), false);
    }

    public Boolean delete(Integer id) {
        productRepository.deleteById(id);
        return true;
    }

    // ─── NAVEGACIÓN: Filtrar productos por categoría ───────

    public List<ProductDTO> findByCategory(Integer categoryId) {
        return productRepository.findByCategoryId_CategoryId(categoryId).stream()
                .map(product -> ProductMapper.toDTO(product, false))
                .toList();
    }

    // ─── COMPARADOR: Producto con precios en todas las tiendas ─

    public ProductComparisonDTO compareProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<ProductStore> productStores = productStoreRepository.findByProductId_ProductId(productId);

        List<StorePriceDTO> storePrices = productStores.stream()
                .map(ps -> new StorePriceDTO(
                        ps.getStoreId().getStoreId(),
                        ps.getStoreId().getName(),
                        ps.getStoreId().getLogo(),
                        ps.getStoreId().getWebsite(),
                        ps.getCurrentPrice(),
                        ps.getAvailable(),
                        ps.getStock(),
                        ps.getUrl(),
                        ps.getExternaId()
                ))
                .toList();

        StorePriceDTO bestPrice = storePrices.stream()
                .filter(sp -> Boolean.TRUE.equals(sp.available()) && sp.currentPrice() != null)
                .min(Comparator.comparingDouble(StorePriceDTO::currentPrice))
                .orElse(null);

        String categoryName = product.getCategoryId() != null ? product.getCategoryId().getName() : null;

        return new ProductComparisonDTO(
                product.getProductId(),
                product.getName(),
                product.getBrand(),
                product.getEan(),
                product.getImageUrl(),
                product.getDescription(),
                categoryName,
                storePrices,
                bestPrice
        );
    }

    // ─── OPTIMIZACIÓN DE CESTA ─────────────────────────────

    public BasketOptimizationDTO optimizeBasket(List<Integer> productIds) {
        int totalProducts = productIds.size();

        // Accumulators per store
        Map<Integer, String> storeNames = new HashMap<>();
        Map<Integer, String> storeLogos = new HashMap<>();
        Map<Integer, List<BasketItemDTO>> storeItems = new HashMap<>();
        Map<Integer, Double> storeCosts = new HashMap<>();
        Map<Integer, Integer> storeAvailable = new HashMap<>();

        for (Integer productId : productIds) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) continue;

            List<ProductStore> productStores = productStoreRepository.findByProductId_ProductId(productId);

            for (ProductStore ps : productStores) {
                Integer storeId = ps.getStoreId().getStoreId();

                storeNames.putIfAbsent(storeId, ps.getStoreId().getName());
                storeLogos.putIfAbsent(storeId, ps.getStoreId().getLogo());
                storeItems.computeIfAbsent(storeId, k -> new ArrayList<>());

                boolean available = Boolean.TRUE.equals(ps.getAvailable());
                BasketItemDTO item = new BasketItemDTO(productId, product.getName(), ps.getCurrentPrice(), available);
                storeItems.get(storeId).add(item);

                if (available && ps.getCurrentPrice() != null) {
                    storeCosts.merge(storeId, ps.getCurrentPrice(), Double::sum);
                    storeAvailable.merge(storeId, 1, Integer::sum);
                }
            }
        }

        List<StoreBasketDTO> storeOptions = storeItems.entrySet().stream()
                .map(e -> {
                    Integer storeId = e.getKey();
                    return new StoreBasketDTO(
                            storeId,
                            storeNames.get(storeId),
                            storeLogos.get(storeId),
                            storeCosts.getOrDefault(storeId, 0.0),
                            storeAvailable.getOrDefault(storeId, 0),
                            totalProducts,
                            e.getValue()
                    );
                })
                .sorted((a, b) -> {
                    int availableCompare = Integer.compare(b.availableProducts(), a.availableProducts());
                    if (availableCompare != 0) return availableCompare;
                    return Double.compare(a.totalCost(), b.totalCost());
                })
                .toList();

        StoreBasketDTO cheapest = storeOptions.isEmpty() ? null : storeOptions.get(0);

        return new BasketOptimizationDTO(storeOptions, cheapest, totalProducts);
    }
}
