package com.example.lambda.handler;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import com.example.lambda.util.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SearchByUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "Bookings";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received request for search booking by userid");
        return Optional.ofNullable(request.getPathParameters())
                .map(params -> params.get("userid"))
                .filter(id -> !id.trim().isEmpty())
                .map(this::findBookingsByUserId)
                .orElseGet(() -> {
                    context.getLogger().log("Missing userid in path");
                    return ResponseUtil.error(400, "Missing userid in path");
                });
    }

    private APIGatewayProxyResponseEvent findBookingsByUserId(String userId) {

        Optional<ScanResponse> responseOpt = getRoomBookings(userId);

        if (!responseOpt.isPresent()) {
            return ResponseUtil.error(500, "Error fetching bookings from database");
        }

        List<Map<String, ? extends Serializable>> bookings = responseOpt.get().items().stream()
                .map(this::convertItemToMap)
                .collect(Collectors.toList());

        if (bookings.isEmpty()) {
            return ResponseUtil.error(404, "No bookings found for the given userid");
        }

        return ResponseUtil.success(bookings);
    }

    private Optional<ScanResponse> getRoomBookings(String userId) {
        try {
            ScanResponse response = DynamoDBClientUtil.getClient().scan(
                    ScanRequest.builder()
                            .tableName(TABLE)
                            .filterExpression("userId = :userId and deleted = :false")
                            .expressionAttributeValues(Map.of(
                                    ":userId", AttributeValue.fromS(userId),
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