package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.config.SecurityConfig;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.service.ReservaService;
import com.reservax.reservax_api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = {
        TelaController.class,
        ReservaController.class,
        ConfiguracaoController.class,
        UsuarioController.class
}, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class AutorizacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    void administradorNaoDeveAcessarTelaMinhasReservas() throws Exception {
        mockMvc.perform(get("/minhas-reservas")
                        .with(user("admin@reservax.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDeveAcessarTelaMinhasReservas() throws Exception {
        mockMvc.perform(get("/minhas-reservas")
                        .with(user("usuario@reservax.com").roles("USUARIO")))
                .andExpect(status().isOk())
                .andExpect(view().name("minhas-reservas"));
    }

    @Test
    void deveExibirCadastroPublicoQuandoAindaNaoExisteAdministrador()
            throws Exception {

        when(usuarioService.cadastroAdminDisponivel()).thenReturn(true);

        mockMvc.perform(get("/cadastro-admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro-admin"));
    }

    @Test
    void deveBloquearCadastroPublicoDepoisDoPrimeiroAdministrador()
            throws Exception {

        when(usuarioService.cadastroAdminDisponivel()).thenReturn(false);

        mockMvc.perform(get("/cadastro-admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?cadastroAdminIndisponivel=true"
                ));
    }

    @Test
    void usuarioComumNaoDeveCriarAdministradorPelaGestaoDeUsuarios()
            throws Exception {

        mockMvc.perform(post("/api/usuarios")
                        .with(user("usuario@reservax.com").roles("USUARIO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Novo Admin",
                                  "email": "novo@reservax.com",
                                  "senha": "senha123",
                                  "tipoUsuario": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorDeveCriarOutroAdministradorPelaGestaoDeUsuarios()
            throws Exception {

        mockMvc.perform(post("/api/usuarios")
                        .with(user("admin@reservax.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Novo Admin",
                                  "email": "novo@reservax.com",
                                  "senha": "senha123",
                                  "tipoUsuario": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(usuarioService).criarAdministradorGerenciado(
                any(Usuario.class)
        );
    }

    @Test
    void administradorNaoDeveAcessarEndpointDeReservasPessoais()
            throws Exception {

        mockMvc.perform(get("/reservas/minhas")
                        .with(user("admin@reservax.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDeveAcessarEndpointDeReservasPessoais() throws Exception {
        mockMvc.perform(get("/reservas/minhas")
                        .with(user("usuario@reservax.com").roles("USUARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioComumDevePoderExcluirLogicamenteAPropriaConta()
            throws Exception {

        MockHttpSession sessao = new MockHttpSession();

        mockMvc.perform(delete("/api/configuracoes/conta")
                        .with(user("usuario@reservax.com").roles("USUARIO"))
                        .session(sessao)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(usuarioService).excluirMinhaConta("usuario@reservax.com");
        assertTrue(sessao.isInvalid());
    }

    @Test
    void administradorDevePoderSolicitarExclusaoDaPropriaConta()
            throws Exception {

        MockHttpSession sessao = new MockHttpSession();

        mockMvc.perform(delete("/api/configuracoes/conta")
                        .with(user("admin@reservax.com").roles("ADMIN"))
                        .session(sessao)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(usuarioService).excluirMinhaConta("admin@reservax.com");
        assertTrue(sessao.isInvalid());
    }
}
