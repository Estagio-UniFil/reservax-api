const listaSolicitacoes = document.getElementById("listaSolicitacoes");
const mensagemSolicitacoes = document.getElementById("mensagemSolicitacoes");

document.addEventListener("DOMContentLoaded", carregarSolicitacoes);

async function carregarSolicitacoes() {
    try {
        const resposta = await fetch("/reservas");

        if (!resposta.ok) {
            throw new Error("Não foi possível carregar as solicitações.");
        }

        const agora = new Date();
        const reservas = (await resposta.json())
            .filter(reserva => dataHoraFimSolicitacao(reserva) > agora)
            .sort((a, b) =>
                dataHoraInicioSolicitacao(a) - dataHoraInicioSolicitacao(b)
            );

        renderizarSolicitacoes(reservas);
    } catch (erro) {
        exibirMensagemSolicitacoes(erro.message, "danger");
    }
}

function renderizarSolicitacoes(reservas) {
    listaSolicitacoes.innerHTML = "";

    if (reservas.length === 0) {
        listaSolicitacoes.innerHTML = `
            <tr><td colspan="6" class="lista-vazia">Nenhuma solicitação atual ou futura.</td></tr>
        `;
        return;
    }

    const reservaDestacada = Number(
        new URLSearchParams(window.location.search).get("reservaId")
    );

    reservas.forEach(reserva => {
        const linha = document.createElement("tr");
        linha.dataset.reservaId = reserva.id;

        if (reserva.id === reservaDestacada) {
            linha.classList.add("linha-destacada");
        }

        linha.innerHTML = `
            <td>${escaparTextoSolicitacao(reserva.ambiente.nome)}</td>
            <td>${formatarDataSolicitacao(reserva.dataReserva)}</td>
            <td>${reserva.horaInicio.substring(0, 5)} - ${reserva.horaFim.substring(0, 5)}</td>
            <td>${escaparTextoSolicitacao(reserva.solicitante)}</td>
            <td>
                <span class="status-reserva status-${reserva.status.toLowerCase()}">
                    ${formatarStatusSolicitacao(reserva.status)}
                </span>
            </td>
            <td>${criarAcoesSolicitacao(reserva)}</td>
        `;

        listaSolicitacoes.appendChild(linha);
    });

    const linhaDestacada = document.querySelector(".linha-destacada");
    linhaDestacada?.scrollIntoView({ behavior: "smooth", block: "center" });
}

function criarAcoesSolicitacao(reserva) {
    if (reserva.status !== "PENDENTE") {
        return "-";
    }

    return `
        <div class="acoes-solicitacao">
            <button class="btn-aprovar" type="button" onclick="analisarSolicitacao(${reserva.id}, 'aprovar')">
                Aprovar
            </button>
            <button class="btn-rejeitar" type="button" onclick="analisarSolicitacao(${reserva.id}, 'rejeitar')">
                Rejeitar
            </button>
        </div>
    `;
}

async function analisarSolicitacao(id, acao) {
    const confirmou = confirm(
        `Deseja realmente ${acao} esta solicitação?`
    );

    if (!confirmou) {
        return;
    }

    try {
        const resposta = await fetchComCsrf(`/reservas/${id}/${acao}`, {
            method: "PATCH"
        });

        if (!resposta.ok) {
            throw new Error(await obterErroSolicitacao(resposta));
        }

        exibirMensagemSolicitacoes(
            `Solicitação ${acao === "aprovar" ? "aprovada" : "rejeitada"} com sucesso.`,
            "success"
        );
        await carregarSolicitacoes();
    } catch (erro) {
        exibirMensagemSolicitacoes(erro.message, "danger");
    }
}

async function obterErroSolicitacao(resposta) {
    try {
        const dados = await resposta.json();
        return dados.mensagem || "Não foi possível analisar a solicitação.";
    } catch (erro) {
        return "Não foi possível analisar a solicitação.";
    }
}

function exibirMensagemSolicitacoes(texto, tipo) {
    mensagemSolicitacoes.innerHTML = `<div class="alert alert-${tipo}">${texto}</div>`;
}

function dataHoraInicioSolicitacao(reserva) {
    return new Date(`${reserva.dataReserva}T${reserva.horaInicio}`);
}

function dataHoraFimSolicitacao(reserva) {
    return new Date(`${reserva.dataReserva}T${reserva.horaFim}`);
}

function formatarDataSolicitacao(data) {
    const [ano, mes, dia] = data.split("-");
    return `${dia}/${mes}/${ano}`;
}

function formatarStatusSolicitacao(status) {
    const nomes = {
        PENDENTE: "Pendente",
        APROVADA: "Aprovada",
        REJEITADA: "Rejeitada",
        CANCELADA: "Cancelada",
        EXPIRADA: "Expirada"
    };
    return nomes[status] || status;
}

function escaparTextoSolicitacao(valor) {
    const elemento = document.createElement("div");
    elemento.innerText = valor || "-";
    return elemento.innerHTML;
}
