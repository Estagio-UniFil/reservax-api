const API_AMBIENTES = "/ambientes";
const API_RESERVAS = "/reservas";

const ambienteSelect = document.getElementById("ambienteReserva");
const infoAmbiente = document.getElementById("infoAmbiente");
const formReserva = document.getElementById("formReserva");
const mensagemReserva = document.getElementById("mensagemReserva");
const dataReserva = document.getElementById("dataReserva");
const dataFimRecorrencia = document.getElementById("dataFimRecorrencia");
const camposRecorrencia = document.getElementById("camposRecorrencia");
const resumoRecorrencia = document.getElementById("resumoRecorrencia");
const rotuloDataReserva = document.getElementById("rotuloDataReserva");
const botaoSolicitar = formReserva.querySelector("button[type='submit']");
const checkboxesDiasSemana = Array.from(
    document.querySelectorAll("#grupoDiasSemana input[type='checkbox']")
);

let ambientes = [];
let nomeSolicitante = "";

/*
    Carrega os ambientes ao abrir a página
*/
document.addEventListener("DOMContentLoaded", () => {
    carregarSolicitante();
    carregarAmbientes();
    configurarDatasMinimas();
    configurarRecorrencia();
});

/*
    Busca os ambientes cadastrados
*/
async function carregarAmbientes() {
    try {

        const response = await fetch(API_AMBIENTES);

        if (!response.ok) {
            throw new Error("Não foi possível carregar os ambientes.");
        }

        const dados = await response.json();
        ambientes = Array.isArray(dados) ? dados : [];

        ambienteSelect.innerHTML =
            '<option value="">Selecione um ambiente</option>';

        ambientes.forEach(ambiente => {

            if (ambiente.ativo) {

                ambienteSelect.innerHTML += `
                    <option value="${ambiente.id}">
                        ${ambiente.nome}
                    </option>
                `;
            }
        });

    } catch (error) {

        exibirMensagem(
            "Erro ao carregar os ambientes.",
            "danger"
        );

        console.error(error);
    }
}

/*
    Atualiza as informações do ambiente selecionado
*/
ambienteSelect.addEventListener("change", () => {

    const ambienteId = Number(ambienteSelect.value);

    const ambiente = ambientes.find(a => a.id === ambienteId);

    if (!ambiente) {

        infoAmbiente.innerHTML = `
            <strong>Capacidade:</strong> -
            <br>
            <strong>Localização:</strong> -
            <br>
            <strong>Recursos:</strong> -
        `;

        return;
    }

    const recursos = [];

    if (ambiente.possuiProjetor) recursos.push("Projetor");
    if (ambiente.possuiTv) recursos.push("TV");
    if (ambiente.possuiMicrofone) recursos.push("Microfone");
    if (ambiente.possuiComputadores) recursos.push("Computadores");

    infoAmbiente.innerHTML = `
        <strong>Capacidade:</strong> ${ambiente.capacidade} pessoas
        <br>
        <strong>Localização:</strong> ${ambiente.localizacao || "-"}
        <br>
        <strong>Recursos:</strong> ${
            recursos.length > 0
                ? recursos.join(", ")
                : "Nenhum"
        }
    `;
});

/*
    Salva a reserva
*/
formReserva.addEventListener("submit", async (event) => {

    event.preventDefault();

    const recorrente = obterTipoSolicitacao() === "RECORRENTE";
    const ambienteId = Number(ambienteSelect.value);
    const horaInicio = document.getElementById("horaInicioReserva").value;
    const horaFim = document.getElementById("horaFimReserva").value;
    const observacao = document.getElementById("observacaoReserva").value;

    let endpoint = API_RESERVAS;
    let reserva;

    if (recorrente) {
        const diasSemana = obterDiasSelecionados();

        if (diasSemana.length === 0) {
            exibirMensagem("Selecione pelo menos um dia da semana.", "danger");
            document.getElementById("grupoDiasSemana").focus();
            return;
        }

        endpoint = `${API_RESERVAS}/lote`;
        reserva = {
            ambienteId,
            dataInicio: dataReserva.value,
            dataFim: dataFimRecorrencia.value,
            diasSemana,
            horaInicio: `${horaInicio}:00`,
            horaFim: `${horaFim}:00`,
            observacao
        };
    } else {
        reserva = {

            ambiente: {
                id: ambienteId
            },

            dataReserva: dataReserva.value,

            horaInicio: `${horaInicio}:00`,

            horaFim: `${horaFim}:00`,

            observacao
        };
    }

    const textoOriginalBotao = botaoSolicitar.innerText;
    botaoSolicitar.disabled = true;
    botaoSolicitar.innerText = recorrente
        ? "Solicitando reservas..."
        : "Solicitando...";

    try {

        const response = await fetchComCsrf(endpoint, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(reserva)
        });

        if (!response.ok) {
            throw new Error(await obterMensagemErro(response));
        }

        const resultado = await response.json();
        const quantidade = recorrente ? resultado.quantidade : 1;

        exibirMensagem(
            quantidade === 1
                ? "Reserva solicitada com sucesso!"
                : `${quantidade} reservas foram solicitadas com sucesso!`,
            "success"
        );

        formReserva.reset();
        restaurarFormulario();

    } catch (error) {

        exibirMensagem(
            error.message || "Não foi possível realizar a reserva.",
            "danger"
        );

        console.error(error);
    } finally {
        botaoSolicitar.disabled = false;
        botaoSolicitar.innerText = textoOriginalBotao;
    }
});

