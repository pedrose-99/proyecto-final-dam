package com.smartcart.smartcart.modules.scraping.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class UserAgentRotator
{

    @Value("${smartcart.scraping.user-agents:}")
    private List<String> userAgents;

    private final Random random = new Random();

    private static final String DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public String getNext()
    {
        if (userAgents == null || userAgents.isEmpty())
        {
            return DEFAULT_USER_AGENT;
        }
        return userAgents.get(random.nextInt(userAgents.size()));
    }

    public String getDefault()
    {
        return DEFAULT_USER_AGENT;
    }
}
