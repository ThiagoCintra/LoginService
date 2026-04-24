#!/usr/bin/env python3
"""
Testes de Segurança (EHT - Ethical Hacking Tests) - LoginService
Realiza testes automatizados de vulnerabilidades contra o serviço de login.
"""

import requests
import json
import time
import base64
from datetime import datetime

BASE_URL = "http://localhost:8081/api/v1"
LOGIN_URL = f"{BASE_URL}/auth/login"
ME_URL = f"{BASE_URL}/auth/me"

results = []


def log(category, test_name, result, details="", severity="INFO", passed=True):
    record = {
        "categoria": category,
        "teste": test_name,
        "resultado": result,
        "detalhes": details,
        "severidade": severity,
        "passou": passed,
        "timestamp": datetime.now().strftime("%H:%M:%S"),
    }
    results.append(record)
    status = "✅ PASS" if passed else "❌ VULN"
    print(f"  [{status}] [{severity}] {test_name}: {result}")


def valid_login():
    """Obtém um token válido para uso nos testes."""
    r = requests.post(
        LOGIN_URL, json={"username": "Thiago", "password": "231299"}, timeout=5
    )
    if r.status_code == 200:
        return r.json().get("token")
    return None


# ─────────────────────────────────────────────────────────
# 1. AUTENTICAÇÃO E CONTROLE DE ACESSO
# ─────────────────────────────────────────────────────────
def test_auth_access_control():
    print("\n[1] Autenticação e Controle de Acesso")

    # 1.1 Acesso sem token
    r = requests.get(ME_URL, timeout=5)
    passed = r.status_code == 401
    log(
        "Autenticação",
        "Acesso /me sem token JWT",
        f"HTTP {r.status_code} (esperado 401)",
        "Endpoint protegido corretamente" if passed else "FALHA: endpoint não protegido",
        "ALTO" if not passed else "INFO",
        passed,
    )

    # 1.2 Token inválido/aleatório
    r = requests.get(
        ME_URL,
        headers={"Authorization": "Bearer tokeninvalidoaleatório123"},
        timeout=5,
    )
    passed = r.status_code == 401
    log(
        "Autenticação",
        "Token JWT completamente inválido",
        f"HTTP {r.status_code}",
        "Rejeita tokens malformados" if passed else "CRÍTICO: aceita token inválido",
        "CRÍTICO" if not passed else "INFO",
        passed,
    )

    # 1.3 Token expirado (forjado manualmente)
    header = (
        base64.urlsafe_b64encode(b'{"alg":"HS256","typ":"JWT"}').rstrip(b"=").decode()
    )
    payload = (
        base64.urlsafe_b64encode(
            b'{"sub":"Thiago","exp":1000000,"sessionId":"fake","role":"USER","contractService":false}'
        )
        .rstrip(b"=")
        .decode()
    )
    fake_token = f"{header}.{payload}.assinaturafalsa"
    r = requests.get(ME_URL, headers={"Authorization": f"Bearer {fake_token}"}, timeout=5)
    passed = r.status_code == 401
    log(
        "Autenticação",
        "Token JWT expirado (forjado)",
        f"HTTP {r.status_code}",
        "Rejeita tokens expirados" if passed else "CRÍTICO: aceita token expirado",
        "CRÍTICO" if not passed else "INFO",
        passed,
    )

    # 1.4 Ataque JWT Algorithm 'none'
    header_none = (
        base64.urlsafe_b64encode(b'{"alg":"none","typ":"JWT"}').rstrip(b"=").decode()
    )
    payload_none = (
        base64.urlsafe_b64encode(
            b'{"sub":"admin","exp":9999999999,"sessionId":"hacked","role":"ADMIN","contractService":true}'
        )
        .rstrip(b"=")
        .decode()
    )
    none_token = f"{header_none}.{payload_none}."
    r = requests.get(ME_URL, headers={"Authorization": f"Bearer {none_token}"}, timeout=5)
    passed = r.status_code == 401
    log(
        "Autenticação",
        "Ataque JWT Algorithm 'none'",
        f"HTTP {r.status_code}",
        "Rejeita alg=none" if passed else "CRÍTICO: vulnerável a JWT none attack",
        "CRÍTICO" if not passed else "INFO",
        passed,
    )

    # 1.5 Header Authorization sem prefixo Bearer
    token = valid_login()
    if token:
        r = requests.get(ME_URL, headers={"Authorization": token}, timeout=5)
        passed = r.status_code == 401
        log(
            "Autenticação",
            "Token sem prefixo 'Bearer'",
            f"HTTP {r.status_code}",
            "Exige prefixo Bearer" if passed else "AVISO: aceita token sem prefixo Bearer",
            "MÉDIO" if not passed else "INFO",
            passed,
        )


