const STATUS_ATIVOS = ["PENDENTE", "APROVADA"];
const LIMITE_PROXIMAS_RESERVAS = 5;

const mensagemDashboard = document.getElementById("mensagemDashboard");
const listaProximasReservas = document.getElementById("listaProximasReservas");
const tituloCalendario = document.getElementById("tituloCalendario");
const diasCalendario = document.getElementById("diasCalendario");

let mesExibido = inicioDoMes(new Date());
let dataAtualConhecida = chaveData(new Date());

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("mesAnterior").addEventListener("click", function () {
        mesExibido.setMonth(mesExibido.getMonth() - 1);
        renderizarCalendario();
    });

    document.getElementById("proximoMes").addEventListener("click", function () {
        mesExibido.setMonth(mesExibido.getMonth() + 1);
        renderizarCalendario();
    });

    renderizarCalendario();
    carregarDashboard();

    setInterval(atualizarDadosECalendario, 60000);
});

async function carregarDashboard() {
    try {
        const respostaUsuario = await fetch("/auth/me");

        if (!respostaUsuario.ok) {
            throw new Error("Não foi possível identificar o usuário.");
        }

        const usuario = await respostaUsuario.json();
        const endpointReservas = usuario.tipoUsuario === "ADMIN"
            ? "/reservas"
            : "/reservas/minhas";
        document.getElementById("linkVerTodasReservas").href =
            usuario.tipoUsuario === "ADMIN"
                ? "/gerenciar-solicitacoes"
                : "/minhas-reservas";

        const [respostaAmbientes, respostaReservas] = await Promise.all([
            fetch("/ambientes"),
            fetch(endpointReservas)
        ]);

        if (!respostaAmbientes.ok || !respostaReservas.ok) {
            throw new Error("Não foi possível carregar os dados do dashboard.");
        }

        const ambientes = await respostaAmbientes.json();
        const reservas = await respostaReservas.json();

        atualizarIndicadores(ambientes, reservas);
        renderizarProximasReservas(reservas);
        mensagemDashboard.innerHTML = "";
    } catch (erro) {
        console.error(erro);
        mensagemDashboard.innerHTML = `
            <div class="alert alert-danger">
                Não foi possível carregar os dados do dashboard.
            </div>
        `;
    }
}

function atualizarIndicadores(ambientes, reservas) {
    const agora = new Date();
    const hoje = chaveData(agora);
    const ambientesAtivos = ambientes.filter(ambiente => ambiente.ativo);

    const reservasValidasHoje = reservas.filter(reserva =>
        reserva.dataReserva === hoje && STATUS_ATIVOS.includes(reserva.status)
    );

    const ambientesOcupadosHoje = new Set(
        reservasValidasHoje.map(reserva => reserva.ambiente.id)
    );

    const totalDisponiveis = ambientesAtivos.filter(
        ambiente => !ambientesOcupadosHoje.has(ambiente.id)
    ).length;

    const totalPendentes = reservas.filter(reserva =>
        reserva.status === "PENDENTE" && dataHoraFim(reserva) > agora
    ).length;

    document.getElementById("totalAmbientesDisponiveis").innerText =
        totalDisponiveis;
    document.getElementById("totalReservasHoje").innerText =
        reservasValidasHoje.length;
    document.getElementById("totalPendentes").innerText =
        totalPendentes;
    document.getElementById("totalConflitos").innerText =
        contarConflitos(reservas);
}

function renderizarProximasReservas(reservas) {
    const agora = new Date();

    const proximasReservas = reservas
        .filter(reserva =>
            STATUS_ATIVOS.includes(reserva.status) &&
            dataHoraFim(reserva) > agora
        )
        .sort((reservaA, reservaB) =>
            dataHoraInicio(reservaA) - dataHoraInicio(reservaB)
        )
        .slice(0, LIMITE_PROXIMAS_RESERVAS);

    listaProximasReservas.innerHTML = "";

    if (proximasReservas.length === 0) {
        listaProximasReservas.innerHTML = `
            <tr>
                <td colspan="5" class="lista-vazia">
                    Nenhuma reserva atual ou futura encontrada.
                </td>
            </tr>
        `;
        return;
    }

    proximasReservas.forEach(reserva => {
        const linha = document.createElement("tr");

        linha.innerHTML = `
            <td>${escaparHtml(reserva.ambiente.nome)}</td>
            <td>${formatarData(reserva.dataReserva)}</td>
            <td>${formatarHorario(reserva)}</td>
            <td>${escaparHtml(reserva.solicitante)}</td>
            <td>
                <span class="status-reserva status-${reserva.status.toLowerCase()}">
                    ${formatarStatus(reserva.status)}
                </span>
            </td>
        `;

        listaProximasReservas.appendChild(linha);
    });
}

