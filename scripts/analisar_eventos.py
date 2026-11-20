"""
Análise dos testes técnicos do ConTato (C4) a partir do registro de eventos.

Lê os CSVs gerados pelo app em build debug:
  ct_eventos_surdocego.csv    (D1, modo Surdocego)
  ct_eventos_comunicador.csv  (D2, modo Comunicador)

Uso:
  python analisar_eventos.py ct_eventos_surdocego.csv ct_eventos_comunicador.csv
  python analisar_eventos.py ... --desde "2026-11-16 14:00" --ate "2026-11-16 15:30"
  python analisar_eventos.py ... --saida resultados_C4

--desde/--ate filtram pela hora de cada aparelho (útil para separar um caso do outro
usando as horas anotadas na planilha). Sem filtro, usa o arquivo inteiro.

Gera na pasta de saída (padrão: resultados_eventos):
  ct37_mensagens.csv      uma linha por mensagem do Comunicador: enviada, recebida (R),
                          vibrando (P), concluída (D), falha, e o que o D1 registrou
  ct16_respostas.csv      uma linha por resposta composta no D1, com o texto que chegou ao D2
  ct41_combinacoes.csv    combinações inválidas: pontos, se vibrou erro, se o texto ficou igual
  gestos.csv              espaço, apagar, repetir, interromper, gesto recusado
  ct29_reconexoes.csv     cada queda: início, fim do modo avião, reconexão e tempos (s)
  ct39_conexoes.csv       tempo do pedido de conexão até "conectado"
  resumo.md               totais de cada caso, para colar na planilha e no TCC

Os tempos de reconexão e conexão usam só o relógio de um aparelho (coluna uptime_ms).
Nada aqui subtrai a hora de um aparelho da hora do outro.

Só usa a biblioteca padrão do Python (não precisa instalar nada).
"""

import argparse
import csv
import sys
from datetime import datetime
from pathlib import Path

COLUNAS = ["data_hora", "epoch_ms", "uptime_ms", "papel", "evento", "msg_id", "detalhe", "aparelho", "versao_app"]
FORMATO_HORA = "%Y-%m-%d %H:%M:%S.%f"


# ───────── Leitura ─────────

def detalhe_para_dict(detalhe: str) -> dict:
    """'tam=3;texto=OI; TUDO' -> {'tam': '3', 'texto': 'OI; TUDO'} (texto sempre vem por último)."""
    resultado = {}
    resto = detalhe or ""
    if "texto=" in resto:
        antes, _, texto = resto.partition("texto=")
        resultado["texto"] = texto
        resto = antes.rstrip(";")
    for par in filter(None, resto.split(";")):
        chave, _, valor = par.partition("=")
        resultado[chave] = valor
    return resultado


def ler(caminho: str, desde, ate) -> list:
    with open(caminho, newline="", encoding="utf-8") as f:
        leitor = csv.DictReader(f)
        faltando = [c for c in COLUNAS if c not in (leitor.fieldnames or [])]
        if faltando:
            sys.exit(f"{caminho}: faltam as colunas {faltando}")
        eventos = []
        for linha in leitor:
            hora = datetime.strptime(linha["data_hora"], FORMATO_HORA)
            if desde and hora < desde:
                continue
            if ate and hora > ate:
                continue
            linha["hora"] = hora
            linha["uptime_ms"] = int(linha["uptime_ms"])
            linha["d"] = detalhe_para_dict(linha["detalhe"])
            eventos.append(linha)
    eventos.sort(key=lambda e: int(e["epoch_ms"]))
    return eventos


def hora_curta(evento) -> str:
    return evento["hora"].strftime("%H:%M:%S.%f")[:-3] if evento else ""


def segundos(inicio, fim) -> str:
    if not inicio or not fim:
        return ""
    return f"{(fim['uptime_ms'] - inicio['uptime_ms']) / 1000:.1f}"


