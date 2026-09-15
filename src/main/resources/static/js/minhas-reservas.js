const listaMinhasReservas =
    document.getElementById("listaMinhasReservas");

const mensagemReservas = 
    document.getElementById("mensagemReservas");

const formEditarReserva =
    document.getElementById("formEditarReserva");

const filtroPeriodo =
    document.getElementById("filtroPeriodo");

const descricaoFiltro =
    document.getElementById("descricaoFiltro");

let reservasAtuais = [];
let ambientesAtivos = [];

document.addEventListener("DOMContentLoaded", function () {
    filtroPeriodo.addEventListener("change", function () {
        atualizarDescricaoFiltro();
        carregarMinhasReservas();
    });

    carregarDadosIniciais();
});

async function carregarDadosIniciais() {
    await Promise.all([
        carregarAmbientes(),
        carregarMinhasReservas()
    ]);
}

async function carregarMinhasReservas() {
    try {
        const antigas = filtroPeriodo.value === "antigas";
        const url = `/reservas/minhas?antigas=${antigas}`;

        const resposta = await fetch(url);

        if (!resposta.ok) {
            throw new Error("Não foi possível carregar as reservas.");
        }

        reservasAtuais = await resposta.json();
        renderizarReservas(reservasAtuais, antigas);
    } catch (erro) {
        console.error(erro);

        mensagemReservas.innerText = "Não foi possível carregar suas reservas.";
    }
}

function renderizarReservas(reservas, antigas) {
    listaMinhasReservas.innerHTML = "";

    if (reservas.length === 0) {
        listaMinhasReservas.innerHTML = `
            <tr>
                <td colspan="5">
                    ${antigas
                        ? "Nenhuma reserva antiga encontrada."
                        : "Nenhuma reserva atual ou futura encontrada."}
                </td>
            </tr>
        `;

        return;
    }

    reservas.forEach(function (reserva) {
        const linha = document.createElement("tr");
        const reservaDestacada = Number(
            new URLSearchParams(window.location.search).get("reservaId")
        );

        if (reserva.id === reservaDestacada) {
            linha.classList.add("linha-reserva-destacada");
        }

        linha.innerHTML = `
            <td>${escaparHtmlReserva(reserva.ambiente.nome)}</td>
            <td>${formatarData(reserva.dataReserva)}</td>
            <td>
                ${reserva.horaInicio.substring(0, 5)}
                -
                ${reserva.horaFim.substring(0, 5)}
            </td>
            <td>
                <span class="status-reserva status-${reserva.status.toLowerCase()}">
                    ${formatarStatus(reserva.status)}
                </span>
            </td>
            <td>
                ${criarAcoesReserva(reserva)}
            </td>
        `;

        listaMinhasReservas.appendChild(linha);
    });

    const linhaDestacada = document.querySelector(
        ".linha-reserva-destacada"
    );
    linhaDestacada?.scrollIntoView({ behavior: "smooth", block: "center" });
}

function criarAcoesReserva(reserva) {
    const inicioReserva = new Date(
        `${reserva.dataReserva}T${reserva.horaInicio}`
    );

    const reservaJaPassou = inicioReserva.getTime() <= Date.now();

    if (reserva.status === "CANCELADA") {
        if (reserva.reaberturaDisponivel && !reservaJaPassou) {
            return `
                <div class="botoes-acoes">
                    <button
                        type="button"
                        class="btn-reabrir"
                        onclick="reabrirReserva(${reserva.id})">
                        Reabrir
                    </button>
                </div>
            `;
        }

        return "-";
    }

    if (
        reserva.status === "REJEITADA" ||
        reserva.status === "EXPIRADA" ||
        reservaJaPassou
    ) {
        return "-";
    }

    return `
        <div class="botoes-acoes">
            <button
                type="button"
                class="btn-editar"
                onclick="abrirEdicaoReserva(${reserva.id})">
                Editar
            </button>
            <button
                type="button"
                class="btn-cancelar"
                onclick="cancelarReserva(${reserva.id})">
                Cancelar
            </button>
        </div>
    `;
}

async function carregarAmbientes() {
    try {
        const resposta = await fetch("/ambientes");

        if (!resposta.ok) {
            throw new Error("Não foi possível carregar os ambientes.");
        }

        const ambientes = await resposta.json();
        ambientesAtivos = ambientes.filter(ambiente => ambiente.ativo);

        const select = document.getElementById("ambienteEdicao");
        select.innerHTML = '<option value="">Selecione um ambiente</option>';

        ambientesAtivos.forEach(ambiente => {
            const option = document.createElement("option");
            option.value = ambiente.id;
            option.textContent = ambiente.nome;
            select.appendChild(option);
        });
    } catch (erro) {
        console.error(erro);
        exibirMensagem("Não foi possível carregar os ambientes.", "danger");
    }
}

