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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional
    public SyncResult syncProducts(List<ScrapedProduct> scrapedProducts, String storeSlug) {
        Store store = storeRepository.findBySlug(storeSlug)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeSlug));

        SyncResult result = new SyncResult();

        for (ScrapedProduct scraped : scrapedProducts) {
            try {
                syncProduct(scraped, store, result);
            } catch (Exception e) {
                log.error("Error syncing product {}: {}", scraped.externalId(), e.getMessage());
                result.errors++;
            }
        }

        log.info("[{}] Sync completed: {} created, {} updated, {} unchanged, {} errors",
                storeSlug, result.created, result.updated, result.unchanged, result.errors);

        return result;
    }

    private void syncProduct(ScrapedProduct scraped, Store store, SyncResult result) {
        if (scraped.price() == null) {
            result.errors++;
            return;
        }

        Category category = findOrCreateCategory(scraped.categoryName());
        Product product = findOrCreateProduct(scraped, category);
        ProductStore productStore = findOrCreateProductStore(scraped, product, store);

        boolean priceChanged = updateProductStorePrice(productStore, scraped);

        if (priceChanged) {
            createPriceHistory(productStore, store, scraped);
            result.updated++;
        } else if (productStore.getStoreProductId() == null) {
            result.created++;
        } else {
            result.unchanged++;
        }

        productStoreRepository.save(productStore);
    }

    private Category findOrCreateCategory(String categoryName) {
        String finalCategoryName = (categoryName == null || categoryName.isBlank())
                ? "Sin categoría"
                : categoryName;

        return categoryRepository.findByName(finalCategoryName)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(finalCategoryName);
                    log.debug("Creating new category: {}", finalCategoryName);
                    return categoryRepository.save(newCategory);
                });
    }

    private Product findOrCreateProduct(ScrapedProduct scraped, Category category) {
        Optional<Product> existing = Optional.empty();

        if (scraped.ean() != null && !scraped.ean().isBlank()) {
            existing = productRepository.findByEan(scraped.ean());
        }

        if (existing.isEmpty() && scraped.name() != null) {
            existing = productRepository.findByNameIgnoreCase(scraped.name());
        }

        if (existing.isPresent()) {
            Product product = existing.get();
            updateProductIfNeeded(product, scraped, category);
            return productRepository.save(product);
        }

        Product newProduct = new Product();
        newProduct.setName(scraped.name());
        newProduct.setBrand(scraped.brand());
        newProduct.setEan(scraped.ean());
        newProduct.setDescription(scraped.description());
        newProduct.setImageUrl(scraped.imageUrl());
        newProduct.setCategoryId(category);
        parseAndSetUnit(newProduct, scraped.unit());

        log.debug("Creating new product: {}", scraped.name());
        return productRepository.save(newProduct);
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

    private ProductStore findOrCreateProductStore(ScrapedProduct scraped, Product product, Store store) {
        Optional<ProductStore> existing = productStoreRepository
                .findByExternaIdAndStoreId_StoreId(scraped.externalId(), store.getStoreId());

        if (existing.isPresent()) {
            return existing.get();
        }

        existing = productStoreRepository
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