def gravar(pasta: Path, nome: str, linhas: list, colunas: list):
    with open(pasta / nome, "w", newline="", encoding="utf-8-sig") as f:
        escritor = csv.DictWriter(f, fieldnames=colunas, delimiter=";")
        escritor.writeheader()
        escritor.writerows(linhas)


def sim_nao(valor) -> str:
    return "Sim" if valor else "Não"


# ───────── CT37: mensagens do Comunicador ─────────

def ct37(d1: list, d2: list) -> list:
    por_id_d1 = {}
    for e in d1:
        if e["msg_id"]:
            por_id_d1.setdefault(e["msg_id"], []).append(e)
    linhas = []
    envios = [e for e in d2 if e["evento"] == "ENVIO"]
    for n, envio in enumerate(envios, start=1):
        mid = envio["msg_id"]
        do_id = [e for e in d2 if e["msg_id"] == mid]
        r = next((e for e in do_id if e["evento"] == "R_RECEBIDA"), None)
        ps = [e for e in do_id if e["evento"] == "P_PROGRESSO"]
        d = next((e for e in do_id if e["evento"] == "D_CONCLUIDA"), None)
        falha = next((e for e in do_id if e["evento"] == "FALHA"), None)
        no_d1 = por_id_d1.get(mid, [])
        chegou = next((e for e in no_d1 if e["evento"] == "MSG_RECEBIDA"), None)
        inicio_vib = next((e for e in no_d1 if e["evento"] == "VIBRACAO_INICIO"), None)
        fim_vib = next((e for e in no_d1 if e["evento"] == "VIBRACAO_FIM"), None)
        interrompida = any(e["evento"] == "VIBRACAO_INTERROMPIDA" for e in no_d1)
        completa = bool(r and ps and d and not falha)
        linhas.append({
            "n": n,
            "hora_envio_D2": hora_curta(envio),
            "texto": envio["d"].get("texto", ""),
            "tamanho": envio["d"].get("tam", ""),
            "enviada": "Sim",
            "recebida_R": sim_nao(r),
            "R_apos_envio_ms": (r["uptime_ms"] - envio["uptime_ms"]) if r else "",
            "vibrando_P": sim_nao(ps),
            "letras_P": len(ps),
            "concluida_D": sim_nao(d),
            "D_apos_envio_s": segundos(envio, d),
            "falha": sim_nao(falha),
            "D1_recebeu": sim_nao(chegou),
            "D1_texto_igual": sim_nao(chegou and chegou["d"].get("texto") == envio["d"].get("texto")) if chegou else "",
            "D1_vibrou": sim_nao(inicio_vib),
            "D1_fim_vibracao": "Sim" if fim_vib else ("Interrompida" if interrompida else "Não"),
            "sequencia_completa": sim_nao(completa),
            "msg_id": mid,
        })
    return linhas


# ───────── CT16/CT20: respostas compostas na célula ─────────

def ct16(d1: list, d2: list) -> list:
    recebidas = {e["msg_id"]: e for e in d2 if e["evento"] == "RESPOSTA_RECEBIDA"}
    linhas = []
    enviadas = [e for e in d1 if e["evento"] == "RESPOSTA_ENVIADA"]
    inicio = 0
    for n, env in enumerate(enviadas, start=1):
        # Letras e gestos usados para compor esta resposta (desde a anterior).
        idx = d1.index(env)
        trecho = d1[inicio:idx]
        inicio = idx + 1
        letras = sum(1 for e in trecho if e["evento"] == "LETRA_CONFIRMADA")
        espacos = sum(1 for e in trecho if e["evento"] == "ESPACO")
        apagadas = sum(1 for e in trecho if e["evento"] == "APAGAR")
        invalidas = sum(1 for e in trecho if e["evento"] == "COMBINACAO_INVALIDA")
        rec = recebidas.get(env["msg_id"])
        enviado = env["d"].get("texto", "")
        recebido = rec["d"].get("texto", "") if rec else ""
        linhas.append({
            "n": n,
            "hora_envio_D1": hora_curta(env),
            "texto_composto_D1": enviado,
            "letras_confirmadas": letras,
            "espacos": espacos,
            "apagadas": apagadas,
            "combinacoes_invalidas": invalidas,
            "chegou_D2": sim_nao(rec),
            "hora_chegada_D2": hora_curta(rec),
            "texto_recebido_D2": recebido,
            "identico": sim_nao(rec and enviado == recebido),
            "msg_id": env["msg_id"],
        })
    return linhas