function contarConflitos(reservas) {
    const agora = new Date();
    const reservasAtivas = reservas.filter(reserva =>
        STATUS_ATIVOS.includes(reserva.status) &&
        dataHoraFim(reserva) > agora
    );
    let conflitos = 0;

    for (let indice = 0; indice < reservasAtivas.length; indice++) {
        for (
            let outroIndice = indice + 1;
            outroIndice < reservasAtivas.length;
            outroIndice++
        ) {
            const reservaA = reservasAtivas[indice];
            const reservaB = reservasAtivas[outroIndice];

            const mesmoAmbiente =
                reservaA.ambiente.id === reservaB.ambiente.id;
            const mesmaData =
                reservaA.dataReserva === reservaB.dataReserva;
            const horariosSobrepostos =
                reservaA.horaInicio < reservaB.horaFim &&
                reservaA.horaFim > reservaB.horaInicio;

            if (mesmoAmbiente && mesmaData && horariosSobrepostos) {
                conflitos++;
            }
        }
    }

    return conflitos;
}

function renderizarCalendario() {
    const hoje = new Date();
    const ano = mesExibido.getFullYear();
    const mes = mesExibido.getMonth();

    tituloCalendario.innerText = new Intl.DateTimeFormat("pt-BR", {
        month: "long",
        year: "numeric"
    }).format(mesExibido);

    diasCalendario.innerHTML = "";

    const primeiroDia = new Date(ano, mes, 1);
    const inicioGrade = new Date(ano, mes, 1 - primeiroDia.getDay());

    for (let indice = 0; indice < 42; indice++) {
        const data = new Date(inicioGrade);
        data.setDate(inicioGrade.getDate() + indice);

        const dia = document.createElement("span");
        dia.classList.add("dia-calendario");
        dia.innerText = data.getDate();

        if (data.getMonth() !== mes) {
            dia.classList.add("outro-mes");
        }

        if (data.getDay() === 0 || data.getDay() === 6) {
            dia.classList.add("fim-semana");
        }

        if (chaveData(data) === chaveData(hoje)) {
            dia.classList.add("hoje");
            dia.setAttribute("aria-current", "date");
        }

        diasCalendario.appendChild(dia);
    }
}

function atualizarDadosECalendario() {
    const novaDataAtual = chaveData(new Date());

    if (novaDataAtual !== dataAtualConhecida) {
        dataAtualConhecida = novaDataAtual;
        mesExibido = inicioDoMes(new Date());
    }

    renderizarCalendario();
    carregarDashboard();
}

function inicioDoMes(data) {
    return new Date(data.getFullYear(), data.getMonth(), 1);
}

function chaveData(data) {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, "0");
    const dia = String(data.getDate()).padStart(2, "0");
    return `${ano}-${mes}-${dia}`;
}

function dataHoraInicio(reserva) {
    return new Date(`${reserva.dataReserva}T${reserva.horaInicio}`);
}

function dataHoraFim(reserva) {
    return new Date(`${reserva.dataReserva}T${reserva.horaFim}`);
}

function formatarData(data) {
    const [ano, mes, dia] = data.split("-");
    return `${dia}/${mes}/${ano}`;
}

function formatarHorario(reserva) {
    return `${reserva.horaInicio.substring(0, 5)} - ${reserva.horaFim.substring(0, 5)}`;
}

function formatarStatus(status) {
    const nomes = {
        PENDENTE: "Pendente",
        APROVADA: "Aprovada",
        REJEITADA: "Rejeitada",
        CANCELADA: "Cancelada",
        EXPIRADA: "Expirada"
    };

    return nomes[status] || status;
}

function escaparHtml(valor) {
    const elemento = document.createElement("div");
    elemento.innerText = valor || "-";
    return elemento.innerHTML;
}
