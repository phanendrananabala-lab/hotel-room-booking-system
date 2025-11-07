package com.example.lambda.util;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;

public class DynamoDBClientUtil {
    private static final DynamoDbClient client = DynamoDbClient.builder()
            .region(Region.AP_SOUTH_1) // change region if needed
            .build();

    public static DynamoDbClient getClient() {
        return client;
    }
}