package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.service.ReservaService;
import org.springframework.http.ResponseEntity;
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
    public List<Reserva> listarTodas() {
        return reservaService.listarTodas();
    }

    @GetMapping("/minhas")
    public List<Reserva> listarMinhasReservas(@RequestParam String solicitante) {
        return reservaService.listarReservasDoSolicitante(solicitante);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {

        return reservaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Reserva salvar(@RequestBody Reserva reserva) {

        return reservaService.salvar(reserva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> atualizar(
            @PathVariable Long id,
            @RequestParam String solicitante,
            @RequestBody Reserva reserva) {

        Reserva reservaAtualizada =
                reservaService.atualizar(
                        id,
                        solicitante,
                        reserva
                );

        return ResponseEntity.ok(reservaAtualizada);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(
            @PathVariable Long id,
            @RequestParam String solicitante) {

        Reserva reservaCancelada =
                reservaService.cancelar(id, solicitante);

        return ResponseEntity.ok(reservaCancelada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        reservaService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}