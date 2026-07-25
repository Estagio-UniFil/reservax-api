package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.repository.AmbienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AmbienteService {

    private final AmbienteRepository ambienteRepository;

    public AmbienteService(AmbienteRepository ambienteRepository) {
        this.ambienteRepository = ambienteRepository;
    }

    public List<Ambiente> listarTodos() {
        return ambienteRepository.findAll();
    }

    public Optional<Ambiente> buscarPorId(Long id) {
        return ambienteRepository.findById(id);
    }

    public Ambiente salvar(Ambiente ambiente) {
        return ambienteRepository.save(ambiente);
    }

    public Ambiente atualizar(Long id, Ambiente ambienteAtualizado) {

        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ambiente não encontrado"));

        ambiente.setNome(ambienteAtualizado.getNome());
        ambiente.setTipo(ambienteAtualizado.getTipo());
        ambiente.setCapacidade(ambienteAtualizado.getCapacidade());
        ambiente.setLocalizacao(ambienteAtualizado.getLocalizacao());
        ambiente.setDescricao(ambienteAtualizado.getDescricao());
        ambiente.setPossuiProjetor(ambienteAtualizado.getPossuiProjetor());
        ambiente.setPossuiTv(ambienteAtualizado.getPossuiTv());
        ambiente.setPossuiMicrofone(ambienteAtualizado.getPossuiMicrofone());
        ambiente.setPossuiComputadores(ambienteAtualizado.getPossuiComputadores());
        ambiente.setAtivo(ambienteAtualizado.getAtivo());

        return ambienteRepository.save(ambiente);
    }

    public void excluir(Long id) {
        ambienteRepository.deleteById(id);
    }
}