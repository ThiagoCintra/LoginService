# LoginService

Serviço de autenticação baseado em **Spring Boot 4 + Java 21**, utilizando **JWT** para autenticação stateless e **Redis** como store de sessões.

---

## Sumário

- [Como Executar a Aplicação](#como-executar-a-aplicação)
- [Desenho de Solução](#desenho-de-solução)
- [Como Instalar o Redis via Docker](#como-instalar-o-redis-via-docker)
- [Relatórios e Avaliações](#relatórios-e-avaliações)
- [Tecnologias](#tecnologias)

---

## Como Executar a Aplicação

### Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ (ou use `./mvnw` incluso no projeto) |
| Redis | 7+ (via Docker, ver seção abaixo) |
| Docker | 20+ (opcional, mas recomendado) |

### 1. Clonar o repositório

```bash
git clone https://github.com/ThiagoCintra/LoginService.git
cd LoginService
```

### 2. Configurar variáveis de ambiente

Copie o arquivo de exemplo e ajuste conforme necessário:

```bash
cp .env.example .env
```

Conteúdo do `.env` (valores padrão para desenvolvimento local):

```env
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

SPRING_DATASOURCE_URL=jdbc:h2:mem:alunos_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=

JWT_SECRET=ChangeThisSecretKeyForDev_UseYourOwnInProd_AtLeast32Chars!
JWT_EXPIRATION_MS=300000

SERVER_PORT=8081
SERVER_SERVLET_CONTEXT_PATH=/api/v1
```

> ⚠️ **Atenção:** Nunca commite secrets reais no repositório. Em produção, injete o `JWT_SECRET` via variável de ambiente segura (Kubernetes Secrets, AWS Secrets Manager, HashiCorp Vault).

### 3. Subir o Redis com Docker

```bash
docker-compose up -d
```

Isso sobe o Redis na porta `6379`. Aguarde o healthcheck passar:

```bash
docker-compose ps   # Status deve ser "healthy"
```

### 4. Compilar e executar a aplicação

**Com Maven Wrapper (recomendado):**

```bash
./mvnw spring-boot:run
```

**Ou compilando o JAR primeiro:**

```bash
./mvnw clean package -DskipTests
java -jar target/login-0.0.1-SNAPSHOT.jar
```

**No Windows:**

```cmd
mvnw.cmd spring-boot:run
```

### 5. Verificar se a aplicação subiu

A aplicação estará disponível em: `http://localhost:8081/api/v1`

```bash
curl http://localhost:8081/api/v1/actuator/health
# Resposta esperada: {"status":"UP"}
```

### Endpoints disponíveis

| Método | Endpoint | Descrição | Autenticação |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Autenticar usuário e obter JWT | Não |
| `GET` | `/api/v1/auth/me` | Retornar dados da sessão atual | Bearer JWT |

**Exemplo de login:**

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "Thiago", "password": "231299"}'
```

Resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Exemplo de /me:**

```bash
curl http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta:

```json
{
  "sessionId": "uuid-da-sessao",
  "username": "Thiago",
  "contractService": false,
  "role": "USER"
}
```

### Console H2 (banco em memória — apenas DEV)

Disponível em: `http://localhost:8081/api/v1/h2-console`

- **JDBC URL:** `jdbc:h2:mem:alunos_db`
- **Usuário:** `sa`
- **Senha:** *(vazio)*

---

## Desenho de Solução

### Visão Geral

```
┌──────────────────────┐          JWT + Redis         ┌───────────────────┐
│    LoginService      │ ──────────────────────────▶  │  Redis (sessões)  │
│    :8081/api/v1      │ ◀──────────────────────────  │  :6379            │
└──────────────────────┘                              └───────────────────┘
          │
          │  GET /auth/me  (valida sessão pelo JWT + Redis)
          ▼
┌──────────────────────┐
│    PixService        │  Chama /me para validar o usuário antes de processar PIX
│                      │  ──▶  SQN (publica evento de transação)
└──────────────────────┘
          │
          │  Publica evento (fila/tópico)
          ▼
┌──────────────────────┐
│    GameService       │  Consome os eventos publicados pelo PixService
└──────────────────────┘
```

### Fluxo de Autenticação

```
Cliente                LoginService              Redis
   │                        │                      │
   │── POST /auth/login ──▶ │                      │
   │                        │── verifica senha      │
   │                        │── gera JWT            │
   │                        │── armazena sessão ──▶ │
   │◀── { token: JWT } ──── │                      │
   │                        │                      │
   │── GET /auth/me ───────▶│                      │
   │   (Bearer JWT)         │── valida JWT          │
   │                        │── busca sessão ──────▶│
   │                        │◀─ dados da sessão ────│
   │◀── MeResponseDTO ───── │                      │
```

### Componentes do LoginService

| Camada | Classes principais | Responsabilidade |
|---|---|---|
| Controller | `LoginImpl`, `Login` | Recebe requisições HTTP |
| Service | `LoginServiceImpl`, `JwtServiceImpl`, `SessionServiceImpl` | Lógica de negócio |
| Security | `JwtAuthenticationFilter`, `LoginRateLimitFilter` | Filtros Spring Security |
| Config | `SecurityConfig`, `RedisConfig` | Configurações de segurança e Redis |
| Repository | `UserAccountRepository`, `UserRepositoryAdapter` | Acesso ao banco de dados |
| Model | `SessionDTO`, `MeResponseDTO`, `AuthResponse` | DTOs de transferência |

### Pontos de Atenção (resumo)

> Consulte o relatório completo em [`relatorios/nota_desenho_de_solucao.pdf`](relatorios/nota_desenho_de_solucao.pdf)

| Severidade | Problema |
|---|---|
| 🔴 CRÍTICO | `symmetricKey` exposta no endpoint `/me` |
| 🔴 CRÍTICO | JWT secret hardcoded no `application.yaml` |
| 🟠 ALTO | Ausência de rate limiting no login |
| 🟠 ALTO | Ausência de HTTPS/TLS |
| 🟠 ALTO | Redis sem alta disponibilidade (single-node) |
| 🟡 MÉDIO | GameService sem DLQ, schema ou correlationId definidos |
| 🟡 MÉDIO | Fluxo PixService → `/me` sem circuit breaker |

---

## Como Instalar o Redis via Docker

### Linux

**Pré-requisito:** Docker instalado ([https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/))

```bash
# Subir Redis com o docker-compose do projeto:
docker-compose up -d redis

# --- OU --- rodar diretamente sem docker-compose:
docker run -d \
  --name login_redis \
  -p 6379:6379 \
  -v redis_data:/data \
  --restart unless-stopped \
  redis:7-alpine

# Verificar se está rodando
docker ps | grep redis

# Testar conexão
docker exec -it login_redis redis-cli ping
# Esperado: PONG
```

### macOS

**Pré-requisito:** Docker Desktop para Mac ([https://docs.docker.com/desktop/mac/install/](https://docs.docker.com/desktop/mac/install/))

```bash
# Após instalar e iniciar o Docker Desktop:

# Subir Redis com o docker-compose do projeto:
docker-compose up -d redis

# --- OU --- rodar diretamente:
docker run -d \
  --name login_redis \
  -p 6379:6379 \
  -v redis_data:/data \
  --restart unless-stopped \
  redis:7-alpine

# Verificar se está rodando
docker ps | grep redis

# Testar conexão
docker exec -it login_redis redis-cli ping
# Esperado: PONG
```

> **Dica macOS:** Certifique-se que o Docker Desktop está em execução (ícone na barra de menu) antes de rodar os comandos.

### Windows

**Pré-requisito:** Docker Desktop para Windows com WSL 2 ([https://docs.docker.com/desktop/windows/install/](https://docs.docker.com/desktop/windows/install/))

**PowerShell ou Prompt de Comando:**

```powershell
# Após instalar o Docker Desktop e reiniciar:

# Subir Redis com o docker-compose do projeto:
docker-compose up -d redis

# --- OU --- rodar diretamente:
docker run -d `
  --name login_redis `
  -p 6379:6379 `
  -v redis_data:/data `
  --restart unless-stopped `
  redis:7-alpine

# Verificar se está rodando
docker ps

# Testar conexão
docker exec -it login_redis redis-cli ping
# Esperado: PONG
```

> **Dica Windows:** Se aparecer erro de WSL2, execute no PowerShell como Administrador:
> ```powershell
> wsl --install
> wsl --set-default-version 2
> ```
> Reinicie o computador e inicie o Docker Desktop novamente.

### Verificando a conexão do Redis com a aplicação

Após subir o Redis, inicie a aplicação e acesse:

```bash
curl http://localhost:8081/api/v1/actuator/health
```

O campo `redis` deve aparecer como `UP`:

```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    "db": { "status": "UP" }
  }
}
```

### Parar e remover o Redis (quando não precisar mais)

```bash
# Parar
docker stop login_redis

# Remover container (dados persistidos no volume)
docker rm login_redis

# Remover também o volume de dados (apaga dados)
docker volume rm redis_data
```

---

## Relatórios e Avaliações

Os relatórios gerados estão na pasta [`relatorios/`](relatorios/):

| Arquivo | Descrição |
|---|---|
| [`relatorio_seguranca_eht.pdf`](relatorios/relatorio_seguranca_eht.pdf) | Relatório de testes de segurança (EHT) em português |
| [`relatorio_stress.pdf`](relatorios/relatorio_stress.pdf) | Relatório de testes de stress com múltiplos usuários |
| [`nota_desenho_de_solucao.pdf`](relatorios/nota_desenho_de_solucao.pdf) | Avaliação do desenho de solução com problemas e roadmap |
| [`nota_desenho_de_solucao.md`](relatorios/nota_desenho_de_solucao.md) | Versão Markdown da avaliação do desenho de solução |

### Scripts de teste

Os scripts estão na pasta [`tests/`](tests/):

| Script | Descrição |
|---|---|
| `security_eht_test.py` | Testes de segurança (EHT) — força bruta, injection, headers, JWT |
| `stress_test.py` | Testes de stress — múltiplos usuários simultâneos |
| `gerar_relatorios_pdf.py` | Gerador dos PDFs a partir dos resultados JSON |

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal (com Virtual Threads) |
| Spring Boot | 4.0.5 | Framework principal |
| Spring Security | 6+ | Autenticação e autorização |
| JWT (jjwt) | 0.11.5 | Tokens de autenticação stateless |
| Redis | 7+ | Store de sessões e rate limiting distribuído |
| H2 | — | Banco em memória (DEV) |
| PostgreSQL | — | Banco relacional (produção) |
| MapStruct | 1.5.5 | Mapeamento de DTOs |
| Lombok | 1.18.32 | Redução de boilerplate |
| Docker | 20+ | Containerização do Redis |
