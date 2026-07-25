package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByAmbienteId(Long ambienteId);

    List<Reserva> findByDataReserva(LocalDate dataReserva);

    List<Reserva> findBySolicitante(String solicitante);

    List<Reserva> findByAmbienteIdAndDataReserva(
            Long ambienteId,
            LocalDate dataReserva
    );

    /**
     * Busca reservas do mesmo ambiente e mesma data
     * para validação de conflito de horário.
     */
    List<Reserva> findByAmbienteIdAndDataReservaAndHoraInicioLessThanAndHoraFimGreaterThan(
            Long ambienteId,
            LocalDate dataReserva,
            LocalTime horaFim,
            LocalTime horaInicio
    );
}