# ───────── CT41: combinações inválidas ─────────

def ct41(d1: list) -> list:
    linhas = []
    texto_atual = ""
    for i, e in enumerate(d1):
        ev = e["evento"]
        if ev == "COMBINACAO_INVALIDA":
            # O aviso de erro é registrado logo antes do evento (mesmo toque).
            perto = d1[max(0, i - 3):i + 3]
            vibrou_erro = any(
                x["evento"] == "AVISO_VIBRADO" and x["d"].get("evento") == "ERROR"
                and abs(x["uptime_ms"] - e["uptime_ms"]) <= 1000
                for x in perto
            )
            depois = e["d"].get("texto", "")
            linhas.append({
                "n": len(linhas) + 1,
                "hora": hora_curta(e),
                "pontos": e["d"].get("pontos", ""),
                "vibrou_erro": sim_nao(vibrou_erro),
                "texto_antes": texto_atual,
                "texto_depois": depois,
                "texto_inalterado": sim_nao(texto_atual == depois),
            })
        if ev in ("LETRA_CONFIRMADA", "COMBINACAO_INVALIDA", "LIMITE_LETRAS", "ESPACO", "APAGAR", "APAGAR_VAZIO"):
            texto_atual = e["d"].get("texto", texto_atual)
        elif ev == "RESPOSTA_ENVIADA":
            texto_atual = ""
    return linhas


# ───────── Gestos (CT15, CT17, CT18, CT42, CT43) ─────────

GESTOS = {
    "LETRA_CONFIRMADA": "CT15 letra confirmada",
    "ESPACO": "CT17 espaço",
    "APAGAR": "CT18 apagar",
    "APAGAR_VAZIO": "CT18 apagar sem texto",
    "REPETIR": "CT42 repetir",
    "REPETIR_RECUSADO": "CT42 repetir recusado",
    "VIBRACAO_INTERROMPIDA": "CT43 interrupção",
    "GESTO_RECUSADO": "gesto recusado",
    "LIMITE_LETRAS": "limite de letras",
}


def gestos(d1: list) -> list:
    linhas = []
    for i, e in enumerate(d1):
        if e["evento"] not in GESTOS:
            continue
        resultado = ""
        if e["evento"] == "REPETIR":
            # Repetiu de verdade se uma nova vibração começou em até 2 s.
            nova = next((x for x in d1[i + 1:i + 6] if x["evento"] == "VIBRACAO_INICIO"
                         and x["uptime_ms"] - e["uptime_ms"] <= 2000), None)
            resultado = "vibrou de novo" if nova else "não vibrou"
        aviso = next((x["d"].get("evento") for x in d1[max(0, i - 2):i + 2]
                      if x["evento"] == "AVISO_VIBRADO" and abs(x["uptime_ms"] - e["uptime_ms"]) <= 1000), "")
        linhas.append({
            "hora": hora_curta(e),
            "caso": GESTOS[e["evento"]],
            "evento": e["evento"],
            "detalhe": e["detalhe"],
            "aviso_vibrado": aviso,
            "resultado": resultado,
        })
    return linhas


# ───────── CT29: quedas e reconexão ─────────

def estado(e):
    return e["d"].get("estado") if e["evento"] == "CONEXAO" else None


