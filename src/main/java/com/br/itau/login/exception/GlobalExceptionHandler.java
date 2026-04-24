package com.br.itau.login.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.br.itau.login.model.response.errors.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Failed", message,
                LocalDateTime.now());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(),
                LocalDateTime.now());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(com.br.itau.login.exception.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(com.br.itau.login.exception.UserNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(),
                LocalDateTime.now());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex) {
        ErrorResponse body = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Usuário inexistente ou senha inválida", LocalDateTime.now());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse body = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
