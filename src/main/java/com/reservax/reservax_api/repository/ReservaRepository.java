package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.enums.StatusReserva;
import com.reservax.reservax_api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE r.ambiente.id = :ambienteId
              AND r.dataExclusao IS NULL
            """)
    List<Reserva> findByAmbienteId(@Param("ambienteId") Long ambienteId);

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE r.dataReserva = :dataReserva
              AND r.dataExclusao IS NULL
            """)
    List<Reserva> findByDataReserva(
            @Param("dataReserva") LocalDate dataReserva
    );

    List<Reserva> findAllByDataExclusaoIsNull();

    Optional<Reserva> findByIdAndDataExclusaoIsNull(Long id);

    @Query("""
            SELECT r
            FROM Reserva r
            LEFT JOIN r.usuario u
            WHERE r.dataExclusao IS NULL
              AND (
                    u.id = :usuarioId
                    OR (
                        u IS NULL
                        AND LOWER(r.solicitante) = LOWER(:solicitante)
                    )
                  )
              AND (
                    r.dataReserva > :dataAtual
                    OR (r.dataReserva = :dataAtual AND r.horaFim > :horaAtual)
                  )
            ORDER BY r.dataReserva ASC, r.horaInicio ASC
            """)
    List<Reserva> findReservasAtuaisOuFuturasDoSolicitante(
            @Param("usuarioId") Long usuarioId,
            @Param("solicitante") String solicitante,
            @Param("dataAtual") LocalDate dataAtual,
            @Param("horaAtual") LocalTime horaAtual
    );

    @Query("""
            SELECT r
            FROM Reserva r
            LEFT JOIN r.usuario u
            WHERE r.dataExclusao IS NULL
              AND (
                    u.id = :usuarioId
                    OR (
                        u IS NULL
                        AND LOWER(r.solicitante) = LOWER(:solicitante)
                    )
                  )
              AND (
                    r.dataReserva < :dataAtual
                    OR (r.dataReserva = :dataAtual AND r.horaFim <= :horaAtual)
                  )
            ORDER BY r.dataReserva DESC, r.horaInicio DESC
            """)
    List<Reserva> findReservasAntigasDoSolicitante(
            @Param("usuarioId") Long usuarioId,
            @Param("solicitante") String solicitante,
            @Param("dataAtual") LocalDate dataAtual,
            @Param("horaAtual") LocalTime horaAtual
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reserva r
            SET r.status = :statusExpirada
            WHERE r.status = :statusPendente
              AND r.dataExclusao IS NULL
              AND (
                    r.dataReserva < :dataAtual
                    OR (r.dataReserva = :dataAtual AND r.horaFim <= :horaAtual)
                  )
            """)
    int expirarReservasPendentesEncerradas(
            @Param("statusPendente") StatusReserva statusPendente,
            @Param("statusExpirada") StatusReserva statusExpirada,
            @Param("dataAtual") LocalDate dataAtual,
            @Param("horaAtual") LocalTime horaAtual
    );

    List<Reserva> findByAmbienteIdAndDataReserva(
            Long ambienteId,
            LocalDate dataReserva
    );

    List<Reserva> findByDataExclusaoIsNullAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
            Long ambienteId,
            LocalDate dataReserva,
            List<StatusReserva> status,
            LocalTime horaFim,
            LocalTime horaInicio
    );

    List<Reserva> findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
            Long reservaId,
            Long ambienteId,
            LocalDate dataReserva,
            List<StatusReserva> status,
            LocalTime horaFim,
            LocalTime horaInicio
    );
}
