#!/usr/bin/env python3
"""
Teste de Stress - LoginService
Simula múltiplos usuários realizando chamadas simultâneas para avaliar
desempenho e estabilidade sob carga.
"""

import requests
import threading
import time
import json
import os
import statistics
from datetime import datetime
from collections import defaultdict

BASE_URL = os.environ.get("LOGIN_BASE_URL", "http://localhost:8081/api/v1")
LOGIN_URL = f"{BASE_URL}/auth/login"
ME_URL = f"{BASE_URL}/auth/me"

CREDENTIALS = {
    "username": os.environ.get("TEST_USERNAME", "Thiago"),
    "password": os.environ.get("TEST_PASSWORD", "231299"),
}

# ─────────────────────────────────────────────────────────
# Coletor de métricas thread-safe
# ─────────────────────────────────────────────────────────
lock = threading.Lock()
metrics = {
    "login": defaultdict(list),
    "me": defaultdict(list),
}


def record(endpoint, status_code, latency_ms, error=None):
    with lock:
        metrics[endpoint]["latencias"].append(latency_ms)
        metrics[endpoint]["status"].append(status_code)
        if error:
            metrics[endpoint]["erros"].append(error)


# ─────────────────────────────────────────────────────────
# Funções de requisição
# ─────────────────────────────────────────────────────────
def do_login():
    start = time.time()
    try:
        r = requests.post(LOGIN_URL, json=CREDENTIALS, timeout=10)
        elapsed = (time.time() - start) * 1000
        record("login", r.status_code, elapsed)
        if r.status_code == 200:
            return r.json().get("token")
        return None
    except requests.exceptions.Timeout:
        elapsed = (time.time() - start) * 1000
        record("login", 0, elapsed, "TIMEOUT")
        return None
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        record("login", 0, elapsed, str(e))
        return None


def do_me(token):
    start = time.time()
    try:
        r = requests.get(
            ME_URL, headers={"Authorization": f"Bearer {token}"}, timeout=10
        )
        elapsed = (time.time() - start) * 1000
        record("me", r.status_code, elapsed)
        return r.status_code
    except requests.exceptions.Timeout:
        elapsed = (time.time() - start) * 1000
        record("me", 0, elapsed, "TIMEOUT")
        return 0
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        record("me", 0, elapsed, str(e))
        return 0


def user_session(user_id, iterations=5):
    """Simula um usuário completo: login + N chamadas /me."""
    token = do_login()
    if token:
        for _ in range(iterations):
            do_me(token)
            time.sleep(0.05)


# ─────────────────────────────────────────────────────────
# Cenários de carga
# ─────────────────────────────────────────────────────────
cenarios_resultados = []


def run_scenario(name, users, iterations_per_user=3):
    """Executa um cenário com N usuários simultâneos."""
    global metrics
    metrics = {"login": defaultdict(list), "me": defaultdict(list)}

    threads = []
    start_total = time.time()

    for i in range(users):
        t = threading.Thread(
            target=user_session, args=(i, iterations_per_user), daemon=True
        )
        threads.append(t)

    # Inicia todas as threads ao mesmo tempo
    for t in threads:
        t.start()

    for t in threads:
        t.join(timeout=60)

    elapsed_total = time.time() - start_total

    # Calcula estatísticas
    def stats(endpoint):
        lats = metrics[endpoint]["latencias"]
        statuses = metrics[endpoint]["status"]
        erros = metrics[endpoint]["erros"]
        if not lats:
            return {}
        ok = sum(1 for s in statuses if s == 200)
        return {
            "total_req": len(lats),
            "sucesso": ok,
            "falhas": len(lats) - ok,
            "taxa_sucesso_pct": round(ok / len(lats) * 100, 1) if lats else 0,
            "lat_min_ms": round(min(lats), 1),
            "lat_max_ms": round(max(lats), 1),
            "lat_media_ms": round(statistics.mean(lats), 1),
            "lat_p50_ms": round(statistics.median(lats), 1),
            "lat_p95_ms": round(
                sorted(lats)[int(len(lats) * 0.95)] if len(lats) > 1 else lats[0], 1
            ),
            "lat_p99_ms": round(
                sorted(lats)[int(len(lats) * 0.99)] if len(lats) > 1 else lats[0], 1
            ),
            "erros": list(set(erros)),
        }

    login_stats = stats("login")
    me_stats = stats("me")

    total_req = login_stats.get("total_req", 0) + me_stats.get("total_req", 0)
    rps = round(total_req / elapsed_total, 1) if elapsed_total > 0 else 0

    resultado = {
        "cenario": name,
        "usuarios_simultaneos": users,
        "iteracoes_por_usuario": iterations_per_user,
        "duracao_total_s": round(elapsed_total, 2),
        "total_requisicoes": total_req,
        "throughput_rps": rps,
        "login": login_stats,
        "me": me_stats,
    }

    cenarios_resultados.append(resultado)

    # Exibe resultado
    print(f"\n  {'─'*60}")
    print(f"  Cenário: {name}")
    print(f"  Usuários simultâneos: {users} | Iterações/usuário: {iterations_per_user}")
    print(f"  Duração total: {elapsed_total:.2f}s | Throughput: {rps} req/s")
    if login_stats:
        print(
            f"  /login  → {login_stats['total_req']} reqs | "
            f"Sucesso: {login_stats['taxa_sucesso_pct']}% | "
            f"P50: {login_stats['lat_p50_ms']}ms | "
            f"P95: {login_stats['lat_p95_ms']}ms | "
            f"Max: {login_stats['lat_max_ms']}ms"
        )
    if me_stats:
        print(
            f"  /me     → {me_stats['total_req']} reqs | "
            f"Sucesso: {me_stats['taxa_sucesso_pct']}% | "
            f"P50: {me_stats['lat_p50_ms']}ms | "
            f"P95: {me_stats['lat_p95_ms']}ms | "
            f"Max: {me_stats['lat_max_ms']}ms"
        )

    return resultado


