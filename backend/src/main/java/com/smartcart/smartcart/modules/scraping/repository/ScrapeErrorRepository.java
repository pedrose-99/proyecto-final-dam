package com.smartcart.smartcart.modules.scraping.repository;

import com.smartcart.smartcart.modules.scraping.entity.ScrapeError;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapeErrorRepository extends JpaRepository<ScrapeError, Long> {

    List<ScrapeError> findByScrapeLogOrderByOccurredAtDesc(ScrapeLog scrapeLog);

    List<ScrapeError> findByScrapeLog_IdOrderByOccurredAtDesc(Long scrapeLogId);

    long countByScrapeLog(ScrapeLog scrapeLog);
}
