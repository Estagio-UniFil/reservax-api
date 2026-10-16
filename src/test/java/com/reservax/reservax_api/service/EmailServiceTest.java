package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void deveEnviarNotificacaoParaEmailDeLogin() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = criarProvider(mailSender);
        EmailService emailService = new EmailService(
                provider,
                true,
                "no-reply@reservax.com"
        );
        Usuario usuario = criarUsuarioAtivo();

        emailService.enviarNotificacao(usuario, "Sua reserva foi aprovada.");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("mario@reservax.com", captor.getValue().getTo()[0]);
        assertTrue(captor.getValue().getText().contains("Sua reserva foi aprovada."));
    }

    @Test
    void naoDeveEnviarQuandoIntegracaoEstaDesabilitada() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = criarProvider(mailSender);
        EmailService emailService = new EmailService(
                provider,
                false,
                "no-reply@reservax.com"
        );

        emailService.enviarNotificacao(
                criarUsuarioAtivo(),
                "Sua reserva foi aprovada."
        );

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> criarProvider(
            JavaMailSender mailSender) {

        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }

    private Usuario criarUsuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setNome("Mario");
        usuario.setEmail("mario@reservax.com");
        usuario.setAtivo(true);
        return usuario;
    }
}
