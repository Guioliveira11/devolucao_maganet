"""
Servidor de demonstração para ver o front-end sem precisar do Java nem do PostgreSQL.

Serve os arquivos de src/main/resources/static e responde a API com dados de mentira,
guardados em memória (reiniciou, voltou tudo ao início). Não use em produção.

Logins de teste (senha "teste" para os dois):
    cliente      -> telas do cliente
    funcionario  -> telas da loja

Para abrir no celular, rode com PREVIEW_HOST=0.0.0.0 e acesse pelo IP do computador.
"""
import base64
import json
import os
from datetime import datetime, timedelta
from http.cookies import SimpleCookie
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

PORTA = 8770
PASTA_STATIC = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "static"
QR_IMAGEM = base64.b64encode(Path(__file__).with_name("preview-qr.png").read_bytes()).decode()

CSRF_TOKEN = "fixture-csrf"
SENHA = "teste"
USUARIOS = {"cliente": "CLIENTE", "funcionario": "FUNCIONARIO"}

# Mesmas regras do back-end: 5 erros seguidos bloqueiam o login por 15 minutos.
MAX_TENTATIVAS = 5
MINUTOS_BLOQUEIO = 15
tentativas_erradas = {}
bloqueado_ate = {}


def agora():
    return datetime.now()


def iso(momento):
    return momento.isoformat(timespec="seconds")


# ---------------------------------------------------------------------------
# Dados de exemplo
# ---------------------------------------------------------------------------

LOJAS = [
    {
        "id": 1,
        "nome": "Loja Centro",
        "endereco": "Rua de Teste, 120 – Centro",
        "horarioFuncionamento": "Segunda a sábado, 9h às 18h",
    },
    {
        "id": 2,
        "nome": "Loja Shopping",
        "endereco": "Avenida de Teste, 450",
        "horarioFuncionamento": "Todos os dias, 10h às 22h",
    },
]

ITENS = [
    {
        "id": 1,
        "numeroPedido": "12345678",
        "produto": "Fone de ouvido sem fio",
        "quantidade": 1,
        "valorPago": 159.9,
        "dataLimiteDevolucao": (agora() + timedelta(days=7)).date().isoformat(),
        "elegivelParaDevolucao": True,
    },
    {
        "id": 2,
        "numeroPedido": "12345001",
        "produto": "Cafeteira elétrica 600 ml",
        "quantidade": 1,
        "valorPago": 189.9,
        "dataLimiteDevolucao": (agora() - timedelta(days=7)).date().isoformat(),
        "elegivelParaDevolucao": False,
    },
]


def nova_devolucao(id, loja):
    return {
        "id": id,
        "numeroPedido": "12345678",
        "produto": "Fone de ouvido sem fio",
        "quantidade": 1,
        "lojaId": loja["id"],
        "loja": loja["nome"],
        "enderecoLoja": loja["endereco"],
        "prazoSolicitacao": ITENS[0]["dataLimiteDevolucao"],
        "status": "AGUARDANDO_ESCANEAMENTO",
        "destino": None,
        "ongDestinatariaId": None,
        "criadaEm": iso(agora()),
        "recebidaEm": None,
        "destinoDefinidoEm": None,
        "concluidaEm": None,
        "qrExpiraEm": iso(agora() + timedelta(hours=24)),
    }


DEVOLUCOES = [nova_devolucao(42, LOJAS[0])]


def item_da_fila(id, pedido, produto, status, minutos, destino=None):
    return {
        "id": id,
        "numeroPedido": pedido,
        "produto": produto,
        "quantidade": 1,
        "status": status,
        "destino": destino,
        "ongDestinatariaId": None,
        "recebidaEm": iso(agora() - timedelta(minutes=minutos)),
        "minutosDesdeRecebimento": minutos,
    }


FILA_DA_LOJA = [
    item_da_fila(50, "98765432", "Caixa de som portátil", "EM_ANALISE", 90),
    item_da_fila(51, "87654321", "Liquidificador", "DESTINO_PENDENTE", 1500, destino="REPARO"),
    item_da_fila(52, "11223344", "Smart TV 55 4K", "EM_ANALISE", 10),
]

AVISOS = [
    {
        "id": 1,
        "solicitacaoId": 42,
        "tipo": "SOLICITACAO_CRIADA",
        "mensagem": "Sua solicitação de devolução foi criada.",
        "criadaEm": iso(agora()),
        "lidaEm": None,
        "qrExpiraEm": None,
    },
]

PREFERENCIAS = {"atualizacoes": True, "lembretesQr": True}


