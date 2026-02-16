package com.smartcart.smartcart.modules.scraping.repository;

import com.smartcart.smartcart.modules.scraping.entity.ScrapeLog;
import com.smartcart.smartcart.modules.scraping.entity.ScrapeStatus;
import com.smartcart.smartcart.modules.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapeLogRepository extends JpaRepository<ScrapeLog, Long> {

    Page<ScrapeLog> findByStoreOrderByStartTimeDesc(Store store, Pageable pageable);

    Page<ScrapeLog> findAllByOrderByStartTimeDesc(Pageable pageable);

    Page<ScrapeLog> findByStatusOrderByStartTimeDesc(ScrapeStatus status, Pageable pageable);

    Page<ScrapeLog> findByStoreAndStatusOrderByStartTimeDesc(Store store, ScrapeStatus status, Pageable pageable);

    Optional<ScrapeLog> findFirstByStoreOrderByStartTimeDesc(Store store);

    List<ScrapeLog> findByStatus(ScrapeStatus status);

    List<ScrapeLog> findTop5ByOrderByStartTimeDesc();
}
