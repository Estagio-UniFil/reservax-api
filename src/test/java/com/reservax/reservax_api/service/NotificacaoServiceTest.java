package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Notificacao;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.NotificacaoRepository;
import com.reservax.reservax_api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificacaoService notificacaoService;

    @Test
    void deveMarcarComoLidaApenasNotificacaoDoUsuarioAutenticado() {
        Usuario usuario = criarUsuario("mario@reservax.com");
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);

        when(notificacaoRepository.findById(1L))
                .thenReturn(Optional.of(notificacao));
        when(notificacaoRepository.save(notificacao))
                .thenReturn(notificacao);

        Notificacao resultado = notificacaoService.marcarComoLida(
                1L,
                "mario@reservax.com"
        );

        assertTrue(resultado.isLida());
        verify(notificacaoRepository).save(notificacao);
    }

    @Test
    void naoDeveAbrirNotificacaoDeOutroUsuario() {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(criarUsuario("outro@reservax.com"));

        when(notificacaoRepository.findById(1L))
                .thenReturn(Optional.of(notificacao));

        assertThrows(
                RuntimeException.class,
                () -> notificacaoService.marcarComoLida(
                        1L,
                        "mario@reservax.com"
                )
        );
    }

    @Test
    void naoDeveCriarNotificacaoQuandoUsuarioDesativouAPreferencia() {
        Usuario usuario = criarUsuario("mario@reservax.com");
        usuario.setNotificacoesAtivas(false);

        notificacaoService.notificarUsuario(
                usuario,
                "Reserva aprovada.",
                "/minhas-reservas"
        );

        verify(notificacaoRepository, never()).save(any(Notificacao.class));
        verify(emailService, never()).enviarNotificacao(any(), any());
    }

    @Test
    void deveCriarNotificacaoEEnviarEmailQuandoPreferenciaEstaAtiva() {
        Usuario usuario = criarUsuario("mario@reservax.com");
        usuario.setNotificacoesAtivas(true);

        notificacaoService.notificarUsuario(
                usuario,
                "Reserva aprovada.",
                "/minhas-reservas"
        );

        verify(notificacaoRepository).save(any(Notificacao.class));
        verify(emailService).enviarNotificacao(
                usuario,
                "Reserva aprovada."
        );
    }

    private Usuario criarUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setAtivo(true);
        return usuario;
    }
}
