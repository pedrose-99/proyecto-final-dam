package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para enriquecer productos sin EAN usando Open Food Facts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEanEnricherService {

    private final ProductRepository productRepository;
    private final OpenFoodFactsService openFoodFactsService;

    /**
     * Enriquece productos de una tienda específica que no tienen EAN
     */
    @Transactional
    public EnrichmentResult enrichProductsForStore(Integer storeId) {
        log.info("Iniciando enriquecimiento de EAN para tienda {}", storeId);

        // Obtener productos sin EAN de la tienda
        List<Product> productsWithoutEan = productRepository.findByStoreId(storeId)
            .stream()
            .filter(p -> p.getEan() == null || p.getEan().isBlank())
            .toList();

        log.info("Encontrados {} productos sin EAN para enriquecer", productsWithoutEan.size());

        if (productsWithoutEan.isEmpty()) {
            return new EnrichmentResult(0, 0, 0);
        }

        AtomicInteger enriched = new AtomicInteger(0);
        AtomicInteger notFound = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);

        // Procesar en paralelo con límite para no saturar la API
        int parallelism = 3;
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);

        List<CompletableFuture<Void>> futures = productsWithoutEan.stream()
            .map(product -> CompletableFuture.runAsync(() -> {
                try {
                    // Rate limiting: esperar entre peticiones
                    Thread.sleep(500);

                    Optional<String> ean = openFoodFactsService.findEanByNameAndBrand(
                        product.getName(),
                        product.getBrand()
                    );

                    if (ean.isPresent()) {
                        product.setEan(ean.get());
                        productRepository.save(product);
                        enriched.incrementAndGet();
                    } else {
                        notFound.incrementAndGet();
                    }

                    int count = processed.incrementAndGet();
                    if (count % 50 == 0) {
                        log.info("Progreso: {}/{} productos procesados, {} enriquecidos",
                            count, productsWithoutEan.size(), enriched.get());
                    }

                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.warn("Error enriqueciendo producto {}: {}",
                        product.getProductId(), e.getMessage());
                }
            }, executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        log.info("Enriquecimiento completado: {} enriquecidos, {} no encontrados, {} errores",
            enriched.get(), notFound.get(), errors.get());

        return new EnrichmentResult(enriched.get(), notFound.get(), errors.get());
    }

    /**
     * Enriquece un producto específico
     */
    @Transactional
    public boolean enrichProduct(Integer productId) {
        Optional<Product> optProduct = productRepository.findById(productId);
        if (optProduct.isEmpty()) {
            return false;
        }

        Product product = optProduct.get();
        Optional<String> ean = openFoodFactsService.findEanByNameAndBrand(
            product.getName(),
            product.getBrand()
        );

        if (ean.isPresent()) {
            product.setEan(ean.get());
            productRepository.save(product);
            return true;
        }

        return false;
    }

    public record EnrichmentResult(int enriched, int notFound, int errors) {}
}
