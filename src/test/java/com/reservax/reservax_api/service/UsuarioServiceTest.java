package com.reservax.reservax_api.service;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void configurar() {
        passwordEncoder = new BCryptPasswordEncoder();
        usuarioService = new UsuarioService(
                usuarioRepository,
                passwordEncoder
        );

        lenient().when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void cadastroPublicoDeveCriarSomenteAdministradorComSenhaCriptografada() {
        Usuario dados = criarDadosUsuario();

        Usuario resultado = usuarioService.criarAdmin(dados);

        assertEquals(TipoUsuario.ADMIN, resultado.getTipoUsuario());
        assertTrue(resultado.getAtivo());
        assertTrue(resultado.getNotificacoesAtivas());
        assertTrue(passwordEncoder.matches("senha123", resultado.getSenha()));
    }

    @Test
    void cadastroPublicoNaoDeveCriarSegundoAdministrador() {
        when(usuarioRepository.existsByTipoUsuario(
                TipoUsuario.ADMIN
        )).thenReturn(true);

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> usuarioService.criarAdmin(criarDadosUsuario())
        );

        assertEquals(
                "O cadastro público de administrador não está mais disponível.",
                erro.getMessage()
        );
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void administradorAutenticadoDevePoderCriarOutroAdministrador() {
        Usuario resultado = usuarioService.criarAdministradorGerenciado(
                criarDadosUsuario()
        );

        assertEquals(TipoUsuario.ADMIN, resultado.getTipoUsuario());
        assertTrue(resultado.getAtivo());
        verify(usuarioRepository).save(resultado);
    }

    @Test
    void cadastroInternoDeveCriarUsuarioComum() {
        Usuario dados = criarDadosUsuario();

        Usuario resultado = usuarioService.criarUsuario(dados);

        assertEquals(TipoUsuario.USUARIO, resultado.getTipoUsuario());
        assertTrue(resultado.getNotificacoesAtivas());
        assertTrue(passwordEncoder.matches("senha123", resultado.getSenha()));
    }

    @Test
    void naoDeveDesativarContaAdministrativaPelaTelaUsuarios() {
        Usuario administrador = criarUsuarioPersistido(
                1L,
                "Admin",
                "admin@reservax.com",
                TipoUsuario.ADMIN
        );
        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(administrador));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> usuarioService.desativar(1L)
        );

        assertEquals(
                "Contas de administrador não podem ser excluídas pela tela Usuários.",
                erro.getMessage()
        );
        assertTrue(administrador.getAtivo());
        verify(usuarioRepository, never()).save(administrador);
    }

    @Test
    void deveDesativarUsuarioComumPelaTelaUsuarios() {
        Usuario usuario = criarUsuarioPersistido(
                2L,
                "Usuário",
                "usuario@reservax.com",
                TipoUsuario.USUARIO
        );
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.desativar(2L);

        assertFalse(resultado.getAtivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void naoDeveExcluirOUltimoAdministradorAtivo() {
        Usuario administrador = criarUsuarioPersistido(
                1L,
                "Admin",
                "admin@reservax.com",
                TipoUsuario.ADMIN
        );
        when(usuarioRepository.findByEmailIgnoreCase("admin@reservax.com"))
                .thenReturn(Optional.of(administrador));
        when(usuarioRepository.buscarAtivosParaAtualizacao(TipoUsuario.ADMIN))
                .thenReturn(List.of(administrador));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> usuarioService.excluirMinhaConta("admin@reservax.com")
        );

        assertEquals(
                "Não é possível excluir o último administrador ativo.",
                erro.getMessage()
        );
        assertTrue(administrador.getAtivo());
        verify(usuarioRepository, never()).save(administrador);
    }

    @Test
    void deveExcluirLogicamenteAdministradorELiberarEmail() {
        Usuario administrador = criarUsuarioPersistido(
                1L,
                "Admin",
                "admin@reservax.com",
                TipoUsuario.ADMIN
        );
        Usuario outroAdministrador = criarUsuarioPersistido(
                2L,
                "Outro Admin",
                "outro@reservax.com",
                TipoUsuario.ADMIN
        );
        String senhaAnterior = administrador.getSenha();

        when(usuarioRepository.findByEmailIgnoreCase("admin@reservax.com"))
                .thenReturn(Optional.of(administrador));
        when(usuarioRepository.buscarAtivosParaAtualizacao(TipoUsuario.ADMIN))
                .thenReturn(List.of(administrador, outroAdministrador));

        usuarioService.excluirMinhaConta("admin@reservax.com");

        assertFalse(administrador.getAtivo());
        assertFalse(administrador.getNotificacoesAtivas());
        assertNotNull(administrador.getDataExclusao());
        assertNotEquals("admin@reservax.com", administrador.getEmail());
        assertTrue(administrador.getEmail().endsWith("@reservax.invalid"));
        assertNotEquals(senhaAnterior, administrador.getSenha());
        verify(usuarioRepository).save(administrador);
    }

    @Test
    void deveAtualizarPreferenciaDeNotificacoes() {
        Usuario usuario = criarUsuarioPersistido(
                2L,
                "Usuário",
                "usuario@reservax.com",
                TipoUsuario.USUARIO
        );
        when(usuarioRepository.findByEmailIgnoreCase("usuario@reservax.com"))
                .thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.atualizarPreferenciaNotificacoes(
                "usuario@reservax.com",
                false
        );

        assertFalse(resultado.getNotificacoesAtivas());
        verify(usuarioRepository).save(usuario);
    }

    private Usuario criarDadosUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Maria Silva");
        usuario.setEmail("MARIA@EXEMPLO.COM");
        usuario.setSenha("senha123");
        return usuario;
    }

    private Usuario criarUsuarioPersistido(
            Long id,
            String nome,
            String email,
            TipoUsuario tipoUsuario) {

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha123"));
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setAtivo(true);
        usuario.setNotificacoesAtivas(true);
        return usuario;
    }
}
