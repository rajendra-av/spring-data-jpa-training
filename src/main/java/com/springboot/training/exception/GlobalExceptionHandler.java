package com.springboot.training.exception;


import com.springboot.training.dto.ErrorResponse;
import com.springboot.training.dto.FieldViolation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Applies to every controller; JSON responses because it is a @RestControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404: thrown by the service when the id does not exist
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    // 400: @Valid failed on a @RequestBody; one FieldViolation per invalid field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", violations);
    }

    // 400: malformed JSON body, or wrong type for a path variable or parameter (e.g. /employees/abc)
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request", List.of());
    }

    // 404: no controller method matches the URL
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnknownUrl(Exception ex) {
        return build(HttpStatus.NOT_FOUND, "No endpoint found for this URL", List.of());
    }

    // 405: URL exists but not for this HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), List.of());
    }

    // 500: anything not handled above; log the real cause, return a generic message
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", List.of());
    }

    // Single place that creates the response, so every error has the same shape
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                List<FieldViolation> violations) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, violations));
    }
}