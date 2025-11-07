package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.example.lambda.model.Booking;
import com.example.lambda.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

public class UpdateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Bookings";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received request for update booking request");
        return Optional.ofNullable(request.getPathParameters())
                .map(params -> params.get("bookingid"))
                .filter(id -> !id.trim().isEmpty())
                .map(bookingId -> updateBooking(request, bookingId))
                .orElseGet(() -> {
                    context.getLogger().log("Missing bookingid in path");
                    return ResponseUtil.error(400, "Missing bookingid");
                });
    }

    private APIGatewayProxyResponseEvent updateBooking(APIGatewayProxyRequestEvent request, String bookingId) {

        Optional<Booking> optionalBooking = parseBooking(request.getBody());

        if (!optionalBooking.isPresent()) {
            return ResponseUtil.error(400, "Invalid or missing booking data");
        }

        Booking updated = optionalBooking.get();

        // Check if booking exists before updating
        Optional<Map<String, AttributeValue>> existingBooking = FindBooking.findBookingById(bookingId);

        if (!existingBooking.isPresent()) {
            return ResponseUtil.error(404, "No booking found with the given bookingid");
        }

        // Perform the update
        return performUpdate(bookingId, updated)
                .map(success -> ResponseUtil.success(Map.of("message", "Booking updated successfully")))
                .orElseGet(() -> ResponseUtil.error(500, "Failed to update booking"));
    }

    private Optional<Booking> parseBooking(String body) {
        return Optional.ofNullable(body)
                .filter(b -> !b.trim().isEmpty())
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, Booking.class));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
    }

    private Optional<Boolean> performUpdate(String bookingId, Booking updated) {
        try {
            DynamoDBClientUtil.getClient().updateItem(
                    UpdateItemRequest.builder()
                            .tableName(TABLE)
                            .key(Map.of("bookingId", AttributeValue.fromS(bookingId)))
                            .updateExpression("SET checkInDate = :in, checkOutDate = :out")
                            .expressionAttributeValues(Map.of(
                                    ":in", AttributeValue.fromS(updated.getCheckInDate()),
                                    ":out", AttributeValue.fromS(updated.getCheckOutDate())
                            ))
                            .build()
            );
            return Optional.of(true);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}