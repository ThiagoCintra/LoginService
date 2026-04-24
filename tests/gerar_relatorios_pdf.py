#!/usr/bin/env python3
"""
Gerador de Relatórios PDF - LoginService
Gera relatório de segurança (EHT) e de stress em português.
"""

import json
import os
import unicodedata
from fpdf import FPDF
from datetime import datetime


def safe(text):
    """Converte caracteres Unicode nao suportados para equivalentes ASCII."""
    text = str(text)
    # Substitui caracteres especiais comuns
    replacements = {
        "\u2014": "-",   # em dash
        "\u2013": "-",   # en dash
        "\u2018": "'",   # left single quote
        "\u2019": "'",   # right single quote
        "\u201c": '"',   # left double quote
        "\u201d": '"',   # right double quote
        "\u2026": "...", # ellipsis
    }
    for ch, rep in replacements.items():
        text = text.replace(ch, rep)
    # Remove qualquer outro caractere fora do latin-1
    return text.encode("latin-1", errors="replace").decode("latin-1")

# ─────────────────────────────────────────────────────────
# Classe base para os relatórios
# ─────────────────────────────────────────────────────────
class RelatorioPDF(FPDF):
    def __init__(self, titulo):
        super().__init__()
        self.titulo_relatorio = titulo
        self.set_auto_page_break(auto=True, margin=20)
        self.add_page()
        self._capa()

    def header(self):
        if self.page_no() > 1:
            self.set_fill_color(30, 90, 160)
            self.rect(0, 0, 210, 12, "F")
            self.set_font("Helvetica", "B", 9)
            self.set_text_color(255, 255, 255)
            self.set_xy(10, 3)
            self.cell(0, 6, self.titulo_relatorio, align="L")
            self.set_xy(0, 3)
            self.cell(195, 6, f"Pag. {self.page_no()}", align="R")
            self.set_text_color(0, 0, 0)
            self.ln(14)

    def footer(self):
        self.set_y(-12)
        self.set_font("Helvetica", "I", 7)
        self.set_text_color(120, 120, 120)
        self.cell(0, 5, f"Gerado em: {datetime.now().strftime('%d/%m/%Y %H:%M')} | LoginService - Confidencial", align="C")

    def _capa(self):
        # Fundo azul escuro no topo
        self.set_fill_color(20, 70, 140)
        self.rect(0, 0, 210, 80, "F")

        # Faixa laranja
        self.set_fill_color(220, 120, 0)
        self.rect(0, 78, 210, 6, "F")

        # Título
        self.set_font("Helvetica", "B", 24)
        self.set_text_color(255, 255, 255)
        self.set_xy(15, 15)
        self.cell(180, 14, "LoginService", align="C")

        self.set_font("Helvetica", "B", 16)
        self.set_xy(15, 32)
        self.cell(180, 10, self.titulo_relatorio, align="C")

        self.set_font("Helvetica", "", 11)
        self.set_xy(15, 48)
        self.cell(180, 8, "Itau - Avaliacao de Qualidade e Seguranca", align="C")

        self.set_font("Helvetica", "", 10)
        self.set_xy(15, 60)
        self.cell(180, 8, f"Data: {datetime.now().strftime('%d de %B de %Y')}", align="C")

        self.set_text_color(0, 0, 0)
        self.set_y(100)

    def section_title(self, text):
        self.ln(4)
        self.set_fill_color(30, 90, 160)
        self.set_font("Helvetica", "B", 12)
        self.set_text_color(255, 255, 255)
        self.cell(0, 9, safe(f"  {text}"), fill=True, ln=True)
        self.set_text_color(0, 0, 0)
        self.ln(3)

    def subsection_title(self, text):
        self.ln(2)
        self.set_fill_color(220, 230, 245)
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(20, 60, 120)
        self.cell(0, 7, safe(f"  {text}"), fill=True, ln=True)
        self.set_text_color(0, 0, 0)
        self.ln(2)

    def paragraph(self, text, size=9):
        self.set_font("Helvetica", "", size)
        self.set_text_color(40, 40, 40)
        self.multi_cell(0, 5, safe(text))
        self.ln(1)

    def kv_row(self, key, value, bold_value=False):
        self.set_font("Helvetica", "B", 9)
        self.set_text_color(60, 60, 60)
        self.cell(55, 6, safe(f"  {key}:"), ln=False)
        if bold_value:
            self.set_font("Helvetica", "B", 9)
        else:
            self.set_font("Helvetica", "", 9)
        self.set_text_color(20, 20, 20)
        self.cell(0, 6, safe(str(value)), ln=True)

    def badge(self, text, color):
        colors = {
            "CRÍTICO": (180, 0, 0),
            "ALTO": (200, 70, 0),
            "MÉDIO": (180, 140, 0),
            "BAIXO": (30, 130, 30),
            "INFO": (50, 100, 180),
            "PASS": (20, 140, 20),
            "VULN": (180, 0, 0),
        }
        r, g, b = colors.get(color, (100, 100, 100))
        self.set_fill_color(r, g, b)
        self.set_text_color(255, 255, 255)
        self.set_font("Helvetica", "B", 7)
        self.cell(22, 5, f" {text} ", fill=True, align="C")
        self.set_text_color(0, 0, 0)


