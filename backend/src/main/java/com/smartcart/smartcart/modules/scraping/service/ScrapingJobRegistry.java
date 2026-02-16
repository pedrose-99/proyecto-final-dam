package com.smartcart.smartcart.modules.scraping.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScrapingJobRegistry {

    private final ConcurrentHashMap<String, Thread> activeJobs = new ConcurrentHashMap<>();

    public void register(String storeSlug) {
        activeJobs.put(storeSlug, Thread.currentThread());
    }

    public void deregister(String storeSlug) {
        activeJobs.remove(storeSlug);
    }

    public boolean cancel(String storeSlug) {
        Thread thread = activeJobs.remove(storeSlug);
        if (thread != null) {
            thread.interrupt();
            return true;
        }
        return false;
    }

    public boolean isRunning(String storeSlug) {
        return activeJobs.containsKey(storeSlug);
    }
}
