package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TelaController {

    private final UsuarioService usuarioService;

    public TelaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute(
                "cadastroAdminDisponivel",
                usuarioService.cadastroAdminDisponivel()
        );
        return "login";
    }

    @GetMapping("/cadastro-admin")
    public String cadastroAdmin() {
        if (!usuarioService.cadastroAdminDisponivel()) {
            return "redirect:/login?cadastroAdminIndisponivel=true";
        }

        return "cadastro-admin";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/salas")
    public String salas() {
        return "salas";
    }

    @GetMapping("/solicitar-reserva")
    public String solicitarReserva() {
        return "solicitar-reserva";
    }

    @GetMapping("/minhas-reservas")
    public String minhasReservas() {
        return "minhas-reservas";
    }

    @GetMapping("/gerenciar-solicitacoes")
    public String gerenciarSolicitacoes() {
        return "gerenciar-solicitacoes";
    }

    @GetMapping("/usuarios")
    public String usuarios() {
        return "usuarios";
    }

    @GetMapping("/configuracoes")
    public String configuracoes() {
        return "configuracoes";
    }

}
