const mensagemConfiguracoes = document.getElementById("mensagemConfiguracoes");
const formNotificacoes = document.getElementById("formNotificacoes");
const notificacoesAtivas = document.getElementById("notificacoesAtivas");
const botaoSalvarNotificacoes = document.getElementById("salvarNotificacoes");
const botaoAbrirExclusao = document.getElementById("abrirExclusaoConta");
const botaoConfirmarExclusao = document.getElementById("confirmarExclusaoConta");
const dialogoExclusao = document.getElementById("dialogoExclusao");

document.addEventListener("DOMContentLoaded", function () {
    formNotificacoes.addEventListener("submit", salvarPreferenciaNotificacoes);
    botaoAbrirExclusao.addEventListener("click", function () {
        dialogoExclusao.showModal();
    });
    botaoConfirmarExclusao.addEventListener("click", excluirMinhaConta);

    carregarConfiguracoes();
});

async function carregarConfiguracoes() {
    try {
        const resposta = await fetch("/api/configuracoes");

        if (!resposta.ok) {
            throw new Error("Não foi possível carregar as configurações.");
        }

        const usuario = await resposta.json();
        const perfil = usuario.tipoUsuario === "ADMIN"
            ? "Administrador"
            : "Usuário";
        const inicial = (usuario.nome || "R").trim().charAt(0).toUpperCase();

        document.getElementById("configuracaoNome").value = usuario.nome;
        document.getElementById("configuracaoEmail").value = usuario.email;
        document.getElementById("configuracaoPerfil").value = perfil;
        document.getElementById("nomeResumo").innerText = usuario.nome;
        document.getElementById("tipoResumo").innerText = perfil;
        document.getElementById("avatarConfiguracoes").innerText = inicial;
        notificacoesAtivas.checked = usuario.notificacoesAtivas;

        document.body.classList.add(
            usuario.tipoUsuario === "ADMIN" ? "perfil-admin" : "perfil-usuario"
        );
    } catch (erro) {
        exibirMensagemConfiguracoes(erro.message, "danger");
    }
}

async function salvarPreferenciaNotificacoes(event) {
    event.preventDefault();
    const textoOriginal = botaoSalvarNotificacoes.innerText;
    botaoSalvarNotificacoes.disabled = true;
    botaoSalvarNotificacoes.innerText = "Salvando...";

    try {
        const resposta = await fetchComCsrf("/api/configuracoes/notificacoes", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                notificacoesAtivas: notificacoesAtivas.checked
            })
        });

        if (!resposta.ok) {
            throw new Error(await obterErroConfiguracoes(resposta));
        }

        exibirMensagemConfiguracoes(
            "Preferências de notificações atualizadas com sucesso.",
            "success"
        );
    } catch (erro) {
        exibirMensagemConfiguracoes(erro.message, "danger");
    } finally {
        botaoSalvarNotificacoes.disabled = false;
        botaoSalvarNotificacoes.innerText = textoOriginal;
    }
}

async function excluirMinhaConta() {
    const textoOriginal = botaoConfirmarExclusao.innerText;
    botaoConfirmarExclusao.disabled = true;
    botaoConfirmarExclusao.innerText = "Excluindo...";

    try {
        const resposta = await fetchComCsrf("/api/configuracoes/conta", {
            method: "DELETE"
        });

        if (!resposta.ok) {
            throw new Error(await obterErroConfiguracoes(resposta));
        }

        window.location.replace("/login?contaExcluida=true");
    } catch (erro) {
        dialogoExclusao.close();
        exibirMensagemConfiguracoes(erro.message, "danger");
        botaoConfirmarExclusao.disabled = false;
        botaoConfirmarExclusao.innerText = textoOriginal;
    }
}

async function obterErroConfiguracoes(resposta) {
    try {
        const dados = await resposta.json();
        return dados.mensagem || "Não foi possível concluir a operação.";
    } catch (erro) {
        return "Não foi possível concluir a operação.";
    }
}

function exibirMensagemConfiguracoes(texto, tipo) {
    mensagemConfiguracoes.innerHTML = "";

    const alerta = document.createElement("div");
    alerta.className = `alert alert-${tipo}`;
    alerta.innerText = texto;
    mensagemConfiguracoes.appendChild(alerta);
    window.scrollTo({ top: 0, behavior: "smooth" });
}
