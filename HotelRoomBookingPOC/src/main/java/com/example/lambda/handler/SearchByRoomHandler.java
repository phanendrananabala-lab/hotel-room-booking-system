package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.example.lambda.util.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class SearchByRoomHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Bookings";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received request for search booking by roomid");
        return Optional.ofNullable(request.getPathParameters())
                .map(params -> params.get("roomid"))
                .filter(id -> !id.trim().isEmpty())
                .map(this::findBookingsByRoomId)
                .orElseGet(() -> {
                    context.getLogger().log("Missing roomid in path");
                    return ResponseUtil.error(400, "Missing roomid in path");});
    }

    private APIGatewayProxyResponseEvent findBookingsByRoomId(String roomId) {

        Optional<ScanResponse> responseOpt = getRoomBookings(roomId);

        if (!responseOpt.isPresent()) {
            return ResponseUtil.error(500, "Error fetching bookings from database");
        }

        List<Map<String, ? extends Serializable>> bookings = responseOpt.get().items().stream()
                .map(this::convertItemToMap)
                .collect(Collectors.toList());

        if (bookings.isEmpty()) {
            return ResponseUtil.error(404, "No bookings found for the given roomid");
        }

        return ResponseUtil.success(bookings);
    }

    private Optional<ScanResponse> getRoomBookings(String roomId) {
        try {
            ScanResponse response = DynamoDBClientUtil.getClient().scan(
                    ScanRequest.builder()
                            .tableName(TABLE)
                            .filterExpression("roomId = :roomId and deleted = :false")
                            .expressionAttributeValues(Map.of(
                                    ":roomId", AttributeValue.fromS(roomId),
                                    ":false", AttributeValue.fromBool(false)
                            ))
                            .build()
            );
            return Optional.of(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Map<String, ? extends Serializable> convertItemToMap(Map<String, AttributeValue> item) {
        return item.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            AttributeValue val = entry.getValue();
                            if (val.s() != null) return val.s();
                            if (val.n() != null) return val.n();
                            if (val.bool() != null) return val.bool();
                            return null;
                        }
                ));
    }
}