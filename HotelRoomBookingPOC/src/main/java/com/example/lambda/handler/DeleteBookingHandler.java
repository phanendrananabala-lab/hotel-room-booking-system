package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.example.lambda.util.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.*;

public class DeleteBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Bookings";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Delete booking request received");
        return Optional.ofNullable(request.getPathParameters())
                .map(params -> params.get("bookingid"))
                .filter(id -> !id.trim().isEmpty())
                .map(this::deleteBooking)
                .orElseGet(() ->{
                    context.getLogger().log("Missing bookingid");
                    return ResponseUtil.error(400, "Missing bookingid");}
                );
    }

    private APIGatewayProxyResponseEvent deleteBooking(String bookingId) {

        // First check if booking exists
        Optional<Map<String, AttributeValue>> existingBooking = FindBooking.findBookingById(bookingId);

        if (!existingBooking.isPresent()) {
            return ResponseUtil.error(404, "No booking found with the given bookingid");
        }

        // Perform soft delete
        return updateBooking(bookingId)
                .map(success -> ResponseUtil.success(Map.of("message", "Booking soft-deleted")))
                .orElseGet(() -> ResponseUtil.error(500, "Failed to delete booking"));
    }

    private Optional<Boolean> updateBooking(String bookingId) {
        try {
            DynamoDBClientUtil.getClient().updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("bookingId", AttributeValue.fromS(bookingId)))
                    .updateExpression("SET deleted = :true")
                    .expressionAttributeValues(Map.of(":true", AttributeValue.fromBool(true)))
                    .build());
            return Optional.of(true);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

