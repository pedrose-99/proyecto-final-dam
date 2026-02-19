package com.smartcart.smartcart.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.smartcart.smartcart.modules.product.entity.PriceHistory;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.PriceHistoryRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MockPriceHistoryGenerator
{
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductStoreRepository productStoreRepository;

    private static final int ENTRIES_PER_PRODUCT = 12;
    private static final int BATCH_SIZE = 1000;

    public void generateIfNeeded()
    {
        long productStoreCount = productStoreRepository.count();

        if (productStoreCount == 0)
        {
            log.info("No hay ProductStore, saltando generacion de historico ficticio");
            return;
        }

        // Comprobar si ya existen registros con fechas anteriores a hoy (mock data)
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long mockCount = priceHistoryRepository.countByRecordedAtBefore(todayStart);

        if (mockCount > 0)
        {
            log.info("Ya existen {} registros de historico ficticio (anteriores a hoy), saltando generacion",
                    mockCount);
            return;
        }

        log.info("Generando historico ficticio de precios para {} product_store...",
                productStoreCount);

        List<ProductStore> allProductStores = productStoreRepository.findAll();
        List<PriceHistory> batch = new ArrayList<>(BATCH_SIZE);
        int totalGenerated = 0;

        LocalDateTime now = LocalDateTime.now();

        for (ProductStore ps : allProductStores)
        {
            if (ps.getCurrentPrice() == null)
            {
                continue;
            }

            double basePrice = ps.getCurrentPrice();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int i = 0; i < ENTRIES_PER_PRODUCT; i++)
            {
                // Fechas escalonadas: desde hace ~6 meses hasta hace ~1 semana
                int daysAgo = 7 + (i * 14);
                LocalDateTime recordedAt = now.minusDays(daysAgo);

                // Variacion aleatoria ±10% sobre el precio base
                double variation = 1.0 + (random.nextDouble(-0.10, 0.10));
                double price = Math.round(basePrice * variation * 100.0) / 100.0;

                boolean isOnSale = random.nextDouble() < 0.20;
                Double originalPrice = null;

                if (isOnSale)
                {
                    originalPrice = price;
                    // Descuento del 10-25%
                    double discount = 1.0 - random.nextDouble(0.10, 0.25);
                    price = Math.round(price * discount * 100.0) / 100.0;
                }

                PriceHistory ph = new PriceHistory();
                ph.setProductStoreId(ps);
                ph.setStoreId(ps.getStoreId());
                ph.setPrice(price);
                ph.setOriginalPrice(originalPrice);
                ph.setIsOnSale(isOnSale);
                ph.setRecordedAt(recordedAt);

                batch.add(ph);

                if (batch.size() >= BATCH_SIZE)
                {
                    priceHistoryRepository.saveAll(batch);
                    totalGenerated += batch.size();
                    batch.clear();
                    log.info("Historico ficticio: {} registros generados...", totalGenerated);
                }
            }
        }

        // Guardar registros restantes
        if (!batch.isEmpty())
        {
            priceHistoryRepository.saveAll(batch);
            totalGenerated += batch.size();
        }

        log.info("Generados {} registros de historico ficticio de precios", totalGenerated);
    }
}