# ─────────────────────────────────────────────────────────
# 2. INJEÇÃO E VALIDAÇÃO DE ENTRADA
# ─────────────────────────────────────────────────────────
def test_injection():
    print("\n[2] Injeção e Validação de Entrada")

    sqli_payloads = [
        "' OR '1'='1",
        "' OR 1=1--",
        "admin'--",
        "' UNION SELECT 1,2,3--",
        "'; DROP TABLE users;--",
    ]
    for payload in sqli_payloads:
        r = requests.post(
            LOGIN_URL,
            json={"username": payload, "password": "qualquercoisa"},
            timeout=5,
        )
        passed = r.status_code in [400, 401, 403]
        log(
            "Injeção SQL",
            f"SQL Injection: {payload[:30]}",
            f"HTTP {r.status_code}",
            "Bloqueado corretamente" if passed else "CRÍTICO: possível SQL Injection",
            "CRÍTICO" if not passed else "BAIXO",
            passed,
        )
        time.sleep(0.1)

    # SQL Injection na senha
    r = requests.post(
        LOGIN_URL,
        json={"username": "Thiago", "password": "' OR '1'='1"},
        timeout=5,
    )
    passed = r.status_code in [400, 401, 403]
    log(
        "Injeção SQL",
        "SQL Injection na senha",
        f"HTTP {r.status_code}",
        "Bloqueado" if passed else "CRÍTICO: possível bypass via senha",
        "CRÍTICO" if not passed else "BAIXO",
        passed,
    )

    # XSS no body JSON
    xss_payloads = [
        "<script>alert(1)</script>",
        "javascript:alert(1)",
        "<img src=x onerror=alert(1)>",
    ]
    for payload in xss_payloads:
        r = requests.post(
            LOGIN_URL, json={"username": payload, "password": "test"}, timeout=5
        )
        passed = r.status_code in [400, 401, 403]
        log(
            "XSS",
            f"XSS payload: {payload[:30]}",
            f"HTTP {r.status_code}",
            "Não executado (API JSON)" if passed else "AVISO: resposta inesperada",
            "BAIXO",
            passed,
        )
        time.sleep(0.1)

    # JSON malformado
    r = requests.post(
        LOGIN_URL,
        data="{{invalid json{{",
        headers={"Content-Type": "application/json"},
        timeout=5,
    )
    passed = r.status_code in [400, 415, 422, 500]
    log(
        "Validação",
        "JSON malformado no body",
        f"HTTP {r.status_code}",
        "Retorna erro adequado" if passed else "AVISO: comportamento inesperado",
        "BAIXO",
        passed,
    )

    # Credenciais vazias
    r = requests.post(
        LOGIN_URL, json={"username": "", "password": ""}, timeout=5
    )
    passed = r.status_code in [400, 401, 403]
    log(
        "Validação",
        "Credenciais vazias",
        f"HTTP {r.status_code}",
        "Rejeita campos vazios" if passed else "AVISO: aceita credenciais vazias",
        "MÉDIO" if not passed else "BAIXO",
        passed,
    )

    # Body sem campos
    r = requests.post(LOGIN_URL, json={}, timeout=5)
    passed = r.status_code in [400, 401, 403]
    log(
        "Validação",
        "Body JSON sem campos obrigatórios",
        f"HTTP {r.status_code}",
        "Rejeita body vazio" if passed else "AVISO: aceita body vazio",
        "MÉDIO" if not passed else "BAIXO",
        passed,
    )

    # Payload muito grande
    big_payload = "A" * 100_000
    r = requests.post(
        LOGIN_URL, json={"username": big_payload, "password": "x"}, timeout=10
    )
    passed = r.status_code in [400, 413, 401, 403]
    log(
        "DoS Input",
        "Payload extremamente grande (100KB username)",
        f"HTTP {r.status_code}",
        "Tratado sem crash" if passed else "AVISO: resposta inesperada com payload grande",
        "MÉDIO" if not passed else "BAIXO",
        passed,
    )


