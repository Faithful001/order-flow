package com.king.orderflow.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Response<T> {

    private static final String DEFAULT_SUCCESS = "Request successful";
    private static final String DEFAULT_ERROR = "Request unsuccessful";

    private final boolean success;
    private final String message;
    private final T data;

    private Response(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(true, DEFAULT_SUCCESS, data);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(true, message, data);
    }

    public static Response<Void> success() {
        return new Response<>(true, DEFAULT_SUCCESS, null);
    }

    public static Response<Void> message(String message) {
        return new Response<>(true, message, null);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(false, message, null);
    }

    public static <T> Response<T> error() {
        return new Response<>(false, DEFAULT_ERROR, null);
    }
}