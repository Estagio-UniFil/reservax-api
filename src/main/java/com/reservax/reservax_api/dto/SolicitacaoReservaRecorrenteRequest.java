package com.reservax.reservax_api.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record SolicitacaoReservaRecorrenteRequest(
        Long ambienteId,
        LocalDate dataInicio,
        LocalDate dataFim,
        Set<DayOfWeek> diasSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        String observacao) {
}