function configurarDatasMinimas() {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, "0");
    const dia = String(hoje.getDate()).padStart(2, "0");
    const dataMinima = `${ano}-${mes}-${dia}`;

    dataReserva.min = dataMinima;
    dataFimRecorrencia.min = dataMinima;
}

function configurarRecorrencia() {
    document.querySelectorAll("input[name='tipoSolicitacao']")
        .forEach(opcao => opcao.addEventListener("change", atualizarModoReserva));

    dataReserva.addEventListener("change", () => {
        dataFimRecorrencia.min = dataReserva.value || dataReserva.min;
        if (dataFimRecorrencia.value &&
                dataFimRecorrencia.value < dataFimRecorrencia.min) {
            dataFimRecorrencia.value = "";
        }
        atualizarResumoRecorrencia();
    });

    dataFimRecorrencia.addEventListener("change", atualizarResumoRecorrencia);
    checkboxesDiasSemana.forEach(checkbox =>
        checkbox.addEventListener("change", atualizarResumoRecorrencia)
    );

    formReserva.addEventListener("reset", () => {
        window.setTimeout(restaurarFormulario, 0);
    });
}

function atualizarModoReserva() {
    const recorrente = obterTipoSolicitacao() === "RECORRENTE";
    camposRecorrencia.hidden = !recorrente;
    dataFimRecorrencia.required = recorrente;
    rotuloDataReserva.innerText = recorrente ? "Data inicial" : "Data";
    atualizarResumoRecorrencia();
}

function obterTipoSolicitacao() {
    return document.querySelector("input[name='tipoSolicitacao']:checked").value;
}

function obterDiasSelecionados() {
    return checkboxesDiasSemana
        .filter(checkbox => checkbox.checked)
        .map(checkbox => checkbox.value);
}

function atualizarResumoRecorrencia() {
    if (obterTipoSolicitacao() !== "RECORRENTE") {
        return;
    }

    const datas = calcularDatasRecorrentes(
        dataReserva.value,
        dataFimRecorrencia.value,
        obterDiasSelecionados()
    );

    if (datas.length === 0) {
        resumoRecorrencia.innerText = "Selecione o período e os dias da semana.";
        return;
    }

    resumoRecorrencia.innerText = datas.length === 1
        ? "Será criada 1 solicitação de reserva."
        : `Serão criadas ${datas.length} solicitações de reserva.`;
}

function calcularDatasRecorrentes(dataInicio, dataFim, diasSemana) {
    if (!dataInicio || !dataFim || diasSemana.length === 0 || dataFim < dataInicio) {
        return [];
    }

    const mapaDias = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    const selecionados = new Set(diasSemana);
    const atual = new Date(`${dataInicio}T12:00:00`);
    const limite = new Date(`${dataFim}T12:00:00`);
    const datas = [];

    while (atual <= limite && datas.length <= 100) {
        if (selecionados.has(mapaDias[atual.getDay()])) {
            datas.push(new Date(atual));
        }
        atual.setDate(atual.getDate() + 1);
    }

    return datas;
}

function restaurarFormulario() {
    camposRecorrencia.hidden = true;
    dataFimRecorrencia.required = false;
    dataFimRecorrencia.min = dataReserva.min;
    rotuloDataReserva.innerText = "Data";
    resumoRecorrencia.innerText = "Selecione o período e os dias da semana.";
    document.getElementById("solicitanteReserva").value = nomeSolicitante;
    infoAmbiente.innerHTML = `
        <strong>Capacidade:</strong> -
        <br>
        <strong>Localização:</strong> -
        <br>
        <strong>Recursos:</strong> -
    `;
}

async function obterMensagemErro(response) {
    try {
        const dados = await response.json();
        return dados.mensagem || "Não foi possível realizar a reserva.";
    } catch (error) {
        return "Não foi possível realizar a reserva.";
    }
}

/*
    Exibe mensagens na tela
*/
function exibirMensagem(texto, tipo) {

    mensagemReserva.innerHTML = `
        <div class="alert alert-${tipo}">
            ${texto}
        </div>
    `;

    setTimeout(() => {

        mensagemReserva.innerHTML = "";

    }, 5000);
}

async function carregarSolicitante() {
    try {
        const resposta = await fetch("/auth/me");
        const usuario = await resposta.json();

        if (resposta.ok) {
            nomeSolicitante = usuario.nome;
            document.getElementById("solicitanteReserva").value = nomeSolicitante;
        }
    } catch (erro) {
        console.error(erro);
    }
}
