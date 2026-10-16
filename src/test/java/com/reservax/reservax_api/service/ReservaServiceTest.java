package com.reservax.reservax_api.service;

import com.reservax.reservax_api.dto.SolicitacaoReservaRecorrenteRequest;
import com.reservax.reservax_api.enums.StatusReserva;
import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.AmbienteRepository;
import com.reservax.reservax_api.repository.ReservaRepository;
import com.reservax.reservax_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AmbienteRepository ambienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuario;

    @BeforeEach
    void configurarUsuario() {
        usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNome("Mario");
        usuario.setEmail("mario@reservax.com");
        usuario.setAtivo(true);

        lenient().when(usuarioRepository.findByEmailIgnoreCase("mario@reservax.com"))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void deveListarReservasAtuaisELiberarReaberturaSemConflito() {
        Reserva reserva = criarReservaCanceladaFutura();

        when(reservaRepository
                .findReservasAtuaisOuFuturasDoSolicitante(
                        any(), any(), any(), any()
                ))
                .thenReturn(List.of(reserva));
        when(reservaRepository
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());

        List<Reserva> resultado =
                reservaService.listarReservasDoSolicitante(
                        "mario@reservax.com",
                        false
                );

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isReaberturaDisponivel());
        verify(reservaRepository, never())
                .findReservasAntigasDoSolicitante(
                        any(), any(), any(), any()
                );
    }

    @Test
    void deveExpirarPendentesEncerradasAntesDeListarReservasAntigas() {
        Reserva reserva = criarReservaCanceladaFutura();
        reserva.setStatus(StatusReserva.EXPIRADA);

        when(reservaRepository.findReservasAntigasDoSolicitante(
                any(), any(), any(), any()
        )).thenReturn(List.of(reserva));

        List<Reserva> resultado =
                reservaService.listarReservasDoSolicitante(
                        "mario@reservax.com",
                        true
                );

        assertEquals(StatusReserva.EXPIRADA, resultado.get(0).getStatus());
        verify(reservaRepository).expirarReservasPendentesEncerradas(
                any(), any(), any(), any()
        );
        verify(reservaRepository, never())
                .findReservasAtuaisOuFuturasDoSolicitante(
                        any(), any(), any(), any()
                );
    }

    @Test
    void deveReabrirReservaCanceladaFuturaSemConflito() {
        Reserva reserva = criarReservaCanceladaFutura();

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.reabrir(
                1L,
                "mario@reservax.com"
        );

        assertEquals(StatusReserva.PENDENTE, resultado.getStatus());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void naoDeveReabrirReservaQuandoExisteConflito() {
        Reserva reserva = criarReservaCanceladaFutura();

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of(new Reserva()));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> reservaService.reabrir(
                        1L,
                        "mario@reservax.com"
                )
        );

        assertEquals(
                "A reserva não pode ser reaberta porque o ambiente já está ocupado neste horário.",
                erro.getMessage()
        );
        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void naoDeveReabrirReservaComDataPassada() {
        Reserva reserva = criarReservaCanceladaFutura();
        reserva.setDataReserva(LocalDate.now().minusDays(1));

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> reservaService.reabrir(
                        1L,
                        "mario@reservax.com"
                )
        );

        assertEquals(
                "Uma reserva com data ou horário já passado não pode ser reaberta.",
                erro.getMessage()
        );
        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void deveAprovarSolicitacaoPendenteENotificarUsuario() {
        Reserva reserva = criarReservaCanceladaFutura();
        reserva.setStatus(StatusReserva.PENDENTE);

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.aprovar(1L);

        assertEquals(StatusReserva.APROVADA, resultado.getStatus());
        verify(notificacaoService).notificarUsuario(
                any(), any(), any()
        );
    }

    @Test
    void naoDeveAprovarReservaDeAmbienteExcluido() {
        Reserva reserva = criarReservaCanceladaFutura();
        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.getAmbiente().setAtivo(false);
        reserva.getAmbiente().setDataExclusao(LocalDateTime.now());

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> reservaService.aprovar(1L)
        );

        assertEquals(
                "A reserva não pode ser aprovada porque o ambiente foi excluído.",
                erro.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarSolicitacaoPendenteENotificarUsuario() {
        Reserva reserva = criarReservaCanceladaFutura();
        reserva.setStatus(StatusReserva.PENDENTE);

        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.rejeitar(1L);

        assertEquals(StatusReserva.REJEITADA, resultado.getStatus());
        verify(notificacaoService).notificarUsuario(
                any(), any(), any()
        );
    }

    @Test
    void deveExcluirReservaLogicamente() {
        Reserva reserva = criarReservaCanceladaFutura();
        when(reservaRepository.findByIdAndDataExclusaoIsNull(1L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        reservaService.excluir(1L);

        assertNotNull(reserva.getDataExclusao());
        verify(reservaRepository).save(reserva);
        verify(reservaRepository, never()).deleteById(1L);
    }

    @Test
    void deveCriarReservasRecorrentesParaOsDiasSelecionados() {
        Ambiente ambiente = new Ambiente();
        ambiente.setId(10L);
        ambiente.setNome("Laboratório 1");
        ambiente.setAtivo(true);

        LocalDate dataInicio = LocalDate.now().plusDays(1);
        SolicitacaoReservaRecorrenteRequest solicitacao =
                new SolicitacaoReservaRecorrenteRequest(
                        10L,
                        dataInicio,
                        dataInicio.plusDays(7),
                        Set.of(dataInicio.getDayOfWeek()),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        "Encontro semanal"
                );

        when(ambienteRepository.findByIdAndDataExclusaoIsNull(10L))
                .thenReturn(Optional.of(ambiente));
        when(reservaRepository
                .findByDataExclusaoIsNullAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());
        when(reservaRepository.saveAll(any()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        List<Reserva> resultado = reservaService.salvarRecorrentes(
                solicitacao,
                "mario@reservax.com"
        );

        assertEquals(2, resultado.size());
        assertEquals(dataInicio, resultado.get(0).getDataReserva());
        assertEquals(dataInicio.plusDays(7), resultado.get(1).getDataReserva());
        assertTrue(resultado.stream().allMatch(
                reserva -> reserva.getStatus() == StatusReserva.PENDENTE
        ));
        verify(reservaRepository).saveAll(any());
        verify(notificacaoService).notificarAdministradores(
                any(),
                any()
        );
    }

    @Test
    void naoDeveSalvarLoteQuandoUmaOcorrenciaPossuiConflito() {
        Ambiente ambiente = new Ambiente();
        ambiente.setId(10L);
        ambiente.setAtivo(true);

        LocalDate data = LocalDate.now().plusDays(1);
        SolicitacaoReservaRecorrenteRequest solicitacao =
                new SolicitacaoReservaRecorrenteRequest(
                        10L,
                        data,
                        data,
                        Set.of(data.getDayOfWeek()),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        null
                );

        when(ambienteRepository.findByIdAndDataExclusaoIsNull(10L))
                .thenReturn(Optional.of(ambiente));
        when(reservaRepository
                .findByDataExclusaoIsNullAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of(new Reserva()));

        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> reservaService.salvarRecorrentes(
                        solicitacao,
                        "mario@reservax.com"
                )
        );

        assertTrue(erro.getMessage().contains("Já existe uma reserva"));
        verify(reservaRepository, never()).saveAll(any());
        verify(notificacaoService, never()).notificarAdministradores(
                any(),
                any()
        );
    }

    private Reserva criarReservaCanceladaFutura() {
        Ambiente ambiente = new Ambiente();
        ambiente.setId(10L);
        ambiente.setAtivo(true);

        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setAmbiente(ambiente);
        reserva.setSolicitante("Mario");
        reserva.setUsuario(usuario);
        reserva.setDataReserva(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(10, 0));
        reserva.setHoraFim(LocalTime.of(11, 0));
        reserva.setStatus(StatusReserva.CANCELADA);

        return reserva;
    }
}