def ct29(d1: list, d2: list) -> list:
    """Cada queda no D1: CONNECTED -> (perdeu) -> CONNECTED de novo."""
    linhas = []
    conectado = False
    queda = None
    aviao_on = aviao_off = None
    for e in d1:
        if e["evento"] == "MODO_AVIAO":
            if e["d"].get("ligado") == "true":
                aviao_on = e
            else:
                aviao_off = e
        st = estado(e)
        if st == "CONNECTED":
            if queda:
                falhas = [x for x in d2 if x["evento"] == "FALHA"
                          and queda["hora"] <= x["hora"] <= e["hora"]]
                linhas.append({
                    "n": len(linhas) + 1,
                    "inicio_queda_D1": hora_curta(queda),
                    "modo_aviao_ligado": hora_curta(aviao_on) if aviao_on and aviao_on["uptime_ms"] <= queda["uptime_ms"] + 5000 else "",
                    "modo_aviao_desligado": hora_curta(aviao_off) if aviao_off and aviao_off["uptime_ms"] >= queda["uptime_ms"] else "",
                    "reconectou_D1": hora_curta(e),
                    "queda_total_s": segundos(queda, e),
                    "fim_aviao_ate_conectar_s": segundos(aviao_off, e) if aviao_off and aviao_off["uptime_ms"] >= queda["uptime_ms"] else "",
                    "reconectou_sozinho": "Sim",
                    "msgs_marcadas_falha_D2": len(falhas),
                })
                queda = None
            conectado = True
        elif st in ("DISCONNECTED", "CONNECTION_ERROR") and conectado:
            queda = e
            conectado = False
    if queda:
        linhas.append({
            "n": len(linhas) + 1, "inicio_queda_D1": hora_curta(queda), "modo_aviao_ligado": "",
            "modo_aviao_desligado": hora_curta(aviao_off) if aviao_off and aviao_off["uptime_ms"] >= queda["uptime_ms"] else "",
            "reconectou_D1": "", "queda_total_s": "", "fim_aviao_ate_conectar_s": "",
            "reconectou_sozinho": "Não (fim do arquivo)", "msgs_marcadas_falha_D2": "",
        })
    return linhas


# ───────── CT39: tempo de conexão ─────────

def ct39(d2: list) -> list:
    """Do pedido no D2 (abriu a tela ou tocou em Conectar) até CONNECTED no D2."""
    linhas = []
    pedido = None
    for e in d2:
        if e["evento"] in ("SESSAO_INICIO", "BUSCA_MANUAL"):
            pedido = e
        elif estado(e) == "CONNECTED" and pedido:
            linhas.append({
                "n": len(linhas) + 1,
                "pedido": pedido["evento"],
                "hora_pedido": hora_curta(pedido),
                "hora_conectado": hora_curta(e),
                "tempo_s": segundos(pedido, e),
            })
            pedido = None
    return linhas


# ───────── Resumo ─────────