# ─────────────────────────────────────────────────────────
# Teste de Soak (duração prolongada)
# ─────────────────────────────────────────────────────────
def run_soak_test(duration_seconds=30, concurrent_users=5):
    """Mantém carga constante por um período para detectar memory leaks."""
    global metrics
    metrics = {"login": defaultdict(list), "me": defaultdict(list)}

    print(f"\n  [Soak Test] {concurrent_users} usuários por {duration_seconds}s...")
    stop_event = threading.Event()
    req_count = [0]

    def worker():
        while not stop_event.is_set():
            token = do_login()
            if token:
                do_me(token)
                with lock:
                    req_count[0] += 1
            time.sleep(0.1)

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(concurrent_users)]
    start = time.time()
    for t in threads:
        t.start()

    time.sleep(duration_seconds)
    stop_event.set()
    for t in threads:
        t.join(timeout=5)

    elapsed = time.time() - start
    lats = metrics["login"]["latencias"] + metrics["me"]["latencias"]
    rps = round(len(lats) / elapsed, 1)

    resultado = {
        "cenario": f"Soak Test ({duration_seconds}s)",
        "usuarios_simultaneos": concurrent_users,
        "duracao_total_s": round(elapsed, 2),
        "total_requisicoes": len(lats),
        "throughput_rps": rps,
        "lat_media_ms": round(statistics.mean(lats), 1) if lats else 0,
        "lat_p95_ms": round(sorted(lats)[int(len(lats) * 0.95)], 1) if len(lats) > 1 else 0,
        "lat_max_ms": round(max(lats), 1) if lats else 0,
    }
    cenarios_resultados.append(resultado)
    print(
        f"  Duração: {elapsed:.1f}s | Reqs: {len(lats)} | "
        f"Throughput: {rps} req/s | P95: {resultado['lat_p95_ms']}ms"
    )
    return resultado


# ─────────────────────────────────────────────────────────
# EXECUÇÃO PRINCIPAL
# ─────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("=" * 70)
    print("  TESTE DE STRESS - LoginService")
    print(f"  Data/Hora: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Alvo: {BASE_URL}")
    print("=" * 70)

    # Aquecimento
    print("\n[Aquecimento] Verificando conectividade...")
    token = do_login()
    if not token:
        print("  ERRO: Não foi possível obter token. Verifique se o serviço está no ar.")
        exit(1)
    print(f"  Login bem-sucedido. Token obtido.")

    # Cenário 1: Carga leve
    run_scenario("Carga Leve", users=5, iterations_per_user=3)
    time.sleep(1)

    # Cenário 2: Carga média
    run_scenario("Carga Média", users=20, iterations_per_user=5)
    time.sleep(1)

    # Cenário 3: Carga alta
    run_scenario("Carga Alta", users=50, iterations_per_user=5)
    time.sleep(1)

    # Cenário 4: Pico de carga (spike)
    run_scenario("Pico (Spike)", users=100, iterations_per_user=3)
    time.sleep(1)

    # Cenário 5: Soak test
    run_soak_test(duration_seconds=20, concurrent_users=10)

    # Resumo
    print("\n" + "=" * 70)
    print("  RESUMO GERAL DOS CENÁRIOS")
    print("=" * 70)
    for r in cenarios_resultados:
        name = r["cenario"]
        users = r.get("usuarios_simultaneos", "?")
        rps = r.get("throughput_rps", "?")
        total = r.get("total_requisicoes", "?")
        duracao = r.get("duracao_total_s", "?")
        print(f"  {name:30s} | Usuários: {str(users):>4} | Reqs: {str(total):>5} | "
              f"{rps} req/s | {duracao}s")

    # Salva resultados em JSON
    output = {
        "timestamp": datetime.now().isoformat(),
        "target": BASE_URL,
        "cenarios": cenarios_resultados,
    }
    with open("/tmp/stress_results.json", "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print("\nResultados salvos em /tmp/stress_results.json")
