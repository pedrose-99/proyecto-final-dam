package com.smartcart.smartcart.graphql.resolver;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class QueryResolver {

    @QueryMapping
    public String ping() {
        return "pong";
    }
}
