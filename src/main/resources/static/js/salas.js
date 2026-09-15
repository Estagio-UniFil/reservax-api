let idSalaEmEdicao = null;
let listaAmbientes = [];
let filtroAtual = "TODAS";
let usuarioAdmin = false;

document.addEventListener("DOMContentLoaded", async function () {
    const botaoSalvar = document.querySelector(".btn-salvar-sala");
    const formNovaSala = document.getElementById("formNovaSala");

    const botaoFiltroSalas = document.getElementById("botaoFiltroSalas");
    const menuFiltroSalas = document.getElementById("menuFiltroSalas");

    await carregarPerfilUsuario();
    carregarSalas();

    if (botaoFiltroSalas && menuFiltroSalas) {
        botaoFiltroSalas.addEventListener("click", function (event) {
            event.stopPropagation();
            const menuAberto = menuFiltroSalas.classList.toggle("mostrar");
            botaoFiltroSalas.setAttribute("aria-expanded", menuAberto);
        });

        menuFiltroSalas.addEventListener("click", function (event) {
            event.stopPropagation();
        });

        document.addEventListener("click", function () {
            menuFiltroSalas.classList.remove("mostrar");
            botaoFiltroSalas.setAttribute("aria-expanded", "false");
        });
    }

    botaoSalvar.addEventListener("click", async function () {
        const ambiente = montarObjetoAmbiente();

        if (!ambiente.nome || !ambiente.tipo || !ambiente.capacidade) {
            alert("Preencha nome, tipo e capacidade.");
            return;
        }

        try {
            if (idSalaEmEdicao === null) {
                await cadastrarSala(ambiente);
                alert("Ambiente cadastrado com sucesso!");
            } else {
                await editarSala(idSalaEmEdicao, ambiente);
                alert("Ambiente salvo com sucesso!");
            }

            formNovaSala.reset();
            idSalaEmEdicao = null;

            document.getElementById("modalNovaSalaLabel").innerText = "Cadastrar novo ambiente";
            botaoSalvar.innerText = "Salvar";

            const modal = bootstrap.Modal.getInstance(document.getElementById("modalNovaSala"));
            modal.hide();

            await carregarSalas();

        } catch (erro) {
            console.error(erro);
            alert("Não foi possível salvar o ambiente.");
        }
    });

    document.getElementById("modalNovaSala").addEventListener("hidden.bs.modal", function () {
        formNovaSala.reset();
        idSalaEmEdicao = null;
        document.getElementById("modalNovaSalaLabel").innerText = "Cadastrar novo ambiente";
        botaoSalvar.innerText = "Salvar";
    });
});

function montarObjetoAmbiente() {
    return {
        nome: document.getElementById("nomeSala").value,
        tipo: document.getElementById("tipoSala").value,
        capacidade: parseInt(document.getElementById("capacidadeSala").value),
        localizacao: document.getElementById("localizacaoSala").value,
        descricao: document.getElementById("descricaoSala").value,
        possuiProjetor: document.getElementById("possuiProjetor").checked,
        possuiTv: document.getElementById("possuiTv").checked,
        possuiMicrofone: document.getElementById("possuiMicrofone").checked,
        possuiComputadores: document.getElementById("possuiComputadores").checked,
        ativo: true
    };
}

async function cadastrarSala(ambiente) {
    const resposta = await fetchComCsrf("/ambientes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ambiente)
    });

    if (!resposta.ok) {
        throw new Error("Erro ao cadastrar ambiente.");
    }

    return await resposta.json();
}

