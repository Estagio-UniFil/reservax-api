package com.reservax.reservax_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TelaController {

    @GetMapping("/salas")
    public String salas() {
        return "salas";
    }

    @GetMapping("/solicitar-reserva")
    public String solicitarReserva() {
        return "solicitar-reserva";
    }

    @GetMapping("/minhas-reservas")
    public String minhasReservas() {
        return "minhas-reservas";
    }

}