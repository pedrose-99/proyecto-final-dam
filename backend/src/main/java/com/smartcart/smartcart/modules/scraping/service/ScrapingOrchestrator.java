package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.AlcampoScraper;
import com.smartcart.smartcart.modules.scraping.scraper.CarrefourScraper;
import com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingOrchestrator
{

    private final MercadonaScrapingService mercadonaService;
    private final AlcampoScrapingService alcampoService;
    private final CarrefourScrapingService carrefourService;
    private final ProductSyncService productSyncService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup()
    {
        log.info("========== INICIO SCRAPING AUTOMATICO ==========");
        long startTime = System.currentTimeMillis();

        scrapeAndEnrichMercadona();
        scrapeAndEnrichAlcampo();
        scrapeAndEnrichCarrefour();

        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        log.info("========== SCRAPING AUTOMATICO COMPLETADO en {}s ==========", totalSeconds);
    }

    private void scrapeAndEnrichMercadona()
    {
        log.info("---------- MERCADONA: Iniciando scraping ----------");
        try
        {
            ScrapingResult scrapingResult = mercadonaService.scrapeAll();
            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "mercadona");

            log.info("[mercadona] Scraping: {} productos, {} errores en {}s",
                     scrapingResult.getTotalProducts(), scrapingResult.getTotalErrors(),
                     scrapingResult.getDurationSeconds());
            log.info("[mercadona] Sync: {} creados, {} actualizados, {} sin cambios, {} errores",
                     syncResult.created, syncResult.updated, syncResult.unchanged, syncResult.errors);

            // Enriquecer EAN via API de Mercadona
            List<ProductStore> productsWithoutEan = productSyncService.findProductsWithoutEan(1);
            if (!productsWithoutEan.isEmpty())
            {
                log.info("[mercadona] Enriqueciendo {} productos sin EAN...", productsWithoutEan.size());

                List<String> externalIds = productsWithoutEan.stream()
                    .map(ProductStore::getExternaId)
                    .filter(id -> id != null)
                    .toList();

                Map<String, MercadonaScraper.ProductDetail> details =
                    mercadonaService.getProductDetails(externalIds);

                ProductSyncService.EnrichResult enrichResult =
                    productSyncService.enrichProductsWithEan(productsWithoutEan, details);

                log.info("[mercadona] Enrich: {} enriquecidos, {} sin EAN, {} no encontrados, {} errores",
                         enrichResult.enriched, enrichResult.noEan, enrichResult.notFound, enrichResult.errors);
            }
            else
            {
                log.info("[mercadona] Todos los productos ya tienen EAN");
            }
        }
        catch (Exception e)
        {
            log.error("[mercadona] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- MERCADONA: Completado ----------");
    }

    private void scrapeAndEnrichAlcampo()
    {
        log.info("---------- ALCAMPO: Iniciando scraping ----------");
        try
        {
            ScrapingResult scrapingResult = alcampoService.scrapeAll();
            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "alcampo");

            log.info("[alcampo] Scraping: {} productos, {} errores en {}s",
                     scrapingResult.getTotalProducts(), scrapingResult.getTotalErrors(),
                     scrapingResult.getDurationSeconds());
            log.info("[alcampo] Sync: {} creados, {} actualizados, {} sin cambios, {} errores",
                     syncResult.created, syncResult.updated, syncResult.unchanged, syncResult.errors);

            // Enriquecer EAN desde paginas de detalle de Alcampo
            List<ProductStore> productsWithoutEan = productSyncService.findProductsWithoutEan(4);
            if (!productsWithoutEan.isEmpty())
            {
                log.info("[alcampo] Enriqueciendo {} productos sin EAN...", productsWithoutEan.size());

                Map<String, AlcampoScraper.ProductDetail> details =
                    alcampoService.getProductDetails(productsWithoutEan);

                Map<String, String> eanMap = details.entrySet().stream()
                    .filter(e -> e.getValue().ean() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().ean()));

                ProductSyncService.EnrichResult enrichResult =
                    productSyncService.enrichProductsWithEanMap(productsWithoutEan, eanMap);

                log.info("[alcampo] Enrich: {} enriquecidos, {} sin EAN, {} no encontrados, {} errores",
                         enrichResult.enriched, enrichResult.noEan, enrichResult.notFound, enrichResult.errors);
            }
            else
            {
                log.info("[alcampo] Todos los productos ya tienen EAN");
            }
        }
        catch (Exception e)
        {
            log.error("[alcampo] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- ALCAMPO: Completado ----------");
    }

    private void scrapeAndEnrichCarrefour()
    {
        log.info("---------- CARREFOUR: Iniciando scraping ----------");
        try
        {
            ScrapingResult scrapingResult = carrefourService.scrapeAll();
            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "carrefour");

            log.info("[carrefour] Scraping: {} productos, {} errores en {}s",
                     scrapingResult.getTotalProducts(), scrapingResult.getTotalErrors(),
                     scrapingResult.getDurationSeconds());
            log.info("[carrefour] Sync: {} creados, {} actualizados, {} sin cambios, {} errores",
                     syncResult.created, syncResult.updated, syncResult.unchanged, syncResult.errors);

            // Enriquecer EAN desde paginas de detalle de Carrefour
            List<ProductStore> productsWithoutEan = productSyncService.findProductsWithoutEan(3);
            if (!productsWithoutEan.isEmpty())
            {
                log.info("[carrefour] Enriqueciendo {} productos sin EAN...", productsWithoutEan.size());

                Map<String, CarrefourScraper.ProductDetail> details =
                    carrefourService.getProductDetails(productsWithoutEan);

                Map<String, String> eanMap = details.entrySet().stream()
                    .filter(e -> e.getValue().ean() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().ean()));

                ProductSyncService.EnrichResult enrichResult =
                    productSyncService.enrichProductsWithEanMap(productsWithoutEan, eanMap);

                log.info("[carrefour] Enrich: {} enriquecidos, {} sin EAN, {} no encontrados, {} errores",
                         enrichResult.enriched, enrichResult.noEan, enrichResult.notFound, enrichResult.errors);
            }
            else
            {
                log.info("[carrefour] Todos los productos ya tienen EAN");
            }
        }
        catch (Exception e)
        {
            log.error("[carrefour] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- CARREFOUR: Completado ----------");
    }
}
