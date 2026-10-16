package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.repository.AmbienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AmbienteService {

    private final AmbienteRepository ambienteRepository;

    public AmbienteService(AmbienteRepository ambienteRepository) {
        this.ambienteRepository = ambienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Ambiente> listarTodos() {
        return ambienteRepository.findAllByDataExclusaoIsNullOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Ambiente> buscarPorId(Long id) {
        return ambienteRepository.findByIdAndDataExclusaoIsNull(id);
    }

    public Ambiente salvar(Ambiente ambiente) {
        ambiente.setId(null);
        ambiente.setDataExclusao(null);
        return ambienteRepository.save(ambiente);
    }

    public Ambiente atualizar(Long id, Ambiente ambienteAtualizado) {

        Ambiente ambiente = ambienteRepository.findByIdAndDataExclusaoIsNull(id)
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
        Ambiente ambiente = ambienteRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Ambiente não encontrado")
                );

        ambiente.setAtivo(false);
        ambiente.setDataExclusao(LocalDateTime.now());
        ambienteRepository.save(ambiente);
    }
}
