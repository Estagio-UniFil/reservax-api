const botaoNotificacoes = document.getElementById("botaoNotificacoes");
const painelNotificacoes = document.getElementById("painelNotificacoes");
const listaNotificacoes = document.getElementById("listaNotificacoes");
const bolinhaNotificacao = document.getElementById("bolinhaNotificacao");

document.addEventListener("DOMContentLoaded", function () {
    carregarUsuarioAutenticado();
    carregarNotificacoes();

    if (botaoNotificacoes && painelNotificacoes) {
        botaoNotificacoes.addEventListener("click", function (event) {
            event.stopPropagation();
            painelNotificacoes.hidden = !painelNotificacoes.hidden;
            botaoNotificacoes.setAttribute(
                "aria-expanded",
                String(!painelNotificacoes.hidden)
            );
        });

        painelNotificacoes.addEventListener("click", function (event) {
            event.stopPropagation();
        });

        document.addEventListener("click", function () {
            painelNotificacoes.hidden = true;
            botaoNotificacoes.setAttribute("aria-expanded", "false");
        });
    }

    document.querySelectorAll("[data-acao-sair]").forEach(botao => {
        botao.addEventListener("click", sair);
    });

    setInterval(carregarNotificacoes, 30000);
});

async function carregarUsuarioAutenticado() {
    try {
        const resposta = await fetch("/auth/me");

        if (!resposta.ok) {
            return;
        }

        const usuario = await resposta.json();
        const classePerfil = usuario.tipoUsuario === "ADMIN"
            ? "perfil-admin"
            : "perfil-usuario";

        document.body.classList.add(classePerfil);

        document.querySelectorAll("[data-usuario-nome]").forEach(elemento => {
            elemento.innerText = usuario.nome;
        });

        document.querySelectorAll("[data-usuario-tipo]").forEach(elemento => {
            elemento.innerText = usuario.tipoUsuario === "ADMIN"
                ? "Administrador"
                : "Usuário";
        });

        window.usuarioAutenticado = usuario;
    } catch (erro) {
        console.error(erro);
    }
}

async function carregarNotificacoes() {
    if (!listaNotificacoes || !bolinhaNotificacao) {
        return;
    }

    try {
        const resposta = await fetch("/notificacoes/nao-lidas");

        if (!resposta.ok) {
            return;
        }

        const notificacoes = await resposta.json();
        bolinhaNotificacao.hidden = notificacoes.length === 0;
        renderizarNotificacoes(notificacoes);
    } catch (erro) {
        console.error(erro);
    }
}

function renderizarNotificacoes(notificacoes) {
    listaNotificacoes.innerHTML = "";

    if (notificacoes.length === 0) {
        listaNotificacoes.innerHTML = `
            <p class="notificacoes-vazias">Nenhuma notificação nova.</p>
        `;
        return;
    }

    notificacoes.forEach(notificacao => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = "item-notificacao";
        item.innerHTML = `
            <strong>${escaparTextoNotificacao(notificacao.mensagem)}</strong>
            <small>${formatarDataNotificacao(notificacao.dataCriacao)}</small>
        `;

        item.addEventListener("click", function () {
            abrirNotificacao(notificacao.id, notificacao.rota);
        });

        listaNotificacoes.appendChild(item);
    });
}

async function abrirNotificacao(id, rota) {
    try {
        const resposta = await fetchComCsrf(`/notificacoes/${id}/ler`, {
            method: "PATCH"
        });

        if (!resposta.ok) {
            throw new Error("Não foi possível abrir a notificação.");
        }

        window.location.href = rota;
    } catch (erro) {
        console.error(erro);
    }
}

async function sair() {
    try {
        await fetchComCsrf("/logout", { method: "POST" });
    } finally {
        window.location.href = "/login";
    }
}

function formatarDataNotificacao(dataCriacao) {
    return new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(new Date(dataCriacao));
}

function escaparTextoNotificacao(valor) {
    const elemento = document.createElement("div");
    elemento.innerText = valor || "";
    return elemento.innerHTML;
}