function abrirEdicaoReserva(id) {
    const reserva = reservasAtuais.find(item => item.id === id);

    if (!reserva) {
        exibirMensagem("Reserva não encontrada na listagem.", "danger");
        return;
    }

    document.getElementById("reservaEdicaoId").value = reserva.id;
    document.getElementById("ambienteEdicao").value = reserva.ambiente.id;
    document.getElementById("dataEdicao").value = reserva.dataReserva;
    document.getElementById("horaInicioEdicao").value = reserva.horaInicio.substring(0, 5);
    document.getElementById("horaFimEdicao").value = reserva.horaFim.substring(0, 5);
    document.getElementById("observacaoEdicao").value = reserva.observacao || "";

    const modal = new bootstrap.Modal(
        document.getElementById("modalEditarReserva")
    );
    modal.show();
}

formEditarReserva.addEventListener("submit", async function (event) {
    event.preventDefault();

    const id = document.getElementById("reservaEdicaoId").value;
    const horaInicio = document.getElementById("horaInicioEdicao").value;
    const horaFim = document.getElementById("horaFimEdicao").value;

    if (horaInicio >= horaFim) {
        exibirMensagem(
            "O horário de início deve ser anterior ao horário de término.",
            "danger"
        );
        return;
    }

    const reservaAtualizada = {
        ambiente: {
            id: Number(document.getElementById("ambienteEdicao").value)
        },
        dataReserva: document.getElementById("dataEdicao").value,
        horaInicio: `${horaInicio}:00`,
        horaFim: `${horaFim}:00`,
        observacao: document.getElementById("observacaoEdicao").value
    };

    try {
        const url = `/reservas/${id}`;

        const resposta = await fetchComCsrf(url, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(reservaAtualizada)
        });

        if (!resposta.ok) {
            throw new Error(await obterMensagemErro(resposta));
        }

        const modal = bootstrap.Modal.getInstance(
            document.getElementById("modalEditarReserva")
        );
        modal.hide();

        exibirMensagem("Reserva atualizada com sucesso!", "success");
        await carregarMinhasReservas();
    } catch (erro) {
        console.error(erro);
        exibirMensagem(erro.message, "danger");
    }
});

async function cancelarReserva(id) {
    const confirmou = confirm(
        "Tem certeza que deseja cancelar esta reserva?"
    );

    if (!confirmou) {
        return;
    }

    try {
        const url = `/reservas/${id}/cancelar`;

        const resposta = await fetchComCsrf(url, {
            method: "PATCH"
        });

        if (!resposta.ok) {
            throw new Error(await obterMensagemErro(resposta));
        }

        exibirMensagem("Reserva cancelada com sucesso!", "success");

        await carregarMinhasReservas();

    } catch (erro) {
        console.error(erro);

        exibirMensagem(erro.message, "danger");
    }
}

async function reabrirReserva(id) {
    const confirmou = confirm(
        "Deseja reabrir esta solicitação de reserva?"
    );

    if (!confirmou) {
        return;
    }

    try {
        const url = `/reservas/${id}/reabrir`;

        const resposta = await fetchComCsrf(url, {
            method: "PATCH"
        });

        if (!resposta.ok) {
            throw new Error(await obterMensagemErro(resposta));
        }

        exibirMensagem(
            "Solicitação reaberta com sucesso e enviada para análise!",
            "success"
        );

        await carregarMinhasReservas();
    } catch (erro) {
        console.error(erro);
        exibirMensagem(erro.message, "danger");

        await carregarMinhasReservas();
    }
}

function atualizarDescricaoFiltro() {
    descricaoFiltro.innerText = filtroPeriodo.value === "antigas"
        ? "Reservas cujo horário de término já passou."
        : "Reservas em andamento ou que ainda vão acontecer.";
}

async function obterMensagemErro(resposta) {
    try {
        const dados = await resposta.json();
        return dados.mensagem || "Não foi possível concluir a operação.";
    } catch (erro) {
        return "Não foi possível concluir a operação.";
    }
}

function exibirMensagem(texto, tipo) {
    mensagemReservas.innerHTML = `
        <div class="alert alert-${tipo}">
            ${texto}
        </div>
    `;
}

function formatarData(data) {
    const partes = data.split("-");

    return `${partes[2]}/${partes[1]}/${partes[0]}`;
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

function escaparHtmlReserva(valor) {
    const elemento = document.createElement("div");
    elemento.innerText = valor || "-";
    return elemento.innerHTML;
}
