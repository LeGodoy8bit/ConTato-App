"""
Análise do teste de latência do ConTato (RNF02, casos CT24/CT25).

Lê os CSVs gerados pelo app em build debug:
  ct_latencia_surdocego.csv   -> coluna T1_ms (chegada da mensagem -> antes do vibrate)
  ct_latencia_comunicador.csv -> coluna T2_ms (envio -> confirmação R)

Uso:
  python analisar_latencia.py ct_latencia_surdocego.csv ct_latencia_comunicador.csv
  python analisar_latencia.py ct_latencia_surdocego.csv          (só T1)
  python analisar_latencia.py ... --saida resultados --limite 500

Gera na pasta de saída:
  estatisticas_latencia.csv   média, mediana, desvio, mínimo, máximo e P95 por tamanho
  tabela_tcc.md               tabela pronta para o TCC (Markdown)
  boxplot_T1.png              T1 por tamanho, com a linha do limite de 500 ms
  boxplot_T2.png              T2 por tamanho (se houver o CSV do Comunicador)
  medicoes_unidas.csv         T1 e T2 de cada envio lado a lado

Requer: pandas e matplotlib (pip install pandas matplotlib).
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402

COLUNAS = ["execucao", "tamanho_msg", "T1_ms", "T2_ms", "aparelho", "versao_app"]


def ler(caminho: str) -> pd.DataFrame:
    df = pd.read_csv(caminho)
    faltando = [c for c in COLUNAS if c not in df.columns]
    if faltando:
        sys.exit(f"{caminho}: faltam as colunas {faltando}")
    # Mensagens digitadas à mão não têm número de execução: ficam fora da análise.
    df = df.dropna(subset=["execucao"]).copy()
    df["execucao"] = df["execucao"].astype(int)
    df["tamanho_msg"] = df["tamanho_msg"].astype(int)
    # Se o teste foi rodado mais de uma vez sem apagar o arquivo, vale a última medição.
    return df.drop_duplicates(subset=["execucao", "tamanho_msg"], keep="last")


def estatisticas(df: pd.DataFrame, coluna: str, limite: float) -> pd.DataFrame:
    dados = df.dropna(subset=[coluna])
    if dados.empty:
        return pd.DataFrame()
    g = dados.groupby("tamanho_msg")[coluna]
    tabela = pd.DataFrame({
        "N": g.count(),
        "media_ms": g.mean(),
        "mediana_ms": g.median(),
        "desvio_ms": g.std(ddof=1),
        "minimo_ms": g.min(),
        "maximo_ms": g.max(),
        "p95_ms": g.quantile(0.95),
        "dentro_limite_pct": g.apply(lambda s: 100.0 * (s <= limite).mean()),
    })
    tabela.insert(0, "medida", coluna.replace("_ms", ""))
    return tabela.reset_index()


def boxplot(df: pd.DataFrame, coluna: str, titulo: str, destino: Path, limite: float | None) -> None:
    dados = df.dropna(subset=[coluna])
    if dados.empty:
        return
    tamanhos = sorted(dados["tamanho_msg"].unique())
    grupos = [dados.loc[dados["tamanho_msg"] == t, coluna] for t in tamanhos]

    fig, ax = plt.subplots(figsize=(7, 4.5), dpi=150)
    ax.boxplot(grupos, showmeans=True, widths=0.5,
               meanprops=dict(marker="D", markerfacecolor="white", markeredgecolor="black", markersize=5))
    ax.set_xticks(range(1, len(tamanhos) + 1), [f"{t} caracteres\n(N = {len(g)})" for t, g in zip(tamanhos, grupos)])
    if limite is not None:
        ax.axhline(limite, linestyle="--", color="#c0392b", linewidth=1.2, label=f"Limite do RNF02 ({limite:.0f} ms)")
        ax.set_ylim(0, max(limite * 1.2, dados[coluna].max() * 1.1))
        ax.legend(loc="upper right", frameon=False)
    ax.set_ylabel("Tempo (ms)")
    ax.set_title(titulo)
    ax.grid(axis="y", alpha=0.3)
    if limite is None:
        ax.set_ylim(bottom=0)
    fig.tight_layout()
    fig.savefig(destino)
    plt.close(fig)


def fmt(v: float) -> str:
    """Vírgula decimal, como no texto do TCC."""
    return f"{v:.1f}".replace(".", ",")


def tabela_markdown(est: pd.DataFrame, limite: float, aparelhos: str) -> str:
    linhas = [
        f"Tabela X – Latência medida no ConTato (aparelhos: {aparelhos})",
        "",
        "| Medida | Tamanho | N | Média (ms) | Mediana (ms) | Desvio padrão (ms) | Mínimo (ms) | Máximo (ms) | P95 (ms) | ≤ "
        f"{limite:.0f} ms |",
        "|---|---|---|---|---|---|---|---|---|---|",
    ]
    nomes = {"T1": "T1 – chegada até a vibração", "T2": "T2 – envio até a confirmação"}
    for _, r in est.iterrows():
        # O limite do RNF02 vale só para T1; T2 é informativo (tempo de ida e volta).
        dentro = f"{fmt(r['dentro_limite_pct'])}%" if r["medida"] == "T1" else "—"
        linhas.append(
            f"| {nomes.get(r['medida'], r['medida'])} | {int(r['tamanho_msg'])} caracteres | {int(r['N'])} | "
            f"{fmt(r['media_ms'])} | {fmt(r['mediana_ms'])} | {fmt(r['desvio_ms'])} | {fmt(r['minimo_ms'])} | "
            f"{fmt(r['maximo_ms'])} | {fmt(r['p95_ms'])} | {dentro} |"
        )
    linhas += ["", "Fonte: elaborado pelos autores."]
    return "\n".join(linhas)


def main() -> None:
    ap = argparse.ArgumentParser(description="Análise de latência do ConTato (RNF02)")
    ap.add_argument("csv_surdocego", help="ct_latencia_surdocego.csv (T1)")
    ap.add_argument("csv_comunicador", nargs="?", help="ct_latencia_comunicador.csv (T2), opcional")
    ap.add_argument("--saida", default="resultados_latencia", help="pasta de saída")
    ap.add_argument("--limite", type=float, default=500.0, help="limite do RNF02 em ms")
    args = ap.parse_args()

    saida = Path(args.saida)
    saida.mkdir(parents=True, exist_ok=True)

    t1 = ler(args.csv_surdocego)[["execucao", "tamanho_msg", "T1_ms", "aparelho", "versao_app"]]
    t1 = t1.rename(columns={"aparelho": "aparelho_surdocego"})
    if args.csv_comunicador:
        t2 = ler(args.csv_comunicador)[["execucao", "tamanho_msg", "T2_ms", "aparelho"]]
        t2 = t2.rename(columns={"aparelho": "aparelho_comunicador"})
        unidas = t1.merge(t2, on=["execucao", "tamanho_msg"], how="outer")
    else:
        unidas = t1.assign(T2_ms=float("nan"), aparelho_comunicador="")
    unidas = unidas.sort_values(["tamanho_msg", "execucao"])
    unidas.to_csv(saida / "medicoes_unidas.csv", index=False)

    est = pd.concat([estatisticas(unidas, "T1_ms", args.limite), estatisticas(unidas, "T2_ms", args.limite)])
    if est.empty:
        sys.exit("Nenhuma medição do teste automático encontrada nos CSVs.")
    est.round(3).to_csv(saida / "estatisticas_latencia.csv", index=False)

    aparelhos = ", ".join(sorted({str(a) for a in unidas["aparelho_surdocego"].dropna()} |
                                 {str(a) for a in unidas["aparelho_comunicador"].dropna() if a}))
    (saida / "tabela_tcc.md").write_text(tabela_markdown(est, args.limite, aparelhos), encoding="utf-8")

    boxplot(unidas, "T1_ms", "T1 – da chegada da mensagem até a vibração (aparelho do Surdocego)",
            saida / "boxplot_T1.png", args.limite)
    boxplot(unidas, "T2_ms", "T2 – do envio até a confirmação de recebimento (aparelho do Comunicador)",
            saida / "boxplot_T2.png", None)

    pd.set_option("display.width", 160)
    print(est.round(1).to_string(index=False))
    print(f"\nArquivos gerados em: {saida.resolve()}")
    for medida in ("T1_ms", "T2_ms"):
        faltando = unidas[medida].isna().sum()
        if faltando and unidas[medida].notna().any():
            print(f"Aviso: {faltando} envio(s) sem {medida[:2]} (mensagem perdida ou teste interrompido).")


if __name__ == "__main__":
    main()
