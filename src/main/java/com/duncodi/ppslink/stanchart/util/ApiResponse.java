package com.duncodi.ppslink.stanchart.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String code;
    private T data;

    @Builder.Default
    private String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now());

    public static <T> ApiResponse<T> success() {
        return success("Operation completed successfully");
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null, getCurrentTimestamp());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Successful", null, data, getCurrentTimestamp());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data, getCurrentTimestamp());
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, message, code, null, getCurrentTimestamp());
    }

    public static <T> ApiResponse<T> error(String message, String code, T errorDetails) {
        return new ApiResponse<>(false, message, code, errorDetails, getCurrentTimestamp());
    }

    public static <T> ApiResponseBuilder<T> successBuilder() {
        return ApiResponse.<T>builder().success(true).timestamp(getCurrentTimestamp());
    }

    public static <T> ApiResponseBuilder<T> errorBuilder() {
        return ApiResponse.<T>builder().success(false).timestamp(getCurrentTimestamp());
    }

    private static String getCurrentTimestamp() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

}
