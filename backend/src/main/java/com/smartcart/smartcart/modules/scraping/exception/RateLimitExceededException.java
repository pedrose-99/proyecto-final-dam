package com.smartcart.smartcart.modules.scraping.exception;

public class RateLimitExceededException extends ScrapingException
{

    public RateLimitExceededException(String storeName)
    {
        super(storeName, null, "Rate limit exceeded");
    }

    public RateLimitExceededException(String storeName, String message)
    {
        super(storeName, null, message);
    }
}
