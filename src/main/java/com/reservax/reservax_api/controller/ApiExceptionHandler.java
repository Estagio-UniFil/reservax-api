package com.reservax.reservax_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> tratarAcessoNegado(
            AccessDeniedException exception) {

        return ResponseEntity.status(403).body(
                Map.of("mensagem", "Você não possui permissão para esta ação.")
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> tratarErroDeNegocio(
            RuntimeException exception) {

        return ResponseEntity.badRequest().body(
                Map.of("mensagem", exception.getMessage())
        );
    }
}