# ─────────────────────────────────────────────────────────
# 3. FORÇA BRUTA E ENUMERAÇÃO
# ─────────────────────────────────────────────────────────
def test_brute_force():
    print("\n[3] Força Bruta e Enumeração")

    senhas = [
        "123456", "password", "admin", "letmein", "qwerty",
        "abc123", "000000", "111111", "test", "pass",
    ]
    bloqueado = False
    last_code = None
    start = time.time()
    for senha in senhas:
        r = requests.post(
            LOGIN_URL, json={"username": "Thiago", "password": senha}, timeout=5
        )
        last_code = r.status_code
        if r.status_code == 429:
            bloqueado = True
            break
        time.sleep(0.05)
    elapsed = time.time() - start
    log(
        "Força Bruta",
        "Rate limiting após múltiplas tentativas incorretas",
        f"Bloqueado: {'Sim' if bloqueado else 'Não'} | Último HTTP: {last_code} | {elapsed:.1f}s",
        "Rate limiting ativo" if bloqueado else "ALTO: sem rate limiting — vulnerável a força bruta",
        "ALTO" if not bloqueado else "INFO",
        bloqueado,
    )

    # Timing attack / enumeração
    t1_start = time.time()
    requests.post(
        LOGIN_URL, json={"username": "Thiago", "password": "senhaerrada"}, timeout=5
    )
    t1 = time.time() - t1_start

    t2_start = time.time()
    requests.post(
        LOGIN_URL,
        json={"username": "usuario_inexistente_xyzabc", "password": "senhaerrada"},
        timeout=5,
    )
    t2 = time.time() - t2_start

    diff = abs(t1 - t2)
    passed = diff < 0.5
    log(
        "Enumeração",
        "Timing attack — diferença entre usuário existente/inexistente",
        f"Existente: {t1:.3f}s | Inexistente: {t2:.3f}s | Diferença: {diff:.3f}s",
        "Tempo similar — resistente a timing attack" if passed else f"MÉDIO: diff {diff:.3f}s pode revelar usuários",
        "MÉDIO" if not passed else "BAIXO",
        passed,
    )

    # Enumeração por código HTTP
    r_exist = requests.post(
        LOGIN_URL, json={"username": "Thiago", "password": "errada"}, timeout=5
    )
    r_noexist = requests.post(
        LOGIN_URL, json={"username": "naoexiste_xyz", "password": "errada"}, timeout=5
    )
    passed = r_exist.status_code == r_noexist.status_code
    log(
        "Enumeração",
        "Código HTTP diferente para usuário existente vs inexistente",
        f"Existente: HTTP {r_exist.status_code} | Inexistente: HTTP {r_noexist.status_code}",
        "Mesmo código HTTP — não vaza existência de usuário" if passed else "MÉDIO: códigos diferentes permitem enumeração",
        "MÉDIO" if not passed else "INFO",
        passed,
    )


# ─────────────────────────────────────────────────────────
# 4. HEADERS DE SEGURANÇA
# ─────────────────────────────────────────────────────────
def test_security_headers():
    print("\n[4] Headers de Segurança HTTP")

    token = valid_login()
    headers_auth = {"Authorization": f"Bearer {token}"} if token else {}
    r = requests.get(ME_URL, headers=headers_auth, timeout=5)
    resp_headers = r.headers

    security_headers = {
        "X-Content-Type-Options": ("nosniff", "MÉDIO"),
        "X-Frame-Options": ("DENY ou SAMEORIGIN", "MÉDIO"),
        "Strict-Transport-Security": ("max-age=...", "ALTO"),
        "Content-Security-Policy": ("presente", "MÉDIO"),
        "X-XSS-Protection": ("1; mode=block", "BAIXO"),
        "Cache-Control": ("no-store", "MÉDIO"),
        "Referrer-Policy": ("presente", "BAIXO"),
    }

    for header, (expected, severity) in security_headers.items():
        present = header in resp_headers
        value = resp_headers.get(header, "AUSENTE")
        log(
            "Headers HTTP",
            f"Header '{header}'",
            f"Valor: {value}",
            f"Esperado: {expected}" if not present else f"Presente: {value}",
            severity if not present else "INFO",
            present,
        )

    # Exposição de versão do servidor
    server_header = resp_headers.get("Server", "")
    x_powered = resp_headers.get("X-Powered-By", "")
    passed = not bool(server_header or x_powered)
    log(
        "Headers HTTP",
        "Exposição de informações do servidor (Server / X-Powered-By)",
        f"Server: '{server_header}' | X-Powered-By: '{x_powered}'",
        "Não expõe versão do servidor" if passed else "BAIXO: expõe informações do servidor",
        "BAIXO" if not passed else "INFO",
        passed,
    )


