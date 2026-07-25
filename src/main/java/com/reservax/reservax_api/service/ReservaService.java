package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.repository.AmbienteRepository;
import com.reservax.reservax_api.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AmbienteRepository ambienteRepository;

    public ReservaService(ReservaRepository reservaRepository,
                           AmbienteRepository ambienteRepository) {

        this.reservaRepository = reservaRepository;
        this.ambienteRepository = ambienteRepository;
    }

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return reservaRepository.findById(id);
    }

    public Reserva salvar(Reserva reserva) {

        // Verifica se o ambiente existe
        Ambiente ambiente = ambienteRepository.findById(
                reserva.getAmbiente().getId()
        ).orElseThrow(() ->
                new RuntimeException("Ambiente não encontrado.")
        );

        // Verifica se o ambiente está ativo
        if (!ambiente.getAtivo()) {
            throw new RuntimeException(
                    "Não é possível reservar um ambiente inativo."
            );
        }

        // Valida horário
        if (!reserva.getHoraInicio().isBefore(reserva.getHoraFim())) {

            throw new RuntimeException(
                    "A hora de início deve ser menor que a hora de término."
            );
        }

        // Verifica conflito de horário
        boolean possuiConflito = !reservaRepository
                .findByAmbienteIdAndDataReservaAndHoraInicioLessThanAndHoraFimGreaterThan(
                        ambiente.getId(),
                        reserva.getDataReserva(),
                        reserva.getHoraFim(),
                        reserva.getHoraInicio()
                )
                .isEmpty();

        if (possuiConflito) {

            throw new RuntimeException(
                    "Já existe uma reserva para este ambiente neste horário."
            );
        }

        reserva.setAmbiente(ambiente);

        return reservaRepository.save(reserva);
    }

    public Reserva atualizar(Long id, Reserva reservaAtualizada) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        reserva.setSolicitante(reservaAtualizada.getSolicitante());
        reserva.setDataReserva(reservaAtualizada.getDataReserva());
        reserva.setHoraInicio(reservaAtualizada.getHoraInicio());
        reserva.setHoraFim(reservaAtualizada.getHoraFim());
        reserva.setStatus(reservaAtualizada.getStatus());
        reserva.setObservacao(reservaAtualizada.getObservacao());

        return reservaRepository.save(reserva);
    }

    public void excluir(Long id) {

        if (!reservaRepository.existsById(id)) {

            throw new RuntimeException("Reserva não encontrada.");
        }

        reservaRepository.deleteById(id);
    }
}