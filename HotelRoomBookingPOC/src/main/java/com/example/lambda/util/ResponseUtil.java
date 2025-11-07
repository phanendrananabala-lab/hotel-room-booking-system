package com.example.lambda.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ResponseUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static APIGatewayProxyResponseEvent success(Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(mapper.writeValueAsString(body));
        } catch (Exception e) {
            return error(500, "Failed to serialize response");
        }
    }

    public static APIGatewayProxyResponseEvent error(int code, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody("{\"error\":\"" + message + "\"}");
    }
}