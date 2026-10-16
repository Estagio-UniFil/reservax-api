package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.model.Notificacao;
import com.reservax.reservax_api.service.NotificacaoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/nao-lidas")
    public List<Notificacao> listarNaoLidas(Authentication authentication) {
        return notificacaoService.listarNaoLidas(authentication.getName());
    }

    @PatchMapping("/{id}/ler")
    public Notificacao marcarComoLida(
            @PathVariable Long id,
            Authentication authentication) {

        return notificacaoService.marcarComoLida(
                id,
                authentication.getName()
        );
    }
}
