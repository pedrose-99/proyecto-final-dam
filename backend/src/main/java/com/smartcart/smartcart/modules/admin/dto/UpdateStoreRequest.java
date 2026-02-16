package com.smartcart.smartcart.modules.admin.dto;

public record UpdateStoreRequest(
    Boolean active,
    String scrapingUrl
) {}
