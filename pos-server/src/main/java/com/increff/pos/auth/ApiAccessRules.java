package com.increff.pos.auth;

import org.springframework.http.HttpMethod;
import java.util.List;

public class ApiAccessRules {
    public record Rule(HttpMethod method, String pattern) {}

    public static final List<Rule> OPERATOR_ALLOWED = List.of(
            new Rule(HttpMethod.POST, "/api/client/get-all-paginated"),
            new Rule(HttpMethod.POST, "/api/client/filter"),
            new Rule(HttpMethod.GET,  "/api/client/get-by-email/**"),

            new Rule(HttpMethod.POST, "/api/product/get-all-paginated"),
            new Rule(HttpMethod.POST, "/api/product/filter"),
            new Rule(HttpMethod.GET,  "/api/product/get-by-barcode/**"),

            new Rule(HttpMethod.POST, "/api/order/get-all-paginated"),
            new Rule(HttpMethod.POST, "/api/order/filter"),
            new Rule(HttpMethod.GET,  "/api/order/get/**"),
            new Rule(HttpMethod.POST, "/api/order/create"),
            new Rule(HttpMethod.PUT,  "/api/order/edit/**")
    );

    private ApiAccessRules() {}
}
