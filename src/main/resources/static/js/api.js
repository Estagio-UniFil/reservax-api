let promessaCsrf = null;

async function obterCsrf() {
    if (!promessaCsrf) {
        promessaCsrf = fetch("/auth/csrf")
            .then(resposta => {
                if (!resposta.ok) {
                    throw new Error("Não foi possível iniciar uma operação segura.");
                }

                return resposta.json();
            })
            .catch(erro => {
                promessaCsrf = null;
                throw erro;
            });
    }

    return promessaCsrf;
}

async function fetchComCsrf(url, opcoes = {}) {
    const metodo = (opcoes.method || "GET").toUpperCase();

    if (["GET", "HEAD", "OPTIONS"].includes(metodo)) {
        return fetch(url, opcoes);
    }

    const csrf = await obterCsrf();
    const headers = new Headers(opcoes.headers || {});
    headers.set(csrf.headerName, csrf.token);

    return fetch(url, {
        ...opcoes,
        headers
    });
}

window.fetchComCsrf = fetchComCsrf;
