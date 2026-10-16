package com.reservax.reservax_api.service;

import com.reservax.reservax_api.dto.SolicitacaoReservaRecorrenteRequest;
import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.model.Reserva;
import com.reservax.reservax_api.model.Usuario;
import com.reservax.reservax_api.repository.AmbienteRepository;
import com.reservax.reservax_api.repository.ReservaRepository;
import com.reservax.reservax_api.repository.UsuarioRepository;
import com.reservax.reservax_api.enums.StatusReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaService {

    private static final int MAXIMO_OCORRENCIAS_POR_SOLICITACAO = 100;
    private static final int MAXIMO_DIAS_PERIODO_RECORRENTE = 366;

    private final ReservaRepository reservaRepository;
    private final AmbienteRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public ReservaService(
            ReservaRepository reservaRepository,
            AmbienteRepository ambienteRepository,
            UsuarioRepository usuarioRepository,
            NotificacaoService notificacaoService) {

        this.reservaRepository = reservaRepository;
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
    }

    public List<Reserva> listarTodas() {
        expirarReservasPendentesEncerradas(LocalDateTime.now());
        return reservaRepository.findAllByDataExclusaoIsNull();
    }

    public List<Reserva> listarReservasDoSolicitante(
            String emailUsuario,
            boolean antigas) {

        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);
        LocalDateTime agora = LocalDateTime.now();
        LocalDate dataAtual = agora.toLocalDate();
        LocalTime horaAtual = agora.toLocalTime();

        expirarReservasPendentesEncerradas(agora);

        List<Reserva> reservas = antigas
                ? reservaRepository.findReservasAntigasDoSolicitante(
                        usuario.getId(),
                        usuario.getNome(),
                        dataAtual,
                        horaAtual
                )
                : reservaRepository.findReservasAtuaisOuFuturasDoSolicitante(
                        usuario.getId(),
                        usuario.getNome(),
                        dataAtual,
                        horaAtual
                );

        reservas.forEach(reserva ->
                reserva.setReaberturaDisponivel(
                        podeReabrir(reserva, agora)
                )
        );

        return reservas;
    }

    public Optional<Reserva> buscarPorId(Long id) {
        expirarReservasPendentesEncerradas(LocalDateTime.now());
        return reservaRepository.findByIdAndDataExclusaoIsNull(id);
    }

    public Reserva salvar(Reserva reserva, String emailUsuario) {

        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        // Verifica se o ambiente existe
        Ambiente ambiente = ambienteRepository.findByIdAndDataExclusaoIsNull(
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

        LocalDateTime inicioReserva = LocalDateTime.of(
                reserva.getDataReserva(),
                reserva.getHoraInicio()
        );

        if (!inicioReserva.isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "A reserva deve possuir uma data e horário futuros."
            );
        }

        // Verifica conflito de horário
        boolean possuiConflito = !reservaRepository
            .findByDataExclusaoIsNullAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
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
        reserva.setUsuario(usuario);
        reserva.setSolicitante(usuario.getNome());
        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.setDataExclusao(null);

        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarAdministradores(
                usuario.getNome() + " criou uma nova solicitação de reserva.",
                "/gerenciar-solicitacoes?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    public List<Reserva> salvarRecorrentes(
            SolicitacaoReservaRecorrenteRequest solicitacao,
            String emailUsuario) {

        if (solicitacao == null) {
            throw new RuntimeException("Informe os dados da reserva recorrente.");
        }

        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);
        Ambiente ambiente = buscarAmbienteAtivo(solicitacao.ambienteId());
        validarSolicitacaoRecorrente(solicitacao);

        List<LocalDate> datasSelecionadas = new ArrayList<>();
        LocalDate data = solicitacao.dataInicio();

        while (!data.isAfter(solicitacao.dataFim())) {
            if (solicitacao.diasSemana().contains(data.getDayOfWeek())) {
                datasSelecionadas.add(data);
            }
            data = data.plusDays(1);
        }

        if (datasSelecionadas.isEmpty()) {
            throw new RuntimeException(
                    "O período informado não possui nenhuma ocorrência nos dias selecionados."
            );
        }

        if (datasSelecionadas.size() > MAXIMO_OCORRENCIAS_POR_SOLICITACAO) {
            throw new RuntimeException(
                    "Uma solicitação pode conter no máximo " +
                            MAXIMO_OCORRENCIAS_POR_SOLICITACAO + " reservas."
            );
        }

        LocalDateTime agora = LocalDateTime.now();
        List<Reserva> reservas = new ArrayList<>();

        for (LocalDate dataReserva : datasSelecionadas) {
            LocalDateTime inicioReserva = LocalDateTime.of(
                    dataReserva,
                    solicitacao.horaInicio()
            );

            if (!inicioReserva.isAfter(agora)) {
                throw new RuntimeException(
                        "Todas as ocorrências devem possuir data e horário futuros."
                );
            }

            if (existeConflito(
                    ambiente.getId(),
                    dataReserva,
                    solicitacao.horaInicio(),
                    solicitacao.horaFim())) {

                throw new RuntimeException(
                        "Já existe uma reserva para este ambiente em " +
                                dataReserva + " no horário informado."
                );
            }

            Reserva reserva = new Reserva();
            reserva.setAmbiente(ambiente);
            reserva.setUsuario(usuario);
            reserva.setSolicitante(usuario.getNome());
            reserva.setDataReserva(dataReserva);
            reserva.setHoraInicio(solicitacao.horaInicio());
            reserva.setHoraFim(solicitacao.horaFim());
            reserva.setObservacao(normalizarObservacao(solicitacao.observacao()));
            reserva.setStatus(StatusReserva.PENDENTE);
            reserva.setDataExclusao(null);
            reservas.add(reserva);
        }

        List<Reserva> reservasSalvas = reservaRepository.saveAll(reservas);
        String rota = reservasSalvas.isEmpty()
                ? "/gerenciar-solicitacoes"
                : "/gerenciar-solicitacoes?reservaId=" + reservasSalvas.get(0).getId();

        notificacaoService.notificarAdministradores(
                usuario.getNome() + " criou " + reservasSalvas.size() +
                        " novas solicitações de reserva recorrente.",
                rota
        );

        return reservasSalvas;
    }

    public Reserva atualizar(
            Long id,
            String emailUsuario,
            Reserva reservaAtualizada) {

        expirarReservasPendentesEncerradas(LocalDateTime.now());
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        Reserva reserva = reservaRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (!pertenceAoUsuario(reserva, usuario)) {
            throw new RuntimeException(
                    "Você não pode editar a reserva de outro usuário."
            );
        }

        if (
                reserva.getStatus() == StatusReserva.CANCELADA ||
                reserva.getStatus() == StatusReserva.REJEITADA ||
                reserva.getStatus() == StatusReserva.EXPIRADA
        ) {
            throw new RuntimeException(
                    "Esta reserva não pode ser editada."
            );
        }

        Ambiente ambiente = ambienteRepository.findByIdAndDataExclusaoIsNull(
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
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
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
        reserva.setUsuario(usuario);
        reserva.setSolicitante(usuario.getNome());
        reserva.setStatus(StatusReserva.PENDENTE);

        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarAdministradores(
                usuario.getNome() + " alterou uma solicitação de reserva.",
                "/gerenciar-solicitacoes?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    public Reserva cancelar(Long id, String emailUsuario) {
        expirarReservasPendentesEncerradas(LocalDateTime.now());
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        Reserva reserva = reservaRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (!pertenceAoUsuario(reserva, usuario)) {
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

        if (reserva.getStatus() == StatusReserva.EXPIRADA) {
            throw new RuntimeException(
                    "Uma reserva expirada não pode ser cancelada."
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

        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarAdministradores(
                usuario.getNome() + " cancelou uma reserva.",
                "/gerenciar-solicitacoes?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    public Reserva reabrir(Long id, String emailUsuario) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        Reserva reserva = reservaRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (!pertenceAoUsuario(reserva, usuario)) {
            throw new RuntimeException(
                    "Você não pode reabrir a reserva de outro usuário."
            );
        }

        if (reserva.getStatus() != StatusReserva.CANCELADA) {
            throw new RuntimeException(
                    "Somente uma reserva cancelada pode ser reaberta."
            );
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioReserva = LocalDateTime.of(
                reserva.getDataReserva(),
                reserva.getHoraInicio()
        );

        if (!inicioReserva.isAfter(agora)) {
            throw new RuntimeException(
                    "Uma reserva com data ou horário já passado não pode ser reaberta."
            );
        }

        if (!reserva.getAmbiente().getAtivo()) {
            throw new RuntimeException(
                    "Não é possível reabrir uma reserva para um ambiente inativo."
            );
        }

        if (possuiConflito(reserva)) {
            throw new RuntimeException(
                    "A reserva não pode ser reaberta porque o ambiente já está ocupado neste horário."
            );
        }

        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.setUsuario(usuario);
        reserva.setSolicitante(usuario.getNome());
        reserva.setReaberturaDisponivel(false);

        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarAdministradores(
                usuario.getNome() + " reabriu uma solicitação de reserva.",
                "/gerenciar-solicitacoes?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    public Reserva aprovar(Long id) {
        expirarReservasPendentesEncerradas(LocalDateTime.now());
        Reserva reserva = buscarReservaPendente(id);

        if (!reserva.getAmbiente().getAtivo() ||
                reserva.getAmbiente().getDataExclusao() != null) {
            throw new RuntimeException(
                    "A reserva não pode ser aprovada porque o ambiente foi excluído."
            );
        }

        if (possuiConflito(reserva)) {
            throw new RuntimeException(
                    "A reserva não pode ser aprovada porque existe conflito de horário."
            );
        }

        reserva.setStatus(StatusReserva.APROVADA);
        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarUsuario(
                obterUsuarioDaReserva(reservaSalva),
                "Sua reserva para " + reservaSalva.getAmbiente().getNome() +
                        " foi aprovada.",
                "/minhas-reservas?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    public Reserva rejeitar(Long id) {
        expirarReservasPendentesEncerradas(LocalDateTime.now());
        Reserva reserva = buscarReservaPendente(id);
        reserva.setStatus(StatusReserva.REJEITADA);

        Reserva reservaSalva = reservaRepository.save(reserva);

        notificacaoService.notificarUsuario(
                obterUsuarioDaReserva(reservaSalva),
                "Sua reserva para " + reservaSalva.getAmbiente().getNome() +
                        " foi rejeitada.",
                "/minhas-reservas?reservaId=" + reservaSalva.getId()
        );

        return reservaSalva;
    }

    private boolean podeReabrir(
            Reserva reserva,
            LocalDateTime agora) {

        if (reserva.getStatus() != StatusReserva.CANCELADA) {
            return false;
        }

        LocalDateTime inicioReserva = LocalDateTime.of(
                reserva.getDataReserva(),
                reserva.getHoraInicio()
        );

        return inicioReserva.isAfter(agora)
                && reserva.getAmbiente().getAtivo()
                && !possuiConflito(reserva);
    }

    private boolean possuiConflito(Reserva reserva) {
        return !reservaRepository
                .findByDataExclusaoIsNullAndIdNotAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        reserva.getId(),
                        reserva.getAmbiente().getId(),
                        reserva.getDataReserva(),
                        List.of(
                                StatusReserva.PENDENTE,
                                StatusReserva.APROVADA
                        ),
                        reserva.getHoraFim(),
                        reserva.getHoraInicio()
                )
                .isEmpty();
    }

    private boolean existeConflito(
            Long ambienteId,
            LocalDate dataReserva,
            LocalTime horaInicio,
            LocalTime horaFim) {

        return !reservaRepository
                .findByDataExclusaoIsNullAndAmbienteIdAndDataReservaAndStatusInAndHoraInicioLessThanAndHoraFimGreaterThan(
                        ambienteId,
                        dataReserva,
                        List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA),
                        horaFim,
                        horaInicio
                )
                .isEmpty();
    }

    private Ambiente buscarAmbienteAtivo(Long ambienteId) {
        if (ambienteId == null) {
            throw new RuntimeException("Selecione um ambiente.");
        }

        Ambiente ambiente = ambienteRepository
                .findByIdAndDataExclusaoIsNull(ambienteId)
                .orElseThrow(() ->
                        new RuntimeException("Ambiente não encontrado.")
                );

        if (!ambiente.getAtivo()) {
            throw new RuntimeException(
                    "Não é possível reservar um ambiente inativo."
            );
        }

        return ambiente;
    }

    private void validarSolicitacaoRecorrente(
            SolicitacaoReservaRecorrenteRequest solicitacao) {

        if (solicitacao.dataInicio() == null || solicitacao.dataFim() == null) {
            throw new RuntimeException("Informe as datas inicial e final.");
        }

        if (solicitacao.dataFim().isBefore(solicitacao.dataInicio())) {
            throw new RuntimeException(
                    "A data final deve ser igual ou posterior à data inicial."
            );
        }

        if (ChronoUnit.DAYS.between(
                solicitacao.dataInicio(),
                solicitacao.dataFim()) > MAXIMO_DIAS_PERIODO_RECORRENTE) {

            throw new RuntimeException(
                    "O período da reserva recorrente não pode ultrapassar um ano."
            );
        }

        if (solicitacao.diasSemana() == null ||
                solicitacao.diasSemana().isEmpty()) {
            throw new RuntimeException("Selecione pelo menos um dia da semana.");
        }

        if (solicitacao.horaInicio() == null || solicitacao.horaFim() == null) {
            throw new RuntimeException("Informe os horários de início e término.");
        }

        if (!solicitacao.horaInicio().isBefore(solicitacao.horaFim())) {
            throw new RuntimeException(
                    "A hora de início deve ser menor que a hora de término."
            );
        }

        if (solicitacao.observacao() != null &&
                solicitacao.observacao().length() > 500) {
            throw new RuntimeException(
                    "A observação deve possuir no máximo 500 caracteres."
            );
        }
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }

    private void expirarReservasPendentesEncerradas(
            LocalDateTime agora) {

        reservaRepository.expirarReservasPendentesEncerradas(
                StatusReserva.PENDENTE,
                StatusReserva.EXPIRADA,
                agora.toLocalDate(),
                agora.toLocalTime()
        );
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new RuntimeException("Usuário autenticado não encontrado.")
                );
    }

    private boolean pertenceAoUsuario(
            Reserva reserva,
            Usuario usuario) {

        if (reserva.getUsuario() != null) {
            return reserva.getUsuario().getId().equals(usuario.getId());
        }

        return reserva.getSolicitante()
                .equalsIgnoreCase(usuario.getNome());
    }

    private Reserva buscarReservaPendente(Long id) {
        Reserva reserva = reservaRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RuntimeException(
                    "Somente uma solicitação pendente pode ser analisada."
            );
        }

        return reserva;
    }

    private Usuario obterUsuarioDaReserva(Reserva reserva) {
        if (reserva.getUsuario() != null) {
            return reserva.getUsuario();
        }

        return usuarioRepository
                .findFirstByNomeIgnoreCase(reserva.getSolicitante())
                .orElse(null);
    }

    public void excluir(Long id) {
        Reserva reserva = reservaRepository.findByIdAndDataExclusaoIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Reserva não encontrada.")
                );

        reserva.setDataExclusao(LocalDateTime.now());
        Reserva reservaExcluida = reservaRepository.save(reserva);

        notificacaoService.notificarUsuario(
                obterUsuarioDaReserva(reservaExcluida),
                "Sua reserva para " + reservaExcluida.getAmbiente().getNome() +
                        " foi removida pelo administrador.",
                "/minhas-reservas"
        );
    }
}
