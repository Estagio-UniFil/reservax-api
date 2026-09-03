package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.repository.AmbienteRepository;
import com.reservax.reservax_api.repository.ReservaRepository;
import com.reservax.reservax_api.enums.StatusReserva;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public List<Reserva> listarReservasDoSolicitante(String solicitante) {
        return reservaRepository.findBySolicitante(solicitante);

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
            .findByAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                    ambiente.getId(),
                    reserva.getDataReserva(),
                    List.of(
                            StatusReserva.PENDENTE,
                            StatusReserva.APROVADA
                    ),
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

    public Reserva atualizar(
            Long id,
            String solicitante,
            Reserva reservaAtualizada) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (!reserva.getSolicitante().equalsIgnoreCase(solicitante)) {
            throw new RuntimeException(
                    "Você não pode editar a reserva de outro usuário."
            );
        }

        if (
                reserva.getStatus() == StatusReserva.CANCELADA ||
                reserva.getStatus() == StatusReserva.REJEITADA
        ) {
            throw new RuntimeException(
                    "Esta reserva não pode ser editada."
            );
        }

        Ambiente ambiente = ambienteRepository.findById(
                reservaAtualizada.getAmbiente().getId()
        ).orElseThrow(() ->
                new RuntimeException("Ambiente não encontrado.")
        );

        if (!ambiente.getAtivo()) {
            throw new RuntimeException(
                    "Não é possível reservar um ambiente inativo."
            );
        }

        if (!reservaAtualizada.getHoraInicio()
                .isBefore(reservaAtualizada.getHoraFim())) {

            throw new RuntimeException(
                    "A hora de início deve ser menor que a hora de término."
            );
        }

        LocalDateTime novoInicio = LocalDateTime.of(
                reservaAtualizada.getDataReserva(),
                reservaAtualizada.getHoraInicio()
        );

        if (novoInicio.isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "A reserva deve possuir uma data e horário futuros."
            );
        }

        boolean possuiConflito = !reservaRepository
                .findByIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        id,
                        ambiente.getId(),
                        reservaAtualizada.getDataReserva(),
                        List.of(
                                StatusReserva.PENDENTE,
                                StatusReserva.APROVADA
                        ),
                        reservaAtualizada.getHoraFim(),
                        reservaAtualizada.getHoraInicio()
                )
                .isEmpty();

        if (possuiConflito) {
            throw new RuntimeException(
                    "Já existe uma reserva para este ambiente neste horário."
            );
        }

        reserva.setAmbiente(ambiente);
        reserva.setDataReserva(reservaAtualizada.getDataReserva());
        reserva.setHoraInicio(reservaAtualizada.getHoraInicio());
        reserva.setHoraFim(reservaAtualizada.getHoraFim());
        reserva.setObservacao(reservaAtualizada.getObservacao());

        reserva.setStatus(StatusReserva.PENDENTE);

        return reservaRepository.save(reserva);
    }

    public Reserva cancelar(Long id, String solicitante) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (!reserva.getSolicitante().equalsIgnoreCase(solicitante)) {
            throw new RuntimeException(
                    "Você não pode cancelar a reserva de outro usuário."
            );
        }

        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new RuntimeException(
                    "Esta reserva já está cancelada."
            );
        }

        if (reserva.getStatus() == StatusReserva.REJEITADA) {
            throw new RuntimeException(
                    "Uma reserva rejeitada não pode ser cancelada."
            );
        }

        LocalDateTime inicioReserva = LocalDateTime.of(
                reserva.getDataReserva(),
                reserva.getHoraInicio()
        );

        if (inicioReserva.isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Uma reserva passada não pode ser cancelada."
            );
        }

        reserva.setStatus(StatusReserva.CANCELADA);

        return reservaRepository.save(reserva);
    }

    public void excluir(Long id) {

        if (!reservaRepository.existsById(id)) {

            throw new RuntimeException("Reserva não encontrada.");
        }

        reservaRepository.deleteById(id);
    }
}