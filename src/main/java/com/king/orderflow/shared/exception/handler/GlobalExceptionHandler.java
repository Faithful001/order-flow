package com.king.orderflow.shared.exception.handler;


import com.king.orderflow.shared.exception.InstrumentNotTradingException;
import com.king.orderflow.shared.exception.InvalidOrderException;
import com.king.orderflow.shared.exception.UnknownInstrumentException;
import com.king.orderflow.shared.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response<List<String>>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(
                    Response.error(ex.getMessage())
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UnknownInstrumentException.class)
    public ResponseEntity<Response<String>> handleUnknowInstrumentException(UnknownInstrumentException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InstrumentNotTradingException.class)
    public ResponseEntity<Response<String>> handleInstrumentNotTradingException(InstrumentNotTradingException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<Response<String>> handleInvalidOrderException(InvalidOrderException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }
}
