package com.smartcart.smartcart.modules.scraping.entity;

import com.smartcart.smartcart.modules.store.entity.Store;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_log")
@Data
public class ScrapeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "products_found")
    private Integer productsFound;

    @Column(name = "products_created")
    private Integer productsCreated;

    @Column(name = "products_updated")
    private Integer productsUpdated;

    @Column(name = "products_unchanged")
    private Integer productsUnchanged;

    @Column(name = "error_count")
    private Integer errorCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScrapeStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    public ScrapeLog() {}
}
