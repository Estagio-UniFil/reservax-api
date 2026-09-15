package com.reservax.reservax_api.service;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = buscarPorEmail(email);

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(usuario.getTipoUsuario().name())
                .disabled(!usuario.getAtivo())
                .build();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado.")
                );
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Usuario criarAdmin(Usuario dados) {
        if (!cadastroAdminDisponivel()) {
            throw new RuntimeException(
                    "O cadastro público de administrador não está mais disponível."
            );
        }

        return criar(dados, TipoUsuario.ADMIN);
    }

    public Usuario criarAdministradorGerenciado(Usuario dados) {
        return criar(dados, TipoUsuario.ADMIN);
    }

    @Transactional(readOnly = true)
    public boolean cadastroAdminDisponivel() {
        return !usuarioRepository.existsByTipoUsuario(
                TipoUsuario.ADMIN
        );
    }

    public Usuario criarUsuario(Usuario dados) {
        return criar(dados, TipoUsuario.USUARIO);
    }

    public Usuario atualizar(Long id, Usuario dados) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado.")
                );

        validarNomeEEmail(dados);

        String emailNormalizado = normalizarEmail(dados.getEmail());
        boolean emailAlterado =
                !usuario.getEmail().equalsIgnoreCase(emailNormalizado);

        if (emailAlterado &&
                usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RuntimeException("Já existe uma conta com este e-mail.");
        }

        usuario.setNome(dados.getNome().trim());
        usuario.setEmail(emailNormalizado);

        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            validarSenha(dados.getSenha());
            usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
        }

        return usuarioRepository.save(usuario);
    }

    public Usuario desativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado.")
                );

        if (usuario.getTipoUsuario() == TipoUsuario.ADMIN) {
            throw new RuntimeException(
                    "Contas de administrador não podem ser excluídas pela tela Usuários."
            );
        }

        usuario.setAtivo(false);
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizarPreferenciaNotificacoes(
            String email,
            boolean notificacoesAtivas) {

        Usuario usuario = buscarPorEmail(email);
        usuario.setNotificacoesAtivas(notificacoesAtivas);
        return usuarioRepository.save(usuario);
    }

    public void excluirMinhaConta(String email) {
        Usuario usuario = buscarPorEmail(email);

        if (usuario.getTipoUsuario() != TipoUsuario.ADMIN) {
            throw new RuntimeException(
                    "A exclusão da própria conta está disponível apenas para administradores."
            );
        }

        List<Usuario> administradoresAtivos = usuarioRepository
                .buscarAtivosParaAtualizacao(TipoUsuario.ADMIN);

        if (administradoresAtivos.size() <= 1) {
            throw new RuntimeException(
                    "Não é possível excluir o último administrador ativo."
            );
        }

        usuario.setAtivo(false);
        usuario.setNotificacoesAtivas(false);
        usuario.setDataExclusao(LocalDateTime.now());
        usuario.setEmail(gerarEmailDeContaExcluida(usuario.getId()));
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));

        usuarioRepository.save(usuario);
    }

    private Usuario criar(Usuario dados, TipoUsuario tipoUsuario) {
        validarNomeEEmail(dados);
        validarSenha(dados.getSenha());

        String emailNormalizado = normalizarEmail(dados.getEmail());

        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RuntimeException("Já existe uma conta com este e-mail.");
        }

        dados.setId(null);
        dados.setNome(dados.getNome().trim());
        dados.setEmail(emailNormalizado);
        dados.setSenha(passwordEncoder.encode(dados.getSenha()));
        dados.setTipoUsuario(tipoUsuario);
        dados.setAtivo(true);
        dados.setNotificacoesAtivas(true);
        dados.setDataExclusao(null);

        return usuarioRepository.save(dados);
    }

    private void validarNomeEEmail(Usuario dados) {
        if (dados.getNome() == null || dados.getNome().isBlank()) {
            throw new RuntimeException("Informe o nome do usuário.");
        }

        if (dados.getEmail() == null ||
                dados.getEmail().isBlank() ||
                !dados.getEmail().contains("@")) {
            throw new RuntimeException("Informe um e-mail válido.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < 6) {
            throw new RuntimeException(
                    "A senha deve possuir pelo menos 6 caracteres."
            );
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String gerarEmailDeContaExcluida(Long id) {
        return "conta-excluida-" + id + "-" + UUID.randomUUID()
                + "@reservax.invalid";
    }
}
