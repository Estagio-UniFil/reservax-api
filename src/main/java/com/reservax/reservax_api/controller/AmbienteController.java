package com.reservax.reservax_api.controller;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.service.AmbienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ambientes")
public class AmbienteController {

    private final AmbienteService ambienteService;

    public AmbienteController(AmbienteService ambienteService) {
        this.ambienteService = ambienteService;
    }

    @GetMapping
    public List<Ambiente> listarTodos() {
        return ambienteService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ambiente> buscarPorId(@PathVariable Long id) {
        return ambienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Ambiente salvar(@RequestBody Ambiente ambiente) {
        return ambienteService.salvar(ambiente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Ambiente> atualizar(@PathVariable Long id, @RequestBody Ambiente ambiente) {
        Ambiente ambienteAtualizado = ambienteService.atualizar(id, ambiente);
        return ResponseEntity.ok(ambienteAtualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ambienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
