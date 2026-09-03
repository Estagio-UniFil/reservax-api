package com.reservax.reservax_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> tratarErroDeNegocio(
            RuntimeException exception) {

        return ResponseEntity.badRequest().body(
                Map.of("mensagem", exception.getMessage())
        );
    }
}