# ─────────────────────────────────────────────────────────
# 5. EXPOSIÇÃO DE DADOS SENSÍVEIS
# ─────────────────────────────────────────────────────────
def test_sensitive_data():
    print("\n[5] Exposição de Dados Sensíveis")

    token = valid_login()

    if token:
        r = requests.get(ME_URL, headers={"Authorization": f"Bearer {token}"}, timeout=5)
        if r.status_code == 200:
            body = r.json()
            has_symkey = "symmetricKey" in body
            log(
                "Exposição de Dados",
                "Endpoint /me expõe symmetricKey no response",
                f"symmetricKey presente: {'SIM' if has_symkey else 'NÃO'} | campos: {list(body.keys())}",
                "CRÍTICO: chave simétrica exposta na API" if has_symkey else "Chave não exposta",
                "CRÍTICO" if has_symkey else "INFO",
                not has_symkey,
            )

            has_password = "password" in body
            log(
                "Exposição de Dados",
                "Endpoint /me expõe senha no response",
                f"password presente: {'SIM' if has_password else 'NÃO'}",
                "CRÍTICO: senha exposta" if has_password else "Senha não exposta",
                "CRÍTICO" if has_password else "INFO",
                not has_password,
            )

    # Resposta de login
    r = requests.post(
        LOGIN_URL, json={"username": "Thiago", "password": "231299"}, timeout=5
    )
    if r.status_code == 200:
        body = r.json()
        exposes_extra = any(k not in ["token", "accessToken", "jwt"] for k in body.keys())
        log(
            "Exposição de Dados",
            "Login expõe dados além do token JWT",
            f"Campos no response: {list(body.keys())}",
            "Apenas token retornado" if not exposes_extra else "Expõe campos extras",
            "BAIXO" if exposes_extra else "INFO",
            True,
        )

    # Stack trace
    r = requests.post(LOGIN_URL, json={"username": "x", "password": "x"}, timeout=5)
    body_text = r.text.lower()
    has_stacktrace = ("at com." in body_text) or ("exception" in body_text and "stack" in body_text)
    log(
        "Exposição de Dados",
        "Stack trace exposto em respostas de erro",
        "Stack trace presente" if has_stacktrace else "Stack trace não exposto",
        "MÉDIO: stack trace vaza estrutura interna" if has_stacktrace else "Protegido",
        "MÉDIO" if has_stacktrace else "INFO",
        not has_stacktrace,
    )

    # JWT claims com dados sensíveis
    if token:
        parts = token.split(".")
        if len(parts) >= 2:
            padded = parts[1] + "=="
            try:
                payload_data = json.loads(base64.urlsafe_b64decode(padded))
                has_sensitive = any(
                    k in payload_data for k in ["password", "symmetricKey", "secret"]
                )
                log(
                    "Exposição de Dados",
                    "Claims JWT expõem dados sensíveis",
                    f"Claims: {list(payload_data.keys())}",
                    "ALTO: dados sensíveis no JWT" if has_sensitive else "Claims adequados",
                    "ALTO" if has_sensitive else "INFO",
                    not has_sensitive,
                )
            except Exception:
                pass


