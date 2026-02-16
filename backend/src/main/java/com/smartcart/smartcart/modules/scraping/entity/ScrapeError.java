package com.smartcart.smartcart.modules.scraping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_error")
@Data
public class ScrapeError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrape_log_id", nullable = false)
    private ScrapeLog scrapeLog;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "failed_url", length = 1000)
    private String failedUrl;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public ScrapeError() {}
}
