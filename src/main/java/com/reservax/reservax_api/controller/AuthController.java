package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/csrf")
    public Map<String, String> obterCsrf(CsrfToken token) {
        return Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName()
        );
    }

    @PostMapping("/cadastro-admin")
    public ResponseEntity<Usuario> cadastrarAdmin(
            @RequestBody Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criarAdmin(usuario));
    }

    @GetMapping("/me")
    public Usuario usuarioAtual(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName());
    }
}