def resumo(r37, r16, r41, rg, r29, r39, d1, d2) -> str:
    def tempos(linhas, chave):
        v = [float(x[chave]) for x in linhas if x.get(chave) not in ("", None)]
        if not v:
            return "sem dados"
        return f"média {sum(v) / len(v):.1f} s, mínimo {min(v):.1f} s, máximo {max(v):.1f} s (n = {len(v)})"

    aparelho = lambda ev: (ev[0]["aparelho"] + ", versão " + ev[0]["versao_app"]) if ev else "sem arquivo"
    l = ["# Resumo dos testes técnicos (registro de eventos)", ""]
    l.append(f"- D1 (Surdocego): {aparelho(d1)}; {len(d1)} eventos, de {hora_curta(d1[0]) if d1 else '-'} a {hora_curta(d1[-1]) if d1 else '-'}")
    l.append(f"- D2 (Comunicador): {aparelho(d2)}; {len(d2)} eventos, de {hora_curta(d2[0]) if d2 else '-'} a {hora_curta(d2[-1]) if d2 else '-'}")
    l.append("")
    completas = sum(1 for x in r37 if x["sequencia_completa"] == "Sim")
    falhas = sum(1 for x in r37 if x["falha"] == "Sim")
    l.append(f"**CT37 (mensagens do Comunicador):** {completas} de {len(r37)} com a sequência completa "
             f"(enviada, recebida, vibrando, concluída); {falhas} marcadas como falha.")
    idem = sum(1 for x in r16 if x["identico"] == "Sim")
    l.append(f"**CT16/CT20 (respostas pela célula):** {idem} de {len(r16)} chegaram idênticas ao Comunicador.")
    ok41 = sum(1 for x in r41 if x["vibrou_erro"] == "Sim" and x["texto_inalterado"] == "Sim")
    l.append(f"**CT41 (combinações inválidas):** {ok41} de {len(r41)} com vibração de erro e texto inalterado.")
    for caso in ("CT17 espaço", "CT18 apagar", "CT42 repetir", "CT43 interrupção"):
        n = sum(1 for x in rg if x["caso"] == caso)
        extra = ""
        if caso == "CT42 repetir":
            ok = sum(1 for x in rg if x["caso"] == caso and x["resultado"] == "vibrou de novo")
            extra = f" ({ok} vibraram de novo)"
        l.append(f"**{caso.split()[0]} ({' '.join(caso.split()[1:])}):** {n} registros{extra}.")
    auto = sum(1 for x in r29 if x["reconectou_sozinho"] == "Sim")
    l.append(f"**CT29 (quedas):** {auto} de {len(r29)} reconectaram sem intervenção. "
             f"Do fim do modo avião até conectar: {tempos(r29, 'fim_aviao_ate_conectar_s')}.")
    l.append(f"**CT39 (tempo de conexão):** {tempos(r39, 'tempo_s')}.")
    l.append("")
    l.append("Observação: os tempos usam o relógio de um só aparelho; as horas dos dois "
             "aparelhos podem diferir alguns segundos entre si.")
    return "\n".join(l) + "\n"


def main():
    ap = argparse.ArgumentParser(description="Análise do registro de eventos do ConTato (C4)")
    ap.add_argument("surdocego", help="ct_eventos_surdocego.csv (D1)")
    ap.add_argument("comunicador", help="ct_eventos_comunicador.csv (D2)")
    ap.add_argument("--desde", help='ex.: "2026-11-16 14:00"')
    ap.add_argument("--ate", help='ex.: "2026-11-16 15:30"')
    ap.add_argument("--saida", default="resultados_eventos")
    a = ap.parse_args()

    def hora(texto):
        if not texto:
            return None
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"):
            try:
                return datetime.strptime(texto, fmt)
            except ValueError:
                pass
        sys.exit(f"Hora inválida: {texto} (use AAAA-MM-DD HH:MM)")

    desde, ate = hora(a.desde), hora(a.ate)
    d1 = ler(a.surdocego, desde, ate)
    d2 = ler(a.comunicador, desde, ate)
    pasta = Path(a.saida)
    pasta.mkdir(parents=True, exist_ok=True)

    r37, r16, r41 = ct37(d1, d2), ct16(d1, d2), ct41(d1)
    rg, r29, r39 = gestos(d1), ct29(d1, d2), ct39(d2)
    for nome, linhas in [("ct37_mensagens.csv", r37), ("ct16_respostas.csv", r16),
                         ("ct41_combinacoes.csv", r41), ("gestos.csv", rg),
                         ("ct29_reconexoes.csv", r29), ("ct39_conexoes.csv", r39)]:
        colunas = list(linhas[0].keys()) if linhas else ["sem_dados"]
        gravar(pasta, nome, linhas, colunas)
    texto = resumo(r37, r16, r41, rg, r29, r39, d1, d2)
    (pasta / "resumo.md").write_text(texto, encoding="utf-8")
    print(texto)
    print(f"Arquivos gravados em: {pasta.resolve()}")


if __name__ == "__main__":
    main()
