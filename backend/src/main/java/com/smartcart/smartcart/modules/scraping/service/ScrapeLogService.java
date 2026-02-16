package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.entity.ScrapeError;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeStatus;
import com.smartcart.smartcart.modules.scraping.repository.ScrapeErrorRepository;
import com.smartcart.smartcart.modules.scraping.repository.ScrapeLogRepository;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapeLogService {

    private final ScrapeLogRepository scrapeLogRepository;
    private final ScrapeErrorRepository scrapeErrorRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public ScrapeLog startLog(Store store) {
        ScrapeLog scrapeLog = new ScrapeLog();
        scrapeLog.setStore(store);
        scrapeLog.setStartTime(LocalDateTime.now());
        scrapeLog.setStatus(ScrapeStatus.RUNNING);
        scrapeLog.setErrorCount(0);
        return scrapeLogRepository.save(scrapeLog);
    }

    @Transactional
    public ScrapeLog completeLog(ScrapeLog scrapeLog, int productsFound,
                                  ProductSyncService.SyncResult syncResult) {
        scrapeLog.setEndTime(LocalDateTime.now());
        scrapeLog.setProductsFound(productsFound);
        scrapeLog.setProductsCreated(syncResult.created);
        scrapeLog.setProductsUpdated(syncResult.updated);
        scrapeLog.setProductsUnchanged(syncResult.unchanged);

        // Sumar errores de sync a los errores de scraping ya registrados
        int scrapingErrors = scrapeLog.getErrorCount() != null ? scrapeLog.getErrorCount() : 0;
        scrapeLog.setErrorCount(scrapingErrors + syncResult.errors);

        // FAILED si no se encontró ningún producto o si todo falló
        boolean nothingFound = productsFound == 0;
        boolean allFailed = syncResult.created + syncResult.updated == 0 && syncResult.errors > 0;
        scrapeLog.setStatus(nothingFound || allFailed ? ScrapeStatus.FAILED : ScrapeStatus.COMPLETED);

        if (nothingFound) {
            scrapeLog.setErrorMessage("Scraping finalizado sin encontrar productos");
        }

        scrapeLog.setDurationSeconds(
                Duration.between(scrapeLog.getStartTime(), scrapeLog.getEndTime()).getSeconds());
        return scrapeLogRepository.save(scrapeLog);
    }

    @Transactional
    public ScrapeLog cancelLog(ScrapeLog scrapeLog) {
        scrapeLog.setEndTime(LocalDateTime.now());
        scrapeLog.setStatus(ScrapeStatus.CANCELLED);
        scrapeLog.setErrorMessage("Cancelado por el administrador");
        scrapeLog.setDurationSeconds(
                Duration.between(scrapeLog.getStartTime(), scrapeLog.getEndTime()).getSeconds());
        return scrapeLogRepository.save(scrapeLog);
    }

    @Transactional
    public ScrapeLog failLog(ScrapeLog scrapeLog, String errorMessage) {
        scrapeLog.setEndTime(LocalDateTime.now());
        scrapeLog.setStatus(ScrapeStatus.FAILED);
        scrapeLog.setErrorMessage(
                errorMessage != null && errorMessage.length() > 2000
                        ? errorMessage.substring(0, 2000)
                        : errorMessage);
        scrapeLog.setDurationSeconds(
                Duration.between(scrapeLog.getStartTime(), scrapeLog.getEndTime()).getSeconds());
        return scrapeLogRepository.save(scrapeLog);
    }

    @Transactional
    public void addError(ScrapeLog scrapeLog, String errorType, String message, String url) {
        ScrapeError error = new ScrapeError();
        error.setScrapeLog(scrapeLog);
        error.setErrorType(errorType);
        error.setErrorMessage(
                message != null && message.length() > 2000
                        ? message.substring(0, 2000)
                        : message);
        error.setFailedUrl(url);
        error.setOccurredAt(LocalDateTime.now());
        scrapeErrorRepository.save(error);

        scrapeLog.setErrorCount(
                scrapeLog.getErrorCount() != null ? scrapeLog.getErrorCount() + 1 : 1);
        scrapeLogRepository.save(scrapeLog);
    }

    public void addErrorsFromScrapingResult(ScrapeLog scrapeLog, Map<String, String> errors) {
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            addError(scrapeLog, "SCRAPING", entry.getValue(), entry.getKey());
        }
    }

    public Page<ScrapeLog> getLogs(String storeSlug, String status, Pageable pageable) {
        Optional<Store> store = storeSlug != null
                ? storeRepository.findBySlug(storeSlug)
                : Optional.empty();
        ScrapeStatus scrapeStatus = status != null
                ? ScrapeStatus.valueOf(status.toUpperCase())
                : null;

        if (store.isPresent() && scrapeStatus != null) {
            return scrapeLogRepository.findByStoreAndStatusOrderByStartTimeDesc(
                    store.get(), scrapeStatus, pageable);
        } else if (store.isPresent()) {
            return scrapeLogRepository.findByStoreOrderByStartTimeDesc(store.get(), pageable);
        } else if (scrapeStatus != null) {
            return scrapeLogRepository.findByStatusOrderByStartTimeDesc(scrapeStatus, pageable);
        } else {
            return scrapeLogRepository.findAllByOrderByStartTimeDesc(pageable);
        }
    }

    public Optional<ScrapeLog> getLastLog(Store store) {
        return scrapeLogRepository.findFirstByStoreOrderByStartTimeDesc(store);
    }

    public List<ScrapeLog> getRecentLogs() {
        return scrapeLogRepository.findTop5ByOrderByStartTimeDesc();
    }

    public List<ScrapeError> getErrors(Long logId) {
        return scrapeErrorRepository.findByScrapeLog_IdOrderByOccurredAtDesc(logId);
    }

    public Optional<ScrapeLog> getLogById(Long id) {
        return scrapeLogRepository.findById(id);
    }
}
