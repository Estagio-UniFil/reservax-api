package com.reservax.reservax_api.dto;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Usuario;

public record ConfiguracaoUsuarioResponse(
        Long id,
        String nome,
        String email,
        TipoUsuario tipoUsuario,
        boolean notificacoesAtivas) {

    public static ConfiguracaoUsuarioResponse de(Usuario usuario) {
        return new ConfiguracaoUsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTipoUsuario(),
                usuario.getNotificacoesAtivas()
        );
    }
}