async function editarSala(id, ambiente) {
    const resposta = await fetchComCsrf(`/ambientes/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ambiente)
    });

    if (!resposta.ok) {
        throw new Error("Erro ao editar ambiente.");
    }

    return await resposta.json();
}

async function excluirSala(id) {
    const confirmar = confirm("Tem certeza que deseja excluir este ambiente?");

    if (!confirmar) {
        return;
    }

    try {
        const resposta = await fetchComCsrf(`/ambientes/${id}`, {
            method: "DELETE"
        });

        if (!resposta.ok) {
            throw new Error("Erro ao excluir ambiente.");
        }

        alert("Ambiente excluído com sucesso!");

        await carregarSalas();

    } catch (erro) {
        console.error(erro);
        alert("Não foi possível excluir o ambiente.");
    }
}

async function carregarSalas() {
    try {
        const resposta = await fetch("/ambientes");

        if (!resposta.ok) {
            throw new Error("Erro ao carregar ambientes.");
        }

        listaAmbientes = await resposta.json();

        renderizarSalas();

    } catch (erro) {
        console.error(erro);
        alert("Não foi possível carregar os ambientes cadastrados.");
    }
}

function renderizarSalas() {
    const cardsGrid = document.getElementById("listaSalas");
    cardsGrid.innerHTML = "";

    let ambientesFiltrados = listaAmbientes;

    if (filtroAtual !== "TODAS") {
        ambientesFiltrados = listaAmbientes.filter(ambiente => ambiente.tipo === filtroAtual);
    }

    if (ambientesFiltrados.length === 0) {
        cardsGrid.innerHTML = `
            <div class="empty-message">
                Nenhum ambiente encontrado para este filtro.
            </div>
        `;
        return;
    }

    ambientesFiltrados.forEach(ambiente => {
        adicionarCardNaTela(ambiente);
    });
}

function filtrarSalas(tipo) {
    filtroAtual = tipo;

    const botaoFiltro = document.getElementById("botaoFiltroSalas");
    const menuFiltro = document.getElementById("menuFiltroSalas");

    if (tipo === "TODAS") {
        botaoFiltro.innerHTML = "Visualizar ambientes <span>∨</span>";
    } else {
        botaoFiltro.innerHTML = `${formatarTipoAmbiente(tipo)} <span>∨</span>`;
    }

    if (menuFiltro) {
        menuFiltro.classList.remove("mostrar");
    }

    document.getElementById("botaoFiltroSalas")
        .setAttribute("aria-expanded", "false");

    document.querySelectorAll("#menuFiltroSalas button")
        .forEach(botao => botao.classList.remove("ativo"));

    const opcoesFiltro = {
        TODAS: 0,
        SALA_AULA: 1,
        LABORATORIO: 2,
        AUDITORIO: 3,
        SALA_REUNIAO: 4,
        BIBLIOTECA: 5
    };

    const botaoSelecionado = document.querySelectorAll(
        "#menuFiltroSalas button"
    )[opcoesFiltro[tipo]];

    if (botaoSelecionado) {
        botaoSelecionado.classList.add("ativo");
    }

    renderizarSalas();
}

function adicionarCardNaTela(ambiente) {
    const cardsGrid = document.getElementById("listaSalas");

    const recursos = [];

    if (ambiente.possuiProjetor) recursos.push("Possui projetor");
    if (ambiente.possuiTv) recursos.push("Possui TV");
    if (ambiente.possuiComputadores) recursos.push("Possui computadores");
    if (ambiente.possuiMicrofone) recursos.push("Possui microfone");

    const recursosHtml = recursos.length > 0
        ? recursos.map(recurso => `<p>${recurso}</p>`).join("")
        : "<p>Sem recursos informados</p>";

    const card = document.createElement("div");
    card.classList.add("room-card");

    card.innerHTML = `
        <div class="card-top">
            <div class="room-icon"></div>

            <div class="resources">
                ${recursosHtml}
            </div>
        </div>

        <h3>${ambiente.nome}</h3>

        <span class="tag">${formatarTipoAmbiente(ambiente.tipo)}</span>

        <p class="capacity">👥 Capacidade: ${ambiente.capacidade} pessoas</p>

        ${usuarioAdmin ? `
            <div class="card-actions">
                <button class="btn-editar-sala" onclick="abrirModalEdicao(${ambiente.id})">
                    Editar
                </button>

                <button class="btn-excluir-sala" onclick="excluirSala(${ambiente.id})">
                    Excluir
                </button>
            </div>
        ` : ""}
    `;

    cardsGrid.appendChild(card);
}

async function abrirModalEdicao(id) {
    try {
        const resposta = await fetch(`/ambientes/${id}`);

        if (!resposta.ok) {
            throw new Error("Erro ao buscar ambiente.");
        }

        const ambiente = await resposta.json();

        idSalaEmEdicao = ambiente.id;

        document.getElementById("nomeSala").value = ambiente.nome;
        document.getElementById("tipoSala").value = ambiente.tipo;
        document.getElementById("capacidadeSala").value = ambiente.capacidade;
        document.getElementById("localizacaoSala").value = ambiente.localizacao || "";
        document.getElementById("descricaoSala").value = ambiente.descricao || "";

        document.getElementById("possuiProjetor").checked = ambiente.possuiProjetor;
        document.getElementById("possuiTv").checked = ambiente.possuiTv;
        document.getElementById("possuiMicrofone").checked = ambiente.possuiMicrofone;
        document.getElementById("possuiComputadores").checked = ambiente.possuiComputadores;

        document.getElementById("modalNovaSalaLabel").innerText = "Editar ambiente";
        document.querySelector(".btn-salvar-sala").innerText = "Salvar";

        const modal = new bootstrap.Modal(document.getElementById("modalNovaSala"));
        modal.show();

    } catch (erro) {
        console.error(erro);
        alert("Não foi possível carregar os dados do ambiente.");
    }
}

function formatarTipoAmbiente(tipo) {
    const tipos = {
        SALA_AULA: "Sala de Aula",
        LABORATORIO: "Laboratório",
        AUDITORIO: "Auditório",
        SALA_REUNIAO: "Sala de Reunião",
        BIBLIOTECA: "Biblioteca"
    };

    return tipos[tipo] || tipo;
}

async function carregarPerfilUsuario() {
    try {
        const resposta = await fetch("/auth/me");
        const usuario = await resposta.json();
        usuarioAdmin = resposta.ok && usuario.tipoUsuario === "ADMIN";

        const areaAdicionar = document.querySelector(".add-area");
        if (areaAdicionar) {
            areaAdicionar.hidden = !usuarioAdmin;
        }
    } catch (erro) {
        usuarioAdmin = false;
    }
}
