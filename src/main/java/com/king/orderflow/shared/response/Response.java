package com.king.orderflow.shared.response;

public class Response<T> {
    private boolean success;
    private String message;
    private T data;
    private int status;

    public static <T> Response<T> error(String message, int status) {
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
        response.status = 500;

        return response;
    }

    public static <T> Response<T> error(int status) {
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
        response.status = 500;

        return response;
    }

    public static <T> Response<T> success(String message, T data, int status) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = data;
        response.status = status;

        return response;
    }

    public static <T> Response<T> success(String message, T data) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = data;
        response.status = 200;

        return response;
    }

    public static <T> Response<T> success(String message) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.status = 200;

        return response;
    }

    public static <T> Response<T> success() {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = "Request successful";
        response.data = null;
        response.status = 200;

        return response;
    }

    public static <T> Response<T> success(String message, int status) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.status = status;

        return response;
    }

    public static <T> Response<T> success(T data, int status) {
        Response<T> response = new Response<T>();
        response.success = false;
        response.message = "Request successful";
        response.data = data;
        response.status = status;

        return response;
    }



}
