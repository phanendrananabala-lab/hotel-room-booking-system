package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.lambda.model.Booking;
import com.example.lambda.util.BookingParser;
import com.example.lambda.util.DynamoDBClientUtil;
import com.example.lambda.util.ResponseUtil;
import com.example.lambda.validation.BookingValidator;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CreateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Bookings";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received request for create booking\n");
        context.getLogger().log("Request body: " + request.getBody() + "\n");

        Optional<Booking> optionalBooking = BookingParser.parseBooking(request.getBody());

        if (!optionalBooking.isPresent()) {
            context.getLogger().log("Invalid or missing booking data\n");
            return ResponseUtil.error(400, "Invalid or missing booking data");
        }

        Booking booking = optionalBooking.get();
        Optional<String> validationError = BookingValidator.validate(booking);

        if (validationError.isPresent()) {
            context.getLogger().log("Validation failed: " + validationError.get() + "\n");
            return ResponseUtil.error(400, validationError.get());
        }

        String roomId = booking.getRoomId();
        String userId = booking.getUserId();
        LocalDate checkIn = LocalDate.parse(booking.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(booking.getCheckOutDate());

        context.getLogger().log("Parsed booking details:\n");
        context.getLogger().log("userId: " + userId + "\n");
        context.getLogger().log("roomId: " + roomId + "\n");
        context.getLogger().log("checkIn: " + checkIn + "\n");
        context.getLogger().log("checkOut: " + checkOut + "\n");

        if (isBookingConflict(userId, roomId, checkIn, checkOut, context)) {
            context.getLogger().log("Room is already booked for the specified period\n");
            return ResponseUtil.error(400, "Room is already booked for the specified period by this user.");
        }

        booking.setBookingId(UUID.randomUUID().toString());

        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(TABLE)
                .item(Map.of(
                        "bookingId", AttributeValue.fromS(booking.getBookingId()),
                        "roomId", AttributeValue.fromS(roomId),
                        "userId", AttributeValue.fromS(userId),
                        "checkInDate", AttributeValue.fromS(booking.getCheckInDate()),
                        "checkOutDate", AttributeValue.fromS(booking.getCheckOutDate()),
                        "deleted", AttributeValue.fromBool(false)
                ))
                .build();

        DynamoDBClientUtil.getClient().putItem(putRequest);
        context.getLogger().log("Booking created successfully with ID: " + booking.getBookingId() + "\n");

        return ResponseUtil.success(Map.of("message", "Booking created", "bookingId", booking.getBookingId()));
    }

    public static boolean isBookingConflict(String userId, String roomId, LocalDate newCheckIn, LocalDate newCheckOut, Context context) {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE)
                .filterExpression("userId = :userId AND roomId = :roomId AND deleted = :deleted")
                .expressionAttributeValues(Map.of(
                        ":userId", AttributeValue.builder().s(userId).build(),
                        ":roomId", AttributeValue.builder().s(roomId).build(),
                        ":deleted", AttributeValue.builder().bool(false).build()
                ))
                .build();

        context.getLogger().log("Executing scan for conflict check...\n");

        ScanResponse response = DynamoDBClientUtil.getClient().scan(scanRequest);
        List<Map<String, AttributeValue>> items = response.items();

        for (Map<String, AttributeValue> item : items) {
            LocalDate existingCheckIn = LocalDate.parse(item.get("checkInDate").s());
            LocalDate existingCheckOut = LocalDate.parse(item.get("checkOutDate").s());

            boolean isOverlapping =
                    (newCheckIn.isBefore(existingCheckOut) && newCheckOut.isAfter(existingCheckIn)) ||
                            newCheckIn.isEqual(existingCheckIn) || newCheckOut.isEqual(existingCheckOut);

            context.getLogger().log("Checking overlap with existing booking: " + existingCheckIn + " to " + existingCheckOut + "\n");
            context.getLogger().log("Is overlapping: " + isOverlapping + "\n");

            if (isOverlapping) {
                return true;
            }
        }

        return false;
    }
}