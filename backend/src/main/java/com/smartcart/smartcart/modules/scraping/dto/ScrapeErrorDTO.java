package com.smartcart.smartcart.modules.scraping.dto;

import com.smartcart.smartcart.modules.scraping.entity.ScrapeError;

import java.time.LocalDateTime;

public record ScrapeErrorDTO(
    Long id,
    String errorType,
    String errorMessage,
    String failedUrl,
    LocalDateTime occurredAt
) {
    public static ScrapeErrorDTO fromEntity(ScrapeError entity) {
        return new ScrapeErrorDTO(
            entity.getId(),
            entity.getErrorType(),
            entity.getErrorMessage(),
            entity.getFailedUrl(),
            entity.getOccurredAt()
        );
    }
}
