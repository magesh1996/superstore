package com.superstore.app.config.exception;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Order(Ordered.HIGHEST_PRECEDENCE) // ensures this advice is evaluated before other @ControllerAdvice beans if we have multiple.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // order matters implicitly - Spring picks the most specific exception type match regardless of method order in the file, 
    // so we don't need to worry about arranging handlers top-to-bottom by specificity.

    // 400 BAD_REQUEST
    // invalid input/arguments passed by client
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("bad request : {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    // 503 SERVICE_UNAVAILABLE
    // downstream service (WebClient) is down/unavailable/failed/timeout
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> handleWebClientRequest(WebClientRequestException ex, HttpServletRequest request) {
        logger.warn("remote service unavailable : {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
    }

    // 500 INTERNAL_SERVER_ERROR
    // unhandled runtime/unchecked exception
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        logger.error("unhandled runtime exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    // 500 INTERNAL_SERVER_ERROR
    // unhandled checked exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        logger.error("unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    // 400 BAD_REQUEST
    // @Valid request body failing validation throws MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.error("validation failed exception", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, Throwable ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            status.value(),
            status.getReasonPhrase(),
            getRootMessage(ex),
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : "unexpected error";
    }
}
