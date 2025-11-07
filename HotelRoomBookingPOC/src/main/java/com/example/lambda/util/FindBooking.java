package com.example.lambda.util;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;
import java.util.Optional;

public class FindBooking {

    private static final String TABLE = "Bookings";

    public static Optional<Map<String, AttributeValue>> findBookingById(String bookingId) {
        try {
            GetItemResponse response = DynamoDBClientUtil.getClient().getItem(
                    GetItemRequest.builder()
                            .tableName(TABLE)
                            .key(Map.of("bookingId", AttributeValue.fromS(bookingId)))
                            .build()
            );
            return Optional.ofNullable(response.item())
                    .filter(item -> !item.isEmpty());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
