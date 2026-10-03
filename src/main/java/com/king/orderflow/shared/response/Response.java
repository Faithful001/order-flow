package com.king.orderflow.shared.response;

import org.springframework.http.HttpStatus;

public class Response<T> {
    private boolean success;
    private String message;
    private T data;
    private HttpStatus status;

    public static <T> Response<T> error(String message, HttpStatus status) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.status = status;

        return response;
    }

    public static <T> Response<T> error(String message) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.status = HttpStatus.INTERNAL_SERVER_ERROR;

        return response;
    }

    public static <T> Response<T> error(HttpStatus status) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = "Request unsuccessful";
        response.data = null;
        response.status = status;

        return response;
    }

    public static <T> Response<T> error() {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = "Request unsuccessful";
        response.data = null;
        response.status = HttpStatus.INTERNAL_SERVER_ERROR;

        return response;
    }

    public static <T> Response<T> success(String message, T data, HttpStatus status) {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.status = status;

        return response;
    }

    public static <T> Response<T> success(String message, T data) {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.status = HttpStatus.OK;

        return response;
    }

    public static <T> Response<T> success(String message) {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = message;
        response.data = null;
        response.status = HttpStatus.OK;

        return response;
    }

    public static <T> Response<T> success() {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = "Request successful";
        response.data = null;
        response.status = HttpStatus.OK;

        return response;
    }

    public static <T> Response<T> success(String message, HttpStatus status) {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = message;
        response.data = null;
        response.status = status;

        return response;
    }

    public static <T> Response<T> success(T data, HttpStatus status) {
        Response<T> response = new Response<T>();
        response.success = true;
        response.message = "Request successful";
        response.data = data;
        response.status = status;

        return response;
    }



}
