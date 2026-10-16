package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.dto.ConfiguracaoUsuarioResponse;
import com.reservax.reservax_api.dto.PreferenciaNotificacaoRequest;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes")
public class ConfiguracaoController {

    private final UsuarioService usuarioService;

    public ConfiguracaoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ConfiguracaoUsuarioResponse buscar(Authentication authentication) {
        return ConfiguracaoUsuarioResponse.de(
                usuarioService.buscarPorEmail(authentication.getName())
        );
    }

    @PatchMapping("/notificacoes")
    public ConfiguracaoUsuarioResponse atualizarNotificacoes(
            @RequestBody PreferenciaNotificacaoRequest preferencia,
            Authentication authentication) {

        Usuario usuario = usuarioService.atualizarPreferenciaNotificacoes(
                authentication.getName(),
                preferencia.notificacoesAtivas()
        );

        return ConfiguracaoUsuarioResponse.de(usuario);
    }

    @DeleteMapping("/conta")
    public ResponseEntity<Void> excluirMinhaConta(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {

        usuarioService.excluirMinhaConta(authentication.getName());

        new CookieClearingLogoutHandler("JSESSIONID", "remember-me")
                .logout(request, response, authentication);
        new SecurityContextLogoutHandler()
                .logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }
}
