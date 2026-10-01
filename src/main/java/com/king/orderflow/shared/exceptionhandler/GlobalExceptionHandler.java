package com.king.orderflow.shared.exceptionhandler;


import com.king.orderflow.shared.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response<String>> handleResponseStatusException(ResponseStatusException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage(), ex.getStatusCode().value()),
                HttpStatus.valueOf(ex.getStatusCode().value())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()),
                HttpStatus.BAD_REQUEST
        );
    }
}
