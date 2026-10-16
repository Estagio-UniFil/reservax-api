const listaUsuarios = document.getElementById("listaUsuarios");
const mensagemUsuarios = document.getElementById("mensagemUsuarios");
const formUsuario = document.getElementById("formUsuario");
const modalUsuario = new bootstrap.Modal(document.getElementById("modalUsuario"));

let usuarios = [];

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("botaoNovoUsuario").addEventListener("click", abrirNovoUsuario);
    formUsuario.addEventListener("submit", salvarUsuario);
    carregarUsuarios();
});

async function carregarUsuarios() {
    try {
        const resposta = await fetch("/api/usuarios");

        if (!resposta.ok) {
            throw new Error("Não foi possível carregar os usuários.");
        }

        usuarios = await resposta.json();
        renderizarUsuarios();
    } catch (erro) {
        exibirMensagemUsuarios(erro.message, "danger");
    }
}

function renderizarUsuarios() {
    listaUsuarios.innerHTML = "";

    if (usuarios.length === 0) {
        listaUsuarios.innerHTML = `
            <tr><td colspan="5" class="lista-vazia">Nenhum usuário cadastrado.</td></tr>
        `;
        return;
    }

    usuarios.forEach(usuario => {
        const linha = document.createElement("tr");
        linha.innerHTML = `
            <td>${escaparTextoUsuario(usuario.nome)}</td>
            <td>${escaparTextoUsuario(usuario.email)}</td>
            <td>
                <span class="tipo-usuario ${usuario.tipoUsuario === "ADMIN" ? "tipo-admin" : "tipo-usuario-comum"}">
                    ${usuario.tipoUsuario === "ADMIN" ? "Admin" : "Usuário"}
                </span>
            </td>
            <td>
                <span class="situacao-usuario ${usuario.ativo ? "situacao-ativo" : "situacao-inativo"}">
                    ${usuario.ativo ? "Ativo" : "Inativo"}
                </span>
            </td>
            <td>
                <div class="acoes-usuario">
                    <button class="btn-editar-usuario" type="button" onclick="abrirEdicaoUsuario(${usuario.id})">
                        Editar
                    </button>
                    ${usuario.tipoUsuario === "ADMIN" ? `
                        <span class="conta-admin-protegida" title="Contas administrativas não podem ser excluídas nesta tela">
                            Protegida
                        </span>
                    ` : usuario.ativo ? `
                        <button class="btn-desativar-usuario" type="button" onclick="desativarUsuario(${usuario.id})">
                            Desativar
                        </button>
                    ` : "-"}
                </div>
            </td>
        `;
        listaUsuarios.appendChild(linha);
    });
}

function abrirNovoUsuario() {
    formUsuario.reset();
    document.getElementById("usuarioId").value = "";
    document.getElementById("tituloModalUsuario").innerText = "Novo usuário";
    document.getElementById("usuarioTipo").value = "USUARIO";
    document.getElementById("usuarioTipo").disabled = false;
    document.getElementById("ajudaTipoUsuario").innerText =
        "Administradores possuem acesso completo ao sistema.";
    document.getElementById("usuarioSenha").required = true;
    document.getElementById("ajudaSenha").innerText = "Mínimo de 6 caracteres.";
    modalUsuario.show();
}

function abrirEdicaoUsuario(id) {
    const usuario = usuarios.find(item => item.id === id);

    if (!usuario) {
        return;
    }

    formUsuario.reset();
    document.getElementById("usuarioId").value = usuario.id;
    document.getElementById("usuarioNome").value = usuario.nome;
    document.getElementById("usuarioEmail").value = usuario.email;
    document.getElementById("usuarioTipo").value = usuario.tipoUsuario;
    document.getElementById("usuarioTipo").disabled = true;
    document.getElementById("ajudaTipoUsuario").innerText =
        "O tipo de uma conta existente não pode ser alterado.";
    document.getElementById("usuarioSenha").required = false;
    document.getElementById("tituloModalUsuario").innerText = "Editar usuário";
    document.getElementById("ajudaSenha").innerText = "Deixe em branco para manter a senha atual.";
    modalUsuario.show();
}

async function salvarUsuario(event) {
    event.preventDefault();

    const id = document.getElementById("usuarioId").value;
    const usuario = {
        nome: document.getElementById("usuarioNome").value,
        email: document.getElementById("usuarioEmail").value,
        senha: document.getElementById("usuarioSenha").value,
        tipoUsuario: document.getElementById("usuarioTipo").value
    };

    try {
        const resposta = await fetchComCsrf(
            id ? `/api/usuarios/${id}` : "/api/usuarios",
            {
                method: id ? "PUT" : "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(usuario)
            }
        );

        if (!resposta.ok) {
            throw new Error(await obterErroUsuario(resposta));
        }

        modalUsuario.hide();
        exibirMensagemUsuarios(
            id ? "Usuário atualizado com sucesso." : "Usuário criado com sucesso.",
            "success"
        );
        await carregarUsuarios();
    } catch (erro) {
        exibirMensagemUsuarios(erro.message, "danger");
    }
}

async function desativarUsuario(id) {
    if (!confirm("Deseja desativar este usuário?")) {
        return;
    }

    try {
        const resposta = await fetchComCsrf(`/api/usuarios/${id}`, {
            method: "DELETE"
        });

        if (!resposta.ok) {
            throw new Error(await obterErroUsuario(resposta));
        }

        exibirMensagemUsuarios("Usuário desativado com sucesso.", "success");
        await carregarUsuarios();
    } catch (erro) {
        exibirMensagemUsuarios(erro.message, "danger");
    }
}

async function obterErroUsuario(resposta) {
    try {
        const dados = await resposta.json();
        return dados.mensagem || "Não foi possível concluir a operação.";
    } catch (erro) {
        return "Não foi possível concluir a operação.";
    }
}

function exibirMensagemUsuarios(texto, tipo) {
    mensagemUsuarios.innerHTML = `<div class="alert alert-${tipo}">${texto}</div>`;
}

function escaparTextoUsuario(valor) {
    const elemento = document.createElement("div");
    elemento.innerText = valor || "-";
    return elemento.innerHTML;
}
