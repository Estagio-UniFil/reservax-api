const formCadastroAdmin = document.getElementById("formCadastroAdmin");
const mensagemCadastro = document.getElementById("mensagemCadastro");

formCadastroAdmin.addEventListener("submit", async function (event) {
    event.preventDefault();

    const senha = document.getElementById("senhaAdmin").value;
    const confirmacao = document.getElementById("confirmacaoSenhaAdmin").value;

    if (senha !== confirmacao) {
        exibirMensagemCadastro("As senhas não coincidem.", "erro");
        return;
    }

    const admin = {
        nome: document.getElementById("nomeAdmin").value,
        email: document.getElementById("emailAdmin").value,
        senha
    };

    try {
        const resposta = await fetchComCsrf("/auth/cadastro-admin", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(admin)
        });

        if (!resposta.ok) {
            throw new Error(await obterMensagemCadastro(resposta));
        }

        window.location.href = "/login?cadastrado=true";
    } catch (erro) {
        exibirMensagemCadastro(erro.message, "erro");
    }
});

async function obterMensagemCadastro(resposta) {
    try {
        const dados = await resposta.json();
        return dados.mensagem || "Não foi possível criar a conta.";
    } catch (erro) {
        return "Não foi possível criar a conta.";
    }
}

function exibirMensagemCadastro(texto, tipo) {
    mensagemCadastro.innerHTML = `
        <div class="mensagem ${tipo}">${texto}</div>
    `;
}