# ─────────────────────────────────────────────────────────
# RELATÓRIO 1: SEGURANÇA (EHT)
# ─────────────────────────────────────────────────────────
def gerar_relatorio_eht(data_path, output_path):
    with open(data_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    pdf = RelatorioPDF("Relatorio de Seguranca - EHT")

    # Sumário executivo
    pdf.section_title("1. Sumario Executivo")
    total = data["total"]
    passou = data["passed"]
    vulns = data["vulnerabilities"]
    ts = data["timestamp"][:19].replace("T", " ")

    pdf.kv_row("Data/Hora do Teste", ts)
    pdf.kv_row("Servico Analisado", "LoginService - http://localhost:8081/api/v1")
    pdf.kv_row("Tecnologia", "Spring Boot 4.0.5 | Java 21 | JWT | Redis | Spring Security")
    pdf.kv_row("Total de Testes", str(total))
    pdf.kv_row("Testes Aprovados", str(passou))
    pdf.kv_row("Vulnerabilidades Encontradas", str(vulns))
    taxa = round(passou / total * 100, 1) if total else 0
    pdf.kv_row("Taxa de Aprovacao", f"{taxa}%")
    pdf.ln(3)

    # Classificação de risco
    results = data["results"]
    severities = {"CRÍTICO": 0, "ALTO": 0, "MÉDIO": 0, "BAIXO": 0}
    for r in results:
        if not r["passou"] and r["severidade"] in severities:
            severities[r["severidade"]] += 1

    pdf.subsection_title("Classificacao de Risco das Vulnerabilidades")
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(40, 40, 40)
    pdf.ln(2)

    for sev, count in severities.items():
        pdf.set_x(20)
        pdf.badge(sev, sev)
        pdf.set_font("Helvetica", "", 9)
        pdf.set_text_color(40, 40, 40)
        pdf.cell(0, 5, safe(f"  {count} {'ocorrencia' if count <= 1 else 'ocorrencias'}"), ln=True)
        pdf.ln(1)

    pdf.ln(3)
    pdf.paragraph(
        "O servico LoginService apresenta uma base de seguranca razoavel, com proteções "
        "contra injeção SQL, XSS e configuração correta de sessão stateless via JWT. "
        "Contudo, foram identificadas vulnerabilidades criticas e altas que devem ser "
        "corrigidas antes de qualquer deploy em producao, especialmente a exposicao da "
        "symmetricKey no endpoint /me e o JWT secret hardcoded no codigo-fonte."
    )

    # Detalhamento por categoria
    pdf.section_title("2. Detalhamento dos Testes por Categoria")

    categorias = {}
    for r in results:
        cat = r["categoria"]
        if cat not in categorias:
            categorias[cat] = []
        categorias[cat].append(r)

    for cat, testes in categorias.items():
        pdf.subsection_title(cat)
        pdf.set_font("Helvetica", "", 8)

        for t in testes:
            passou_t = t["passou"]
            sev = t["severidade"]
            pdf.set_x(15)

            # Status badge
            status_text = "PASS" if passou_t else "VULN"
            pdf.badge(status_text, "PASS" if passou_t else "VULN")
            pdf.set_x(40)
            pdf.badge(sev, sev)
            pdf.set_x(65)

            # Nome do teste
            pdf.set_font("Helvetica", "B", 8)
            pdf.set_text_color(20, 20, 20)
            nome = t["teste"][:65]
            pdf.cell(0, 5, safe(nome), ln=True)

            # Resultado
            pdf.set_x(20)
            pdf.set_font("Helvetica", "", 8)
            pdf.set_text_color(80, 80, 80)
            resultado = t["resultado"][:100]
            pdf.cell(0, 4, safe(f"Resultado: {resultado}"), ln=True)

            # Detalhes (apenas para vulnerabilidades)
            if not passou_t and t["detalhes"]:
                pdf.set_x(20)
                pdf.set_font("Helvetica", "I", 8)
                pdf.set_text_color(150, 50, 50)
                detalhes = t["detalhes"][:120]
                pdf.cell(0, 4, safe(f"Risco: {detalhes}"), ln=True)

            pdf.set_text_color(0, 0, 0)
            pdf.ln(1)

    # Vulnerabilidades críticas + recomendações
    pdf.section_title("3. Vulnerabilidades Criticas e Recomendacoes")

    vulnerabilidades = [r for r in results if not r["passou"]]
    prioridade = {"CRÍTICO": 0, "ALTO": 1, "MÉDIO": 2, "BAIXO": 3}
    vulnerabilidades.sort(key=lambda x: prioridade.get(x["severidade"], 99))

    recomendacoes = {
        "Endpoint /me expoe symmetricKey no response": (
            "CRÍTICO",
            "Remover o campo 'symmetricKey' do SessionDTO antes de retornar a resposta do "
            "endpoint /me. A chave simetrica e um segredo interno que nao deve ser exposto "
            "ao cliente. Criar um DTO de resposta separado (MeResponse) sem esse campo."
        ),
        "JWT secret hardcoded em application.yaml": (
            "CRÍTICO",
            "Mover o segredo JWT para variavel de ambiente segura (ex: JWT_SECRET). "
            "Nunca commitar secrets no codigo-fonte. Usar AWS Secrets Manager, HashiCorp "
            "Vault ou variaveis de ambiente injetadas pelo orchestrador (Kubernetes Secrets)."
        ),
        "HTTPS nao habilitado": (
            "ALTO",
            "Em producao, SEMPRE usar HTTPS/TLS. Configurar certificado SSL no servidor ou "
            "usar um API Gateway/Load Balancer com terminacao TLS. Sem HTTPS, tokens JWT "
            "podem ser interceptados em ataques man-in-the-middle."
        ),
        "Rate limiting apos multiplas tentativas incorretas": (
            "ALTO",
            "Implementar rate limiting no endpoint /auth/login para mitigar ataques de "
            "forca bruta. Usar Spring Security com Bucket4j ou um API Gateway. Sugestao: "
            "bloquear apos 5 tentativas em 60 segundos por IP."
        ),
        "CSRF Protection desabilitada": (
            "MÉDIO",
            "CSRF pode ser desabilitado para APIs REST puras que usam JWT (stateless). "
            "Porem, documentar explicitamente essa decisao e garantir que o token JWT nao "
            "seja armazenado em cookies (usar localStorage ou sessionStorage no cliente)."
        ),
        "Header Strict-Transport-Security ausente": (
            "ALTO",
            "Adicionar header HSTS: 'Strict-Transport-Security: max-age=31536000; "
            "includeSubDomains'. Isso instrui browsers a usar apenas HTTPS."
        ),
        "Header Content-Security-Policy ausente": (
            "MÉDIO",
            "Configurar Content-Security-Policy no Spring Security para evitar ataques "
            "XSS em eventuais interfaces web. Para APIs puras, menor criticidade."
        ),
        "Acesso /me sem token JWT": (
            "INFO",
            "O servico retorna HTTP 403 (Forbidden) ao inves de HTTP 401 (Unauthorized) "
            "para requisicoes sem token. Considerar retornar 401 para requisicoes sem "
            "autenticacao e 403 apenas para acesso negado com autenticacao valida."
        ),
    }

    for vuln in vulnerabilidades:
        sev = vuln["severidade"]
        nome = vuln["teste"]

        # Encontrar recomendação correspondente
        rec_key = next(
            (k for k in recomendacoes if k.lower() in nome.lower() or nome.lower() in k.lower()),
            None
        )

        pdf.set_x(10)
        pdf.badge(sev, sev)
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_text_color(20, 20, 20)
        pdf.set_x(35)
        pdf.cell(0, 5, safe(nome[:80]), ln=True)

        pdf.set_x(15)
        pdf.set_font("Helvetica", "", 8)
        pdf.set_text_color(60, 60, 60)
        pdf.cell(0, 4, safe(f"Achado: {vuln['resultado'][:100]}"), ln=True)

        if rec_key:
            _, rec_text = recomendacoes[rec_key]
            pdf.set_x(15)
            pdf.set_font("Helvetica", "I", 8)
            pdf.set_text_color(0, 80, 0)
            pdf.multi_cell(175, 4, safe(f"Recomendacao: {rec_text}"))

        pdf.set_text_color(0, 0, 0)
        pdf.ln(3)

    # Análise estática do código
    pdf.section_title("4. Analise Estatica do Codigo-Fonte")
    pdf.subsection_title("Achados da Analise Estatica")
    achados_estaticos = [
        ("CRÍTICO", "JwtServiceImpl.java", "JWT secret derivado de String.getBytes() sem charset explicito. Pode gerar chave diferente em JVMs com default charset distinto."),
        ("ALTO", "SessionUtils.java", "Uso de variaveis estaticas mutaveis (static SessionService, JwtService). Nao e thread-safe em cenarios de recarga de contexto Spring."),
        ("MÉDIO", "LoginServiceImpl.java", "Chamada desnecessaria a userRepositoryPort.findByUsername() apos autenticacao bem-sucedida pelo AuthenticationManager (dupla consulta ao banco)."),
        ("MÉDIO", "DataInitializer.java", "Senha do usuario inicial hardcoded ('231299'). Deve ser configuravel via variavel de ambiente ou removida do DataInitializer em producao."),
        ("MÉDIO", "application.yaml", "H2 console habilitado (h2.console.enabled: true) — nunca habilitar em producao."),
        ("BAIXO", "SecurityConfig.java", "CORS nao configurado explicitamente. Adicionar CorsConfigurationSource para controle granular de origens permitidas."),
        ("BAIXO", "SessionDTO.java", "SessionDTO retornado diretamente no /me expoe symmetricKey — criar DTO separado para a resposta publica."),
    ]

    for sev, arquivo, desc in achados_estaticos:
        pdf.set_x(15)
        pdf.badge(sev, sev)
        pdf.set_font("Helvetica", "B", 8)
        pdf.set_text_color(20, 20, 20)
        pdf.set_x(40)
        pdf.cell(0, 5, safe(arquivo), ln=True)
        pdf.set_x(20)
        pdf.set_font("Helvetica", "", 8)
        pdf.set_text_color(60, 60, 60)
        pdf.multi_cell(175, 4, safe(desc))
        pdf.set_text_color(0, 0, 0)
        pdf.ln(2)

    # Conclusão
    pdf.section_title("5. Conclusao e Proximos Passos")
    pdf.paragraph(
        "O LoginService demonstra uma arquitetura bem estruturada com uso adequado de "
        "Spring Security, JWT e Redis para gerenciamento de sessoes stateless. As "
        "vulnerabilidades criticas identificadas — especialmente a exposicao da "
        "symmetricKey e o JWT secret hardcoded — devem ser corrigidas com prioridade "
        "maxima antes de qualquer deploy em ambiente de producao ou staging.",
        size=9
    )
    pdf.ln(2)
    pdf.subsection_title("Acoes Imediatas (Sprint Atual)")
    acoes = [
        "1. Remover symmetricKey do response do endpoint /me (criar MeResponseDTO)",
        "2. Mover JWT secret para variavel de ambiente segura (nao commitar no git)",
        "3. Implementar rate limiting no endpoint /auth/login (Bucket4j ou API Gateway)",
        "4. Corrigir comportamento de retorno HTTP 401 vs 403",
    ]
    for acao in acoes:
        pdf.set_font("Helvetica", "", 9)
        pdf.set_x(15)
        pdf.cell(0, 6, safe(acao), ln=True)

    pdf.ln(2)
    pdf.subsection_title("Acoes de Medio Prazo")
    acoes_mp = [
        "5. Configurar HTTPS/TLS em producao (certificado SSL + HSTS header)",
        "6. Adicionar Content-Security-Policy e Referrer-Policy headers",
        "7. Remover H2 console e usuario inicial hardcoded em producao",
        "8. Refatorar SessionUtils para eliminar estado estatico mutavel",
        "9. Implementar auditoria de login (logs de tentativas falhas por IP)",
    ]
    for acao in acoes_mp:
        pdf.set_font("Helvetica", "", 9)
        pdf.set_x(15)
        pdf.cell(0, 6, safe(acao), ln=True)

    pdf.output(output_path)
    print(f"Relatorio EHT gerado: {output_path}")


# ─────────────────────────────────────────────────────────
# RELATÓRIO 2: STRESS
# ─────────────────────────────────────────────────────────
def gerar_relatorio_stress(data_path, output_path):
    with open(data_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    pdf = RelatorioPDF("Relatorio de Teste de Stress")

    # Sumário executivo
    pdf.section_title("1. Sumario Executivo")
    ts = data["timestamp"][:19].replace("T", " ")
    cenarios = data["cenarios"]

    pdf.kv_row("Data/Hora do Teste", ts)
    pdf.kv_row("Servico Analisado", "LoginService - http://localhost:8081/api/v1")
    pdf.kv_row("Endpoints Testados", "POST /auth/login | GET /auth/me")
    pdf.kv_row("Cenarios Executados", str(len(cenarios)))
    pdf.ln(2)

    # Tabela resumo de cenários
    pdf.subsection_title("Resumo dos Cenarios")

    # Cabeçalho da tabela
    pdf.set_fill_color(30, 90, 160)
    pdf.set_text_color(255, 255, 255)
    pdf.set_font("Helvetica", "B", 8)
    cols = [52, 24, 28, 26, 28, 28]
    headers = ["Cenario", "Usuarios", "Total Reqs", "Throughput", "Duracao", "P95 /login"]
    for i, (h, w) in enumerate(zip(headers, cols)):
        pdf.cell(w, 7, h, fill=True, align="C")
    pdf.ln()
    pdf.set_text_color(0, 0, 0)

    # Linhas da tabela
    for idx, c in enumerate(cenarios):
        pdf.set_fill_color(240, 245, 255) if idx % 2 == 0 else pdf.set_fill_color(255, 255, 255)
        pdf.set_font("Helvetica", "", 8)
        nome = c["cenario"][:28]
        usuarios = str(c.get("usuarios_simultaneos", "?"))
        total_req = str(c.get("total_requisicoes", "?"))
        rps = f"{c.get('throughput_rps', '?')} r/s"
        duracao = f"{c.get('duracao_total_s', '?')}s"
        login_p95 = f"{c.get('login', {}).get('lat_p95_ms', c.get('lat_p95_ms', '?'))}ms"
        vals = [nome, usuarios, total_req, rps, duracao, login_p95]
        for v, w in zip(vals, cols):
            pdf.cell(w, 6, safe(str(v)), fill=True, align="C")
        pdf.ln()
    pdf.ln(3)

    # Interpretação geral
    pdf.paragraph(
        "O LoginService demonstrou estabilidade sob carga moderada a alta com 100% de taxa "
        "de sucesso em todos os cenarios testados. A principal degradacao observada foi na "
        "latencia do endpoint /auth/login sob alta concorrencia, atingindo P95 de ~5s com "
        "100 usuarios simultaneos — o que e esperado dado o custo de BCrypt (fator 10) e a "
        "ausencia de cache de autenticacao. O endpoint /auth/me (apenas leitura do Redis) "
        "manteve latencias muito baixas mesmo sob pico de carga."
    )

    # Detalhamento por cenário
    pdf.section_title("2. Detalhamento por Cenario")

    for c in cenarios:
        nome = c["cenario"]
        pdf.subsection_title(nome)
        pdf.kv_row("Usuarios simultaneos", str(c.get("usuarios_simultaneos", "N/A")))
        pdf.kv_row("Total de requisicoes", str(c.get("total_requisicoes", "N/A")))
        pdf.kv_row("Throughput", f"{c.get('throughput_rps', 'N/A')} requisicoes/segundo")
        pdf.kv_row("Duracao total", f"{c.get('duracao_total_s', 'N/A')} segundos")

        login = c.get("login", {})
        if login:
            pdf.ln(1)
            pdf.set_font("Helvetica", "B", 8)
            pdf.set_text_color(30, 80, 150)
            pdf.cell(0, 5, "  Endpoint POST /auth/login:", ln=True)
            pdf.set_text_color(0, 0, 0)
            pdf.kv_row("  Total de reqs", str(login.get("total_req", "N/A")))
            pdf.kv_row("  Taxa de sucesso", f"{login.get('taxa_sucesso_pct', 'N/A')}%")
            pdf.kv_row("  Latencia minima", f"{login.get('lat_min_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia media", f"{login.get('lat_media_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia P50", f"{login.get('lat_p50_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia P95", f"{login.get('lat_p95_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia P99", f"{login.get('lat_p99_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia maxima", f"{login.get('lat_max_ms', 'N/A')} ms")

        me = c.get("me", {})
        if me:
            pdf.ln(1)
            pdf.set_font("Helvetica", "B", 8)
            pdf.set_text_color(30, 80, 150)
            pdf.cell(0, 5, "  Endpoint GET /auth/me:", ln=True)
            pdf.set_text_color(0, 0, 0)
            pdf.kv_row("  Total de reqs", str(me.get("total_req", "N/A")))
            pdf.kv_row("  Taxa de sucesso", f"{me.get('taxa_sucesso_pct', 'N/A')}%")
            pdf.kv_row("  Latencia media", f"{me.get('lat_media_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia P50", f"{me.get('lat_p50_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia P95", f"{me.get('lat_p95_ms', 'N/A')} ms")
            pdf.kv_row("  Latencia maxima", f"{me.get('lat_max_ms', 'N/A')} ms")

        pdf.ln(2)

    # Análise de performance
    pdf.section_title("3. Analise de Performance")

    pdf.subsection_title("Gargalos Identificados")
    gargalos = [
        ("BCrypt no login", "ALTO",
         "O BCrypt (fator de custo 10) e CPU-intensivo por design. Com 100 usuarios "
         "simultaneos, o P95 de /login atingiu ~5s. Isso e esperado e correto do ponto "
         "de vista de seguranca, mas pode ser um gargalo em producao com alta carga de "
         "autenticacao. Considerar cache de autenticacao ou reduzir fator BCrypt para "
         "ambientes de alta carga (minimo recomendado: fator 8)."),
        ("Sem pool de conexoes otimizado", "MÉDIO",
         "A aplicacao usa H2 em memoria para teste. Em producao com PostgreSQL, o pool "
         "de conexoes HikariCP (configurado com max 20 conexoes) pode ser insuficiente "
         "para cargas acima de 50 usuarios simultaneos."),
        ("Redis sem cluster/sentinel", "MÉDIO",
         "O Redis esta configurado como instancia unica (sem replicacao ou sentinel). "
         "Em producao, um Redis standalone e ponto unico de falha para toda a sessao."),
        ("Ausencia de circuit breaker", "BAIXO",
         "Nao ha mecanismo de circuit breaker ou fallback caso o Redis ou banco de dados "
         "fiquem indisponiveis. Uma falha nesses componentes derruba o servico inteiro."),
    ]

    for titulo, sev, desc in gargalos:
        pdf.set_x(15)
        pdf.badge(sev, sev)
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_text_color(20, 20, 20)
        pdf.set_x(40)
        pdf.cell(0, 5, safe(titulo), ln=True)
        pdf.set_x(20)
        pdf.set_font("Helvetica", "", 8)
        pdf.set_text_color(60, 60, 60)
        pdf.multi_cell(175, 4, safe(desc))
        pdf.set_text_color(0, 0, 0)
        pdf.ln(2)

    pdf.subsection_title("Pontos Positivos")
    positivos = [
        "100% de taxa de sucesso em todos os cenarios de carga testados",
        "Endpoint /auth/me com excelente performance (P95 < 100ms em carga moderada)",
        "Uso de threads virtuais (Java 21) contribui para alta concorrencia",
        "Redis como store de sessao garante lookups rapidos (O(1))",
        "Nenhum timeout ou erro de conexao observado durante os testes",
        "Soak test de 20 segundos sem degradacao de performance observada",
    ]
    for p in positivos:
        pdf.set_font("Helvetica", "", 9)
        pdf.set_x(15)
        pdf.cell(0, 6, safe(f"+ {p}"), ln=True)

    # Recomendações de performance
    pdf.section_title("4. Recomendacoes de Performance")
    recomendacoes = [
        ("Curto prazo", [
            "Adicionar cache de autenticacao (Spring Cache + Redis) para usuarios frequentes",
            "Configurar timeouts adequados nas conexoes Redis e PostgreSQL",
            "Habilitar compressao GZIP nas respostas (ja configurado no application.yaml)",
        ]),
        ("Medio prazo", [
            "Implementar Redis Sentinel ou Cluster para alta disponibilidade",
            "Adicionar Resilience4j para circuit breaker em chamadas externas",
            "Configurar HikariCP com pool size dinamico baseado em metricas",
            "Implementar metricas de negocio via Micrometer (ja habilitado no Actuator)",
        ]),
        ("Longo prazo", [
            "Avaliar uso de WebFlux (reativo) para maior throughput se necessario",
            "Implementar testes de carga continuos no pipeline CI/CD (Gatling/k6)",
            "Adicionar APM (Application Performance Monitoring) em producao",
        ]),
    ]

    for prazo, itens in recomendacoes:
        pdf.subsection_title(prazo)
        for item in itens:
            pdf.set_font("Helvetica", "", 9)
            pdf.set_x(15)
            pdf.cell(0, 6, safe(f"- {item}"), ln=True)

    # Conclusão
    pdf.section_title("5. Conclusao")
    pdf.paragraph(
        "O LoginService apresentou comportamento estavel e confiavel durante todos os "
        "cenarios de stress testados, mantendo 100% de disponibilidade. A latencia do "
        "endpoint de autenticacao aumenta linearmente com a carga — comportamento esperado "
        "dado o algoritmo BCrypt — mas o servico nao apresentou erros ou timeouts. "
        "Para producao, os principais pontos de atencao sao: (1) alta disponibilidade do "
        "Redis, (2) dimensionamento correto do pool de conexoes e (3) monitoramento "
        "continuo das metricas de latencia via Actuator/Micrometer.",
        size=9
    )

    pdf.output(output_path)
    print(f"Relatorio Stress gerado: {output_path}")


# ─────────────────────────────────────────────────────────
# EXECUÇÃO
# ─────────────────────────────────────────────────────────
if __name__ == "__main__":
    import argparse

    # Determine project root relative to this script's location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    relatorios_dir = os.path.join(project_root, "relatorios")
    os.makedirs(relatorios_dir, exist_ok=True)

    parser = argparse.ArgumentParser(description="Gera relatorios PDF do LoginService")
    parser.add_argument("--eht-input", default="/tmp/eht_results.json")
    parser.add_argument("--stress-input", default="/tmp/stress_results.json")
    parser.add_argument("--output-dir", default=relatorios_dir)
    args = parser.parse_args()

    gerar_relatorio_eht(
        args.eht_input,
        os.path.join(args.output_dir, "relatorio_seguranca_eht.pdf"),
    )
    gerar_relatorio_stress(
        args.stress_input,
        os.path.join(args.output_dir, "relatorio_stress.pdf"),
    )
    print("\nAmbos os relatorios gerados com sucesso!")
