package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Reserva> listarTodas() {
        return reservaService.listarTodas();
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('USUARIO')")
    public List<Reserva> listarMinhasReservas(
            @RequestParam(defaultValue = "false") boolean antigas,
            Authentication authentication) {

        return reservaService.listarReservasDoSolicitante(
                authentication.getName(),
                antigas
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {

        return reservaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('USUARIO')")
    public Reserva salvar(
            @RequestBody Reserva reserva,
            Authentication authentication) {

        return reservaService.salvar(
                reserva,
                authentication.getName()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Reserva> atualizar(
            @PathVariable Long id,
            @RequestBody Reserva reserva,
            Authentication authentication) {

        Reserva reservaAtualizada =
                reservaService.atualizar(
                        id,
                        authentication.getName(),
                        reserva
                );

        return ResponseEntity.ok(reservaAtualizada);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Reserva> cancelar(
            @PathVariable Long id,
            Authentication authentication) {

        Reserva reservaCancelada =
                reservaService.cancelar(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(reservaCancelada);
    }

    @PatchMapping("/{id}/reabrir")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Reserva> reabrir(
            @PathVariable Long id,
            Authentication authentication) {

        Reserva reservaReaberta =
                reservaService.reabrir(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(reservaReaberta);
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    public Reserva aprovar(@PathVariable Long id) {
        return reservaService.aprovar(id);
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    public Reserva rejeitar(@PathVariable Long id) {
        return reservaService.rejeitar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        reservaService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
