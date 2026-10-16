package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean habilitado;
    private final String remetente;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${reservax.email.enabled:false}") boolean habilitado,
            @Value("${reservax.email.from:no-reply@reservax.local}") String remetente) {

        this.mailSenderProvider = mailSenderProvider;
        this.habilitado = habilitado;
        this.remetente = remetente;
    }

    public void enviarNotificacao(Usuario usuario, String mensagem) {
        if (!habilitado || usuario == null || !usuario.getAtivo()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("Envio de e-mail habilitado, mas o SMTP não está configurado.");
            return;
        }

        String email = usuario.getEmail();
        if (email == null || email.isBlank() || email.endsWith("@reservax.invalid")) {
            return;
        }

        SimpleMailMessage emailNotificacao = new SimpleMailMessage();
        emailNotificacao.setFrom(remetente);
        emailNotificacao.setTo(email);
        emailNotificacao.setSubject("ReservaX - Atualização de reserva");
        emailNotificacao.setText(
                "Olá, " + usuario.getNome() + ".\n\n" +
                        mensagem + "\n\n" +
                        "Acesse o ReservaX para consultar os detalhes."
        );

        try {
            mailSender.send(emailNotificacao);
        } catch (MailException exception) {
            LOGGER.warn(
                    "Não foi possível enviar a notificação por e-mail para {}.",
                    email,
                    exception
            );
        }
    }
}
