package com.reservax.reservax_api.service;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Notificacao;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.NotificacaoRepository;
import com.reservax.reservax_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            UsuarioRepository usuarioRepository) {

        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listarNaoLidas(String email) {
        return notificacaoRepository
                .findByUsuarioEmailIgnoreCaseAndLidaFalseOrderByDataCriacaoDesc(
                        email
                );
    }

    public Notificacao marcarComoLida(Long id, String email) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notificação não encontrada.")
                );

        if (!notificacao.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException(
                    "Você não pode acessar a notificação de outro usuário."
            );
        }

        notificacao.setLida(true);
        return notificacaoRepository.save(notificacao);
    }

    public void notificarAdministradores(String mensagem, String rota) {
        List<Usuario> administradores = usuarioRepository
                .findByTipoUsuarioAndAtivoTrue(TipoUsuario.ADMIN);

        administradores.stream()
                .filter(Usuario::getNotificacoesAtivas)
                .forEach(administrador ->
                        criar(administrador, mensagem, rota)
                );
    }

    public void notificarUsuario(
            Usuario usuario,
            String mensagem,
            String rota) {

        if (usuario != null &&
                usuario.getAtivo() &&
                usuario.getNotificacoesAtivas()) {
            criar(usuario, mensagem, rota);
        }
    }

    private void criar(
            Usuario usuario,
            String mensagem,
            String rota) {

        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setMensagem(mensagem);
        notificacao.setRota(rota);

        notificacaoRepository.save(notificacao);
    }
}
