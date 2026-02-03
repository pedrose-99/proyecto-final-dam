package com.smartcart.smartcart.modules.scraping.exception;

import lombok.Getter;

/**
 * Excepción base para errores de scraping.
 */
@Getter
public class ScrapingException extends RuntimeException {

    private final String storeName;
    private final String url;

    public ScrapingException(String storeName, String url, String message) {
        super(message);
        this.storeName = storeName;
        this.url = url;
    }

    public ScrapingException(String storeName, String url, String message, Throwable cause) {
        super(message, cause);
        this.storeName = storeName;
        this.url = url;
    }

    public ScrapingException(String message) {
        super(message);
        this.storeName = null;
        this.url = null;
    }
}
