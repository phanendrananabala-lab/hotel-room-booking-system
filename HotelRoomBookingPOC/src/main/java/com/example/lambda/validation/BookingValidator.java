package com.example.lambda.validation;

import com.example.lambda.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookingValidator {

    public static Optional<String> validate(Booking booking) {
        if (booking == null) return Optional.of("Booking payload is missing");
        if (booking.getRoomId() == null || booking.getRoomId().isEmpty())
            return Optional.of("Missing roomId");
        if (booking.getUserId() == null || booking.getUserId().isEmpty())
            return Optional.of("Missing userId");
        if (booking.getCheckInDate() == null || booking.getCheckInDate().isEmpty())
            return Optional.of("Missing checkInDate");
        if (booking.getCheckOutDate() == null || booking.getCheckOutDate().isEmpty())
            return Optional.of("Missing checkOutDate");
        return Optional.empty();
    }

}