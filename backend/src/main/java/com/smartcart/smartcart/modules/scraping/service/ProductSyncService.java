package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;
import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.PriceHistoryRepository;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EntityManager entityManager;

    private static final int FLUSH_BATCH_SIZE = 500;

    @Transactional
    public SyncResult syncProducts(List<ScrapedProduct> scrapedProducts, String storeSlug) {
        Store store = storeRepository.findBySlug(storeSlug)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeSlug));

        SyncResult result = new SyncResult();

        // Pre-cargar caches en memoria para evitar queries repetidas
        Map<String, Category> categoryCache = loadCategoryCache();
        Map<String, ProductStore> psCache = loadProductStoreCache(store.getStoreId());
        Map<String, Product> productByNameCache = loadProductByNameCache();
        Map<String, Product> productByEanCache = loadProductByEanCache();

        log.info("[{}] Sync starting: {} products to process (caches loaded: {} categories, {} productStores, {} products)",
                storeSlug, scrapedProducts.size(), categoryCache.size(), psCache.size(), productByNameCache.size());

        int count = 0;
        for (ScrapedProduct scraped : scrapedProducts) {
            try {
                syncProductCached(scraped, store, result, categoryCache, psCache, productByNameCache, productByEanCache);
            } catch (Exception e) {
                log.error("Error syncing product {}: {}", scraped.externalId(), e.getMessage());
                result.errors++;
            }

            count++;
            if (count % FLUSH_BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
                // Re-attach store y recargar caches tras clear
                store = storeRepository.findBySlug(storeSlug)
                        .orElseThrow(() -> new RuntimeException("Store not found: " + storeSlug));
                categoryCache = loadCategoryCache();
                psCache = loadProductStoreCache(store.getStoreId());
                productByNameCache = loadProductByNameCache();
                productByEanCache = loadProductByEanCache();
                log.info("[{}] Progress: {}/{} products processed ({} created, {} updated, {} errors)",
                        storeSlug, count, scrapedProducts.size(), result.created, result.updated, result.errors);
            }
        }

        log.info("[{}] Sync completed: {} created, {} updated, {} unchanged, {} errors",
                storeSlug, result.created, result.updated, result.unchanged, result.errors);

        return result;
    }

    private Map<String, Category> loadCategoryCache() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c, (a, b) -> a));
    }

    private Map<String, ProductStore> loadProductStoreCache(Integer storeId) {
        return productStoreRepository.findAllByStoreWithProductAndCategory(storeId).stream()
                .filter(ps -> ps.getExternaId() != null && !ps.getExternaId().isBlank())
                .collect(Collectors.toMap(ProductStore::getExternaId, ps -> ps, (a, b) -> a));
    }

    private Map<String, Product> loadProductByNameCache() {
        return productRepository.findAll().stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(p -> p.getName().toLowerCase(), p -> p, (a, b) -> a));
    }

    private Map<String, Product> loadProductByEanCache() {
        return productRepository.findAll().stream()
                .filter(p -> p.getEan() != null && !p.getEan().isBlank())
                .collect(Collectors.toMap(Product::getEan, p -> p, (a, b) -> a));
    }

    private void syncProductCached(ScrapedProduct scraped, Store store, SyncResult result,
                                   Map<String, Category> categoryCache,
                                   Map<String, ProductStore> psCache,
                                   Map<String, Product> productByNameCache,
                                   Map<String, Product> productByEanCache) {
        if (scraped.price() == null) {
            result.errors++;
            return;
        }

        Category category = findOrCreateCategoryCached(scraped.categoryName(), categoryCache);
        Product product = findOrCreateProductCached(scraped, category, productByNameCache, productByEanCache);
        ProductStore productStore = findOrCreateProductStoreCached(scraped, product, store, psCache);

        boolean isNew = productStore.getStoreProductId() == null;
        boolean priceChanged = updateProductStorePrice(productStore, scraped);

        productStoreRepository.save(productStore);

        if (isNew) {
            createInitialPriceHistory(productStore, store, scraped);
            result.created++;
            // Añadir al cache para futuros lookups dentro del mismo batch
            if (scraped.externalId() != null && !scraped.externalId().isBlank()) {
                psCache.put(scraped.externalId(), productStore);
            }
        } else if (priceChanged) {
            createPriceHistory(productStore, store, scraped);
            result.updated++;
        } else {
            result.unchanged++;
        }
    }

    private Category findOrCreateCategoryCached(String categoryName, Map<String, Category> cache) {
        String finalCategoryName = (categoryName == null || categoryName.isBlank())
                ? "Sin categoría"
                : categoryName;

        String key = finalCategoryName.toLowerCase();
        Category cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        Category newCategory = new Category();
        newCategory.setName(finalCategoryName);
        log.debug("Creating new category: {}", finalCategoryName);
        newCategory = categoryRepository.save(newCategory);
        cache.put(key, newCategory);
        return newCategory;
    }

    private Product findOrCreateProductCached(ScrapedProduct scraped, Category category,
                                              Map<String, Product> byNameCache,
                                              Map<String, Product> byEanCache) {
        Product existing = null;

        if (scraped.ean() != null && !scraped.ean().isBlank()) {
            existing = byEanCache.get(scraped.ean());
        }

        if (existing == null && scraped.name() != null) {
            existing = byNameCache.get(scraped.name().toLowerCase());
        }

        if (existing != null) {
            updateProductIfNeeded(existing, scraped, category);
            return productRepository.save(existing);
        }

        Product newProduct = new Product();
        newProduct.setName(scraped.name());
        newProduct.setBrand(scraped.brand());
        newProduct.setEan(scraped.ean());
        newProduct.setImageUrl(scraped.imageUrl());
        newProduct.setCategoryId(category);
        parseAndSetUnit(newProduct, scraped.unit());

        String description = scraped.description();
        if (description == null || description.isBlank()) {
            description = generateDescription(scraped.name(), scraped.brand(), category.getName(), scraped.unit());
        }
        newProduct.setDescription(description);

        log.debug("Creating new product: {}", scraped.name());
        newProduct = productRepository.save(newProduct);

        // Añadir al cache
        if (scraped.name() != null) {
            byNameCache.put(scraped.name().toLowerCase(), newProduct);
        }
        if (scraped.ean() != null && !scraped.ean().isBlank()) {
            byEanCache.put(scraped.ean(), newProduct);
        }
        return newProduct;
    }

    private ProductStore findOrCreateProductStoreCached(ScrapedProduct scraped, Product product, Store store,
                                                       Map<String, ProductStore> psCache) {
        // Buscar por externalId en cache
        if (scraped.externalId() != null && !scraped.externalId().isBlank()) {
            ProductStore cached = psCache.get(scraped.externalId());
            if (cached != null) {
                return cached;
            }
        }

        // Fallback: buscar por productId + storeId en BD (no cacheado por externalId)
        Optional<ProductStore> existing = productStoreRepository
                .findByProductId_ProductIdAndStoreId_StoreId(product.getProductId(), store.getStoreId());

        if (existing.isPresent()) {
            ProductStore ps = existing.get();
            if (ps.getExternaId() == null) {
                ps.setExternaId(scraped.externalId());
            }
            return ps;
        }

        ProductStore newPs = new ProductStore();
        newPs.setProductId(product);
        newPs.setStoreId(store);
        newPs.setExternaId(scraped.externalId());
        newPs.setUrl(scraped.productUrl());
        newPs.setAvailable(true);

        return newPs;
    }

    private void updateProductIfNeeded(Product product, ScrapedProduct scraped, Category category) {
        if (product.getEan() == null && scraped.ean() != null) {
            product.setEan(scraped.ean());
        }
        if (product.getBrand() == null && scraped.brand() != null) {
            product.setBrand(scraped.brand());
        }
        if (product.getImageUrl() == null && scraped.imageUrl() != null) {
            product.setImageUrl(scraped.imageUrl());
        }
        if (product.getCategoryId() == null) {
            product.setCategoryId(category);
        }
        if (product.getUnit() == null && scraped.unit() != null) {
            parseAndSetUnit(product, scraped.unit());
        }
        if (product.getDescription() == null || product.getDescription().isBlank()) {
            String catName = product.getCategoryId() != null ? product.getCategoryId().getName() : category.getName();
            product.setDescription(generateDescription(product.getName(), product.getBrand(), catName, product.getUnit()));
        }
    }

    private void parseAndSetUnit(Product product, String unitString) {
        if (unitString == null || unitString.isBlank()) {
            return;
        }

        String[] parts = unitString.trim().split("\\s+", 2);
        if (parts.length == 2) {
            try {
                product.setQuantity(Double.parseDouble(parts[0]));
                product.setUnit(parts[1]);
            } catch (NumberFormatException e) {
                product.setUnit(unitString);
            }
        } else {
            product.setUnit(unitString);
        }
    }

    /**
     * Genera una descripcion automatica a partir de los datos disponibles.
     * Ej: "Leche entera de la marca Hacendado. Categoria: Lacteos. Formato: 1L."
     */
    private String generateDescription(String name, String brand, String categoryName, String unit) {
        StringBuilder sb = new StringBuilder();

        if (name != null && !name.isBlank()) {
            sb.append(name);
        }

        if (brand != null && !brand.isBlank()) {
            sb.append(" de la marca ").append(brand);
        }

        sb.append(".");

        if (categoryName != null && !categoryName.isBlank() && !"Sin categoria".equalsIgnoreCase(categoryName)) {
            sb.append(" Categoria: ").append(categoryName).append(".");
        }

        if (unit != null && !unit.isBlank()) {
            sb.append(" Formato: ").append(unit).append(".");
        }

        return sb.toString().trim();
    }

    private boolean updateProductStorePrice(ProductStore productStore, ScrapedProduct scraped) {
        Double newPrice = scraped.price() != null ? scraped.price().doubleValue() : null;
        Double currentPrice = productStore.getCurrentPrice();

        boolean priceChanged = currentPrice == null || !currentPrice.equals(newPrice);

        productStore.setCurrentPrice(newPrice);
        productStore.setAvailable(newPrice != null && newPrice > 0);

        if (scraped.productUrl() != null) {
            productStore.setUrl(scraped.productUrl());
        }

        return priceChanged && productStore.getStoreProductId() != null;
    }

    private void createInitialPriceHistory(ProductStore productStore, Store store, ScrapedProduct scraped) {
        PriceHistory history = new PriceHistory();
        history.setProductStoreId(productStore);
        history.setStoreId(store);
        history.setPrice(scraped.price() != null ? scraped.price().doubleValue() : null);
        history.setOriginalPrice(null);
        history.setIsOnSale(null);
        history.setRecordedAt(LocalDateTime.now());

        priceHistoryRepository.save(history);
    }

    private void createPriceHistory(ProductStore productStore, Store store, ScrapedProduct scraped) {
        PriceHistory history = new PriceHistory();
        history.setProductStoreId(productStore);
        history.setStoreId(store);
        history.setPrice(scraped.price() != null ? scraped.price().doubleValue() : null);
        history.setOriginalPrice(scraped.originalPrice() != null ? scraped.originalPrice().doubleValue() : null);
        history.setIsOnSale(scraped.onSale());
        history.setRecordedAt(LocalDateTime.now());

        priceHistoryRepository.save(history);
    }

    @Transactional
    public EnrichResult enrichProductsWithEan(List<ProductStore> productStores,
                                               java.util.Map<String, com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper.ProductDetail> details) {
        EnrichResult result = new EnrichResult();

        for (ProductStore ps : productStores) {
            try {
                String externalId = ps.getExternaId();
                if (externalId == null) {
                    continue;
                }

                var detail = details.get(externalId);
                if (detail == null) {
                    result.notFound++;
                    continue;
                }

                if (detail.ean() != null && !detail.ean().isBlank()) {
                    Product product = ps.getProductId();
                    product.setEan(detail.ean());
                    productRepository.save(product);
                    result.enriched++;
                    log.debug("EAN updated for product {}: {}", product.getName(), detail.ean());
                } else {
                    result.noEan++;
                }
            } catch (Exception e) {
                log.error("Error enriching product {}: {}", ps.getExternaId(), e.getMessage());
                result.errors++;
            }
        }

        log.info("Enrich completed: {} enriched, {} no EAN available, {} not found, {} errors",
                result.enriched, result.noEan, result.notFound, result.errors);

        return result;
    }

    @Transactional
    public EnrichResult enrichProductsWithEanMap(List<ProductStore> productStores,
                                                  java.util.Map<String, String> eanByExternalId) {
        EnrichResult result = new EnrichResult();

        for (ProductStore ps : productStores) {
            try {
                String externalId = ps.getExternaId();
                if (externalId == null) {
                    continue;
                }

                String ean = eanByExternalId.get(externalId);
                if (ean == null) {
                    result.notFound++;
                    continue;
                }

                if (!ean.isBlank()) {
                    Product product = ps.getProductId();
                    product.setEan(ean);
                    productRepository.save(product);
                    result.enriched++;
                    log.debug("EAN updated for product {}: {}", product.getName(), ean);
                } else {
                    result.noEan++;
                }
            } catch (Exception e) {
                log.error("Error enriching product {}: {}", ps.getExternaId(), e.getMessage());
                result.errors++;
            }
        }

        log.info("Enrich (generic) completed: {} enriched, {} no EAN available, {} not found, {} errors",
                result.enriched, result.noEan, result.notFound, result.errors);

        return result;
    }

    public List<ProductStore> findProductsWithoutEan(Integer storeId) {
        return productStoreRepository.findByStoreWithoutEan(storeId);
    }

    public static class SyncResult {
        public int created = 0;
        public int updated = 0;
        public int unchanged = 0;
        public int errors = 0;

        public int getTotal() {
            return created + updated + unchanged + errors;
        }
    }

    public static class EnrichResult {
        public int enriched = 0;
        public int noEan = 0;
        public int notFound = 0;
        public int errors = 0;

        public int getTotal() {
            return enriched + noEan + notFound + errors;
        }
    }
}
