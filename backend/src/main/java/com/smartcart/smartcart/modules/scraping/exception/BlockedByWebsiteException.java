package com.smartcart.smartcart.modules.scraping.exception;

/**
 * Excepción lanzada cuando el scraper es detectado y bloqueado por el sitio web.
 */
public class BlockedByWebsiteException extends ScrapingException {

    public BlockedByWebsiteException(String storeName, String url) {
        super(storeName, url, "Blocked by website - possible bot detection");
    }

    public BlockedByWebsiteException(String storeName, String url, String message) {
        super(storeName, url, message);
    }
}
