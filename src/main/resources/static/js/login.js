const campoSenhaLogin = document.getElementById("senhaLogin");
const botaoVisibilidadeSenha = document.getElementById("alternarVisibilidadeSenha");

botaoVisibilidadeSenha.addEventListener("click", () => {
    const senhaVisivel = campoSenhaLogin.type === "text";
    campoSenhaLogin.type = senhaVisivel ? "password" : "text";

    const visivelAgora = !senhaVisivel;
    botaoVisibilidadeSenha.setAttribute("aria-pressed", String(visivelAgora));
    botaoVisibilidadeSenha.setAttribute(
        "aria-label",
        visivelAgora ? "Ocultar senha" : "Mostrar senha"
    );
    botaoVisibilidadeSenha.querySelector("span").innerText = visivelAgora
        ? "🙈"
        : "👁";
});
