const API_AMBIENTES = "/ambientes";
const API_RESERVAS = "/reservas";

const ambienteSelect = document.getElementById("ambienteReserva");
const infoAmbiente = document.getElementById("infoAmbiente");
const formReserva = document.getElementById("formReserva");
const mensagemReserva = document.getElementById("mensagemReserva");

let ambientes = [];

/*
    Carrega os ambientes ao abrir a página
*/
document.addEventListener("DOMContentLoaded", () => {
    carregarAmbientes();
});

/*
    Busca os ambientes cadastrados
*/
async function carregarAmbientes() {

    try {

        const response = await fetch(API_AMBIENTES);
        ambientes = await response.json();

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

    const reserva = {

        ambiente: {
            id: Number(
                document.getElementById("ambienteReserva").value
            )
        },

        solicitante: document
            .getElementById("solicitanteReserva")
            .value,

        dataReserva: document
            .getElementById("dataReserva")
            .value,

        horaInicio: document
            .getElementById("horaInicioReserva")
            .value + ":00",

        horaFim: document
            .getElementById("horaFimReserva")
            .value + ":00",

        observacao: document
            .getElementById("observacaoReserva")
            .value
    };

    try {

        const response = await fetch(API_RESERVAS, {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(reserva)
        });

        if (!response.ok) {

            throw new Error();
        }

        exibirMensagem(
            "Reserva solicitada com sucesso!",
            "success"
        );

        formReserva.reset();

        infoAmbiente.innerHTML = `
            <strong>Capacidade:</strong> -
            <br>
            <strong>Localização:</strong> -
            <br>
            <strong>Recursos:</strong> -
        `;

    } catch (error) {

        exibirMensagem(
            "Não foi possível realizar a reserva. Verifique se o horário já está ocupado.",
            "danger"
        );

        console.error(error);
    }
});

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
