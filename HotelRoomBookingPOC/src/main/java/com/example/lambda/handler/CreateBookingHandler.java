package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.lambda.model.Booking;
import com.example.lambda.util.*;
import com.example.lambda.validation.BookingValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CreateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
//    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TABLE = "Bookings";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
//            Booking booking = mapper.readValue(request.getBody(), Booking.class);
        context.getLogger().log("Received request for create booking");
        Optional<Booking> optionalBooking = BookingParser.parseBooking(request.getBody());

        if(!optionalBooking.isPresent()) {
            context.getLogger().log("Invalid or missing booking data for the create booking request");
            return ResponseUtil.error(400, "Invalid or missing booking data");
        }
        Booking booking = optionalBooking.get();
        Optional<String> validationError = BookingValidator.validate(booking);
        if (validationError.isPresent()) {
            context.getLogger().log("Validation failed for the create booking request");
            return ResponseUtil.error(400, validationError.get());
        }
        String roomId = String.valueOf(AttributeValue.fromS(booking.getRoomId()));
        String userId = String.valueOf(AttributeValue.fromS(booking.getUserId()));
        String checkInDate = String.valueOf(AttributeValue.fromS(booking.getCheckInDate()));
        String checkOutDate = String.valueOf(AttributeValue.fromS(booking.getCheckOutDate()));
        LocalDate checkIn = LocalDate.parse(checkInDate);
        LocalDate checkOut = LocalDate.parse(checkOutDate);

        if(!isRoomAvailable(userId,roomId,checkIn,checkOut)){
            context.getLogger().log("Room is not available for booking");
            return ResponseUtil.error(400, "Room is not available");
        }

        booking.setBookingId(UUID.randomUUID().toString());
        DynamoDBClientUtil.getClient().putItem(PutItemRequest.builder()
                .tableName(TABLE)
                .item(Map.of(
                        "bookingId", AttributeValue.fromS(booking.getBookingId()),
                        "roomId", AttributeValue.fromS(booking.getRoomId()),
                        "userId", AttributeValue.fromS(booking.getUserId()),
                        "checkInDate", AttributeValue.fromS(booking.getCheckInDate()),
                        "checkOutDate", AttributeValue.fromS(booking.getCheckOutDate()),
                        "deleted", AttributeValue.fromBool(false)
                ))
                .build());
        context.getLogger().log("Booking has been created successfully");
        return ResponseUtil.success(Map.of("message", "Booking created", "bookingId", booking.getBookingId()));

    }

    public static boolean isRoomAvailable(String userId, String roomId, LocalDate newStart, LocalDate newEnd) {
        // Scan DynamoDB for bookings of this user and room that are not deleted
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE)
                .filterExpression("userId = :userId AND roomId = :roomId AND isDeleted = :isDeleted")
                .expressionAttributeValues(Map.of(
                        ":userId", AttributeValue.builder().s(userId).build(),
                        ":roomId", AttributeValue.builder().s(roomId).build(),
                        ":isDeleted", AttributeValue.builder().bool(false).build()
                ))
                .build();

        ScanResponse response = DynamoDBClientUtil.getClient().scan(scanRequest);
        List<Map<String, AttributeValue>> items = response.items();


        for (Map<String, AttributeValue> item : items) {
            LocalDate existingStart = LocalDate.parse(item.get("startDate").s());
            LocalDate existingEnd = LocalDate.parse(item.get("endDate").s());

            if (!newEnd.isBefore(existingStart) && !newStart.isAfter(existingEnd)) {
                return false;
            }
        }

        return true;
    }
}
