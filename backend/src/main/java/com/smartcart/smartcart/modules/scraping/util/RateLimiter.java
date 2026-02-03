package com.smartcart.smartcart.modules.scraping.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Controla la velocidad de las peticiones HTTP para evitar saturar los servidores.
 */
@Component
public class RateLimiter {

    @Value("${smartcart.scraping.request-delay-ms:2000}")
    private long delayMs;

    private long lastRequestTime = 0;

    /**
     * Espera si es necesario para respetar el delay entre peticiones.
     */
    public synchronized void waitIfNeeded() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;

        if (elapsed < delayMs) {
            try {
                Thread.sleep(delayMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Permite ajustar el delay dinámicamente.
     */
    public void setDelay(long ms) {
        this.delayMs = ms;
    }

    public long getDelay() {
        return this.delayMs;
    }
}
