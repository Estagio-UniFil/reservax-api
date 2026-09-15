package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> listarTodos() {
        return usuarioService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Usuario> criarUsuario(
            @RequestBody Usuario usuario) {

        Usuario usuarioCriado = usuario.getTipoUsuario() == null ||
                usuario.getTipoUsuario() != TipoUsuario.ADMIN
                ? usuarioService.criarUsuario(usuario)
                : usuarioService.criarAdministradorGerenciado(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioCriado);
    }

    @PutMapping("/{id}")
    public Usuario atualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario) {

        return usuarioService.atualizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    public Usuario desativar(@PathVariable Long id) {

        return usuarioService.desativar(id);
    }
}
