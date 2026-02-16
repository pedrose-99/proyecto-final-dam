package com.smartcart.smartcart.modules.admin.dto;

public record ServiceHealthDTO(
    String backend,
    String database,
    String pythonScraper
) {}
