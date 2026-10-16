package com.reservax.reservax_api.dto;

import com.reservax.reservax_api.model.Reserva;

import java.util.List;

public record ReservaLoteResponse(
        int quantidade,
        List<Reserva> reservas) {

    public static ReservaLoteResponse de(List<Reserva> reservas) {
        return new ReservaLoteResponse(reservas.size(), reservas);
    }
}
