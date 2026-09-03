package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.enums.StatusReserva;
import com.reservax.reservax_api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    List<Reserva> findByAmbienteId(Long ambienteId);

    List<Reserva> findByDataReserva(LocalDate dataReserva);

    List<Reserva> findBySolicitante(String solicitante);

    List<Reserva> findByAmbienteIdAndDataReserva(
            Long ambienteId,
            LocalDate dataReserva
    );

    List<Reserva> findByAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
            Long ambienteId,
            LocalDate dataReserva,
            List<StatusReserva> status,
            LocalTime horaFim,
            LocalTime horaInicio
    );

    List<Reserva> findByIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
            Long reservaId,
            Long ambienteId,
            LocalDate dataReserva,
            List<StatusReserva> status,
            LocalTime horaFim,
            LocalTime horaInicio
    );
}