# ─────────────────────────────────────────────────────────
# 6. CSRF E CORS
# ─────────────────────────────────────────────────────────
def test_csrf_cors():
    print("\n[6] CSRF e CORS")

    log(
        "CSRF",
        "CSRF Protection desabilitada no SecurityConfig",
        "csrf.disable() detectado via análise estática",
        "MÉDIO: CSRF desabilitado — aceitável para APIs REST stateless com JWT, porém deve ser documentado",
        "MÉDIO",
        False,
    )

    headers_cors = {
        "Origin": "https://malicioso.exemplo.com",
        "Access-Control-Request-Method": "POST",
    }
    r = requests.options(LOGIN_URL, headers=headers_cors, timeout=5)
    acao_header = r.headers.get("Access-Control-Allow-Origin", "")
    wildcard = acao_header == "*"
    log(
        "CORS",
        "CORS com Origin malicioso",
        f"Access-Control-Allow-Origin: '{acao_header}'",
        "ALTO: CORS wildcard (*) permite qualquer origem" if wildcard else
        ("CORS não configurado/sem resposta" if not acao_header else f"CORS: {acao_header}"),
        "ALTO" if wildcard else "INFO",
        not wildcard,
    )


# ─────────────────────────────────────────────────────────
# 7. CONFIGURAÇÃO E TRANSPORTE
# ─────────────────────────────────────────────────────────
def test_transport_config():
    print("\n[7] Configuração e Transporte")

    log(
        "Transporte",
        "HTTPS não habilitado (ambiente local sem TLS)",
        "Serviço rodando em HTTP na porta 8081",
        "ALTO: em produção DEVE usar HTTPS/TLS. Sem HTTPS, JWTs podem ser interceptados.",
        "ALTO",
        False,
    )

    log(
        "Configuração",
        "JWT secret hardcoded em application.yaml",
        "Secret: 'ChangeThisSecretKeyForProdUseAtLeast32Chars!'",
        "CRÍTICO: secret JWT hardcoded no fonte — usar variável de ambiente em produção",
        "CRÍTICO",
        False,
    )

    r = requests.get("http://localhost:8081/h2-console", timeout=5)
    h2_enabled = r.status_code not in [404, 403]
    log(
        "Configuração",
        "H2 Console Web habilitado",
        f"HTTP {r.status_code} no endpoint /h2-console",
        "ALTO: H2 console exposto — NUNCA habilitar em produção" if h2_enabled else "H2 console não acessível externamente",
        "ALTO" if h2_enabled else "BAIXO",
        not h2_enabled,
    )

    r = requests.get("http://localhost:8081/api/v1/actuator", timeout=5)
    actuator_open = r.status_code == 200
    log(
        "Configuração",
        "Spring Actuator exposto sem autenticação",
        f"HTTP {r.status_code}",
        "MÉDIO: actuator exposto — limitar em produção" if actuator_open else "Actuator requer autenticação",
        "MÉDIO" if actuator_open else "INFO",
        not actuator_open,
    )

    log(
        "Configuração",
        "Gerenciamento de sessão stateless (STATELESS)",
        "SessionCreationPolicy.STATELESS configurado",
        "Correto para API JWT — sem sessão server-side",
        "INFO",
        True,
    )


# ─────────────────────────────────────────────────────────
# EXECUÇÃO PRINCIPAL
# ─────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("=" * 70)
    print("  TESTES DE SEGURANÇA EHT - LoginService")
    print(f"  Data/Hora: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Alvo: {BASE_URL}")
    print("=" * 70)

    test_auth_access_control()
    test_injection()
    test_brute_force()
    test_security_headers()
    test_sensitive_data()
    test_csrf_cors()
    test_transport_config()

    total = len(results)
    vulns = [r for r in results if not r["passou"]]
    passed_tests = [r for r in results if r["passou"]]

    print("\n" + "=" * 70)
    print(f"  RESUMO: {len(passed_tests)}/{total} testes passaram | {len(vulns)} vulnerabilidades")
    print("=" * 70)

    with open("/tmp/eht_results.json", "w", encoding="utf-8") as f:
        json.dump(
            {
                "timestamp": datetime.now().isoformat(),
                "target": BASE_URL,
                "total": total,
                "passed": len(passed_tests),
                "vulnerabilities": len(vulns),
                "results": results,
            },
            f,
            ensure_ascii=False,
            indent=2,
        )

    print("\nResultados salvos em /tmp/eht_results.json")
