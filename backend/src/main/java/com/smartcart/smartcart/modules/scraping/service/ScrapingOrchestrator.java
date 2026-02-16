package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import com.smartcart.smartcart.modules.scraping.scraper.MercadonaScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingOrchestrator
{

    private final MercadonaScrapingService mercadonaService;
    private final AlcampoScrapingService alcampoService;
    private final PythonScraperService pythonScraperService;
    private final ProductSyncService productSyncService;

    // TODO: Descomentar para activar scraping automático al arrancar
    // @Async
    // @EventListener(ApplicationReadyEvent.class)
    public void onStartup()
    {
        log.info("========== INICIO SCRAPING AUTOMATICO ==========");
        long startTime = System.currentTimeMillis();

        scrapeAndEnrichMercadona();
        scrapeAndEnrichAlcampo();
        scrapeAndSyncDia();
        scrapeAndSyncCarrefour();

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
        }
        catch (Exception e)
        {
            log.error("[alcampo] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- ALCAMPO: Completado ----------");
    }

    private void scrapeAndSyncDia()
    {
        log.info("---------- DIA: Iniciando scraping ----------");
        try
        {
            if (!pythonScraperService.isHealthy())
            {
                log.warn("[dia] Scraper Python no disponible, omitiendo Dia");
                return;
            }

            ScrapingResult scrapingResult = pythonScraperService.scrapeDia();
            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "dia");

            log.info("[dia] Scraping: {} productos, {} errores en {}s",
                     scrapingResult.getTotalProducts(), scrapingResult.getTotalErrors(),
                     scrapingResult.getDurationSeconds());
            log.info("[dia] Sync: {} creados, {} actualizados, {} sin cambios, {} errores",
                     syncResult.created, syncResult.updated, syncResult.unchanged, syncResult.errors);
        }
        catch (Exception e)
        {
            log.error("[dia] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- DIA: Completado ----------");
    }

    private void scrapeAndSyncCarrefour()
    {
        log.info("---------- CARREFOUR: Iniciando scraping ----------");
        try
        {
            if (!pythonScraperService.isHealthy())
            {
                log.warn("[carrefour] Scraper Python no disponible, omitiendo Carrefour");
                return;
            }

            ScrapingResult scrapingResult = pythonScraperService.scrapeCarrefour();
            ProductSyncService.SyncResult syncResult = productSyncService.syncProducts(
                scrapingResult.getProducts(), "carrefour");

            log.info("[carrefour] Scraping: {} productos, {} errores en {}s",
                     scrapingResult.getTotalProducts(), scrapingResult.getTotalErrors(),
                     scrapingResult.getDurationSeconds());
            log.info("[carrefour] Sync: {} creados, {} actualizados, {} sin cambios, {} errores",
                     syncResult.created, syncResult.updated, syncResult.unchanged, syncResult.errors);
        }
        catch (Exception e)
        {
            log.error("[carrefour] Error en scraping: {}", e.getMessage(), e);
        }
        log.info("---------- CARREFOUR: Completado ----------");
    }

}
