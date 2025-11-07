package com.example.lambda.util;

import com.example.lambda.model.Booking;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

public class BookingParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Optional<Booking> parseBooking(String requestBody) {
        return Optional.ofNullable(requestBody)                  // Handle null request body
                .filter(body -> !body.trim().isEmpty())    // Ignore empty strings
                .map(body -> {
                    try {
                        return mapper.readValue(body, Booking.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Invalid JSON format: " + e.getMessage(), e);
                    }
                });
    }
}