def paginar(registros, query):
    pagina = int(query.get("pagina", ["0"])[0])
    tamanho = int(query.get("tamanho", ["20"])[0])
    inicio = pagina * tamanho
    return {
        "itens": registros[inicio:inicio + tamanho],
        "pagina": pagina,
        "tamanho": tamanho,
        "total": len(registros),
    }


def buscar(lista, id):
    return next((r for r in lista if r["id"] == id), None)


def qr_ainda_vale(devolucao):
    return (devolucao["status"] == "AGUARDANDO_ESCANEAMENTO"
            and datetime.fromisoformat(devolucao["qrExpiraEm"]) > agora())


# ---------------------------------------------------------------------------
# Servidor
# ---------------------------------------------------------------------------

class Handler(SimpleHTTPRequestHandler):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(PASTA_STATIC), **kwargs)

    # --- utilidades -------------------------------------------------------

    def perfil_logado(self):
        cookie = SimpleCookie(self.headers.get("Cookie", ""))
        return cookie["testrole"].value if "testrole" in cookie else None

    def responder(self, corpo=None, status=200, cookie=None):
        self.send_response(status)
        self.send_header("Content-Type", "application/json;charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        if cookie:
            self.send_header("Set-Cookie", cookie)
        self.end_headers()
        if corpo is not None:
            self.wfile.write(json.dumps(corpo).encode())

    def erro(self, mensagem, status):
        self.responder({"mensagem": mensagem}, status)

    def id_na_url(self, caminho, posicao=4):
        return int(caminho.split("/")[posicao])

    # --- leituras ---------------------------------------------------------

    def do_GET(self):
        url = urlparse(self.path)
        caminho, query = url.path, parse_qs(url.query)

        if not caminho.startswith("/api/"):
            return super().do_GET()

        if caminho == "/api/v1/csrf":
            return self.responder({"token": CSRF_TOKEN, "headerName": "X-CSRF-TOKEN"})

        perfil = self.perfil_logado()
        if not perfil:
            return self.erro("Entre novamente", 401)

        if caminho == "/api/v1/sessao":
            if perfil == "CLIENTE":
                return self.responder({"nome": "Cliente de Teste", "perfil": perfil, "lojaId": None, "loja": None})
            return self.responder({"nome": "Atendente de Teste", "perfil": perfil, "lojaId": 1, "loja": "Loja Centro"})

        if caminho == "/api/v1/itens":
            return self.responder(paginar(ITENS, query))

        if caminho == "/api/v1/lojas":
            termo = query.get("busca", [""])[0].lower()
            encontradas = [l for l in LOJAS if termo in f"{l['nome']} {l['endereco']}".lower()]
            return self.responder(paginar(encontradas, query))

        if caminho == "/api/v1/lojas/proximas":
            return self.responder([{**loja, "distanciaKm": 1.2} for loja in LOJAS])

        if caminho == "/api/v1/notificacoes/preferencias":
            return self.responder(PREFERENCIAS)

        if caminho == "/api/v1/notificacoes":
            so_nao_lidas = query.get("naoLidas", ["false"])[0] == "true"
            avisos = [a for a in AVISOS if not so_nao_lidas or not a["lidaEm"]]
            return self.responder(paginar(avisos, query))

        if caminho == "/api/v1/funcionario/devolucoes":
            return self.responder(paginar(self.filtrar_fila(query), query))

        if caminho == "/api/v1/devolucoes":
            status = query.get("status", [None])[0]
            devolucoes = [d for d in DEVOLUCOES if not status or d["status"] == status]
            return self.responder(paginar(devolucoes, query))

        if caminho.startswith("/api/v1/devolucoes/"):
            return self.detalhe_da_devolucao(caminho, query)

        self.erro("Rota não encontrada", 404)

    def filtrar_fila(self, query):
        status = query.get("status", [None])[0]
        destino = query.get("destino", [None])[0]
        minimo_horas = query.get("minimoHoras", [None])[0]

        resultado = FILA_DA_LOJA
        if status:
            resultado = [d for d in resultado if d["status"] == status]
        if destino:
            resultado = [d for d in resultado if d["destino"] == destino]
        if minimo_horas:
            resultado = [d for d in resultado if d["minutosDesdeRecebimento"] >= int(minimo_horas) * 60]
        return resultado

    def detalhe_da_devolucao(self, caminho, query):
        devolucao = buscar(DEVOLUCOES, self.id_na_url(caminho))
        if not devolucao:
            return self.erro("Registro não encontrado", 404)

        if caminho.endswith("/acompanhamento"):
            return self.responder(devolucao)

        if caminho.endswith("/historico"):
            eventos = [{
                "id": 1,
                "tipo": "SOLICITACAO_CRIADA",
                "mensagem": "Sua solicitação de devolução foi criada.",
                "ocorridoEm": devolucao["criadaEm"],
            }]
            return self.responder(paginar(eventos, query))

        self.responder({
            "id": devolucao["id"],
            "status": devolucao["status"],
            "destinoFinal": devolucao["destino"],
            "qrCodeImagemBase64": QR_IMAGEM if qr_ainda_vale(devolucao) else None,
            "qrCodeExpiraEm": devolucao["qrExpiraEm"],
        })

    # --- escritas ---------------------------------------------------------

    def do_POST(self):
        self.alterar()

    def do_PUT(self):
        self.alterar()

    def alterar(self):
        caminho = urlparse(self.path).path
        tamanho = int(self.headers.get("Content-Length", 0))
        corpo_bruto = self.rfile.read(tamanho).decode()

        if self.headers.get("X-CSRF-TOKEN") != CSRF_TOKEN:
            return self.erro("CSRF inválido", 403)

        if caminho == "/api/v1/sessao":
            return self.login(parse_qs(corpo_bruto))

        if not self.perfil_logado():
            return self.responder(status=401)

        corpo = json.loads(corpo_bruto) if corpo_bruto else {}

        if caminho == "/api/v1/sessao/sair":
            return self.responder(status=204, cookie="testrole=; Path=/; Max-Age=0")

        if caminho == "/api/v1/notificacoes/preferencias":
            PREFERENCIAS.update(corpo)
            return self.responder(PREFERENCIAS)

        if caminho.endswith("/ler"):
            aviso = buscar(AVISOS, self.id_na_url(caminho))
            if aviso:
                aviso["lidaEm"] = iso(agora())
            return self.responder(status=204)

        if caminho == "/api/v1/devolucoes":
            loja = buscar(LOJAS, corpo["lojaId"])
            devolucao = nova_devolucao(max(d["id"] for d in DEVOLUCOES) + 1, loja)
            DEVOLUCOES.insert(0, devolucao)
            ITENS[0]["elegivelParaDevolucao"] = False
            return self.responder({"id": devolucao["id"]}, 201)

        if caminho.endswith("/cancelar"):
            devolucao = buscar(DEVOLUCOES, self.id_na_url(caminho))
            devolucao["status"] = "CANCELADA"
            return self.responder({"id": devolucao["id"]})

        if caminho.endswith("/renovar"):
            devolucao = buscar(DEVOLUCOES, self.id_na_url(caminho))
            devolucao["status"] = "AGUARDANDO_ESCANEAMENTO"
            devolucao["qrExpiraEm"] = iso(agora() + timedelta(hours=24))
            return self.responder({"id": devolucao["id"]})

        if caminho == "/api/v1/funcionario/devolucoes/escanear":
            return self.responder({"id": 55, "status": "EM_ANALISE"})

        if caminho.endswith("/destino"):
            item = buscar(FILA_DA_LOJA, self.id_na_url(caminho, posicao=5))
            item["status"] = "DESTINO_PENDENTE"
            item["destino"] = corpo["destino"]
            return self.responder({"id": item["id"]})

        self.erro("Rota não encontrada", 404)

    def login(self, formulario):
        usuario = formulario.get("username", [""])[0]
        senha = formulario.get("password", [""])[0]

        if usuario in bloqueado_ate and bloqueado_ate[usuario] > agora():
            return self.erro("Sua conta foi bloqueada por questões de segurança. "
                             "Tente novamente em 15 minutos.", 401)

        if usuario not in USUARIOS or senha != SENHA:
            if usuario in USUARIOS:
                tentativas_erradas[usuario] = tentativas_erradas.get(usuario, 0) + 1
                falhas = tentativas_erradas[usuario]
                if falhas >= MAX_TENTATIVAS:
                    bloqueado_ate[usuario] = agora() + timedelta(minutes=MINUTOS_BLOQUEIO)
                    return self.erro("Sua conta foi bloqueada por questões de segurança. Tente novamente em 15 minutos.", 401)
                
                restantes = MAX_TENTATIVAS - falhas
                msg = f"Senha incorreta. Você tem mais {restantes} tentativa{'s' if restantes > 1 else ''} antes do bloqueio."
                return self.erro(msg, 401)
                
            return self.erro("Login ou senha inválidos.", 401)

        tentativas_erradas.pop(usuario, None)
        bloqueado_ate.pop(usuario, None)
        cookie = f"testrole={USUARIOS[usuario]}; Path=/; HttpOnly; SameSite=Lax"
        return self.responder(status=204, cookie=cookie)


if __name__ == "__main__":
    host = os.environ.get("PREVIEW_HOST", "127.0.0.1")
    print(f"Preview de teste: http://127.0.0.1:{PORTA} (dados fictícios)", flush=True)
    ThreadingHTTPServer((host, PORTA), Handler).serve_forever()
