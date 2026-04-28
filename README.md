# LoginService

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-brightgreen?logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-jjwt%200.11.5-blue?logo=jsonwebtokens)
![Redis](https://img.shields.io/badge/Redis-8.6.2-red?logo=redis)
![H2](https://img.shields.io/badge/H2-in--memory-lightgrey)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-driver-blue?logo=postgresql)
![Lombok](https://img.shields.io/badge/Lombok-1.18.46-pink)
![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-yellow)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)

Microserviço de autenticação e gestão de sessões baseado em **Spring Boot 4 + Java 25**, utilizando **JWT (HS256)** para autenticação stateless, **Redis** como store de sessões e rate-limiter distribuído, e **H2** (dev) / **PostgreSQL** (produção) como banco relacional.

---

## 📋 Sumário

- [1. Visão Geral do Projeto](#1-visão-geral-do-projeto)
- [2. Arquitetura do Sistema](#2-arquitetura-do-sistema)
- [3. Diagramas UML](#3-diagramas-uml)
- [4. Serviços e Comunicação](#4-serviços-e-comunicação)
- [5. Modelo de Dados](#5-modelo-de-dados)
- [6. Segurança e Autenticação](#6-segurança-e-autenticação)
- [7. Como Subir o Projeto (Docker)](#7-como-subir-o-projeto-docker)
- [8. Testando as APIs](#8-testando-as-apis)
- [9. Estrutura de Pastas](#9-estrutura-de-pastas)
- [10. Variáveis de Ambiente](#10-variáveis-de-ambiente)
- [11. Troubleshooting](#11-troubleshooting)
- [12. Próximos Passos e Melhorias](#12-próximos-passos-e-melhorias)
- [Relatórios e Avaliações](#relatórios-e-avaliações)

---

## 1. 🎯 Visão Geral do Projeto

### Nome e Propósito

**Artifact ID:** `login` | **Group ID:** `com.br.itau` | **Version:** `0.0.1-SNAPSHOT`

O **LoginService** é um microserviço de **autenticação e autorização** responsável por:

- Autenticar usuários via credenciais (username + password) com senha armazenada em BCrypt
- Emitir **tokens JWT (HS256)** com expiração de **5 minutos (300.000 ms)**
- Armazenar sessões autenticadas no **Redis** com TTL sincronizado ao token
- Aplicar **rate limiting distribuído** no endpoint de login (5 tentativas / 60 s por IP)
- Expor o endpoint `/auth/me` para que outros serviços validem a sessão do usuário
- Controlar acesso ao endpoint `/contract` via flag `contractService` da sessão

### Principais Funcionalidades

| # | Funcionalidade | Controller / Serviço |
|---|---|---|
| 1 | Login com emissão de JWT | `LoginImpl` → `LoginServiceImpl` |
| 2 | Consulta de sessão autenticada (`/me`) | `LoginImpl` → `SessionDTO` via `@AuthenticationPrincipal` |
| 3 | Contratação de serviço (`/contract`) | `ContractControllerIml` |
| 4 | Rate limiting por IP no login | `LoginRateLimitFilter` → `LoginRateLimiter` |
| 5 | Validação de JWT em cada requisição | `JwtAuthenticationFilter` |
| 6 | Autorização de contrato por flag de sessão | `ContractAuthorizationFilter` |

### Tecnologias (versões exatas)

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | **25** | Linguagem principal (Virtual Threads ativadas) |
| Spring Boot | **4.0.5** | Framework principal |
| Spring Security | (Boot 4.0.5) | Autenticação/autorização, filtros |
| Spring Data JPA | (Boot 4.0.5) | Acesso relacional (Hibernate) |
| Spring Data Redis | (Boot 4.0.5) | Store de sessões, rate limiting |
| JJWT | **0.11.5** | Geração e validação de JWT |
| Redis (Docker) | **8.6.2** | Cache e sessões |
| H2 Database | (Boot 4.0.5) | Banco em memória (dev/local) |
| PostgreSQL | (Boot 4.0.5) | Banco relacional (produção) |
| MapStruct | **1.6.3** | Mapeamento de DTOs |
| Lombok | **1.18.46** | Redução de boilerplate |
| Maven | 3.9.6 (Dockerfile) | Build tool |
| Docker | 3.8 (Compose) | Containerização |

---

## 2. 🏗️ Arquitetura do Sistema

### Diagrama de Arquitetura

```mermaid
flowchart TD
    Client["🌐 Cliente\n(Browser / App / curl)"]

    subgraph LoginService["LoginService — porta :8081"]
        direction TB
        RLF["LoginRateLimitFilter\n(429 após 5 req/60s por IP)"]
        JAF["JwtAuthenticationFilter\n(valida Bearer token)"]
        CAF["ContractAuthorizationFilter\n(verifica contractService flag)"]
        LC["LoginImpl\nPOST /auth/login\nGET /auth/me"]
        CC["ContractControllerIml\nPOST /contract"]
        LS["LoginServiceImpl"]
        JS["JwtServiceImpl\n(HS256, 5 min)"]
        SS["SessionServiceImpl"]
        UDS["UserDetailsServiceImpl"]
        REPO["UserAccountRepository\n(JPA)"]
        DATA["DataInitializer\n(user: Thiago / 231299)"]
    end

    subgraph Storage["Armazenamento"]
        H2[("H2 in-memory\nalunos_db\n(DEV)")]
        PG[("PostgreSQL\n(PROD)")]
        REDIS[("Redis :6379\nsession:{uuid}\nrate_limit:login:{ip}")]
    end

    Client -->|POST /api/v1/auth/login| RLF
    RLF --> JAF
    JAF --> LC
    LC --> LS
    LS --> UDS
    LS --> JS
    LS --> SS
    UDS --> REPO
    SS -->|SET session:{uuid} TTL 300s| REDIS
    JS -->|rate_limit:login:{ip} INCR+EXPIRE| REDIS
    REPO --> H2
    REPO -.->|produção| PG
    DATA -->|seed inicial| REPO

    Client -->|GET /api/v1/auth/me\nBearer JWT| JAF
    JAF -->|GET session:{uuid}| REDIS
    JAF --> LC

    Client -->|POST /api/v1/contract\nBearer JWT| CAF
    CAF --> CC
    CC --> SS
```

### Descrição dos Componentes

| Componente | Pacote | Responsabilidade |
|---|---|---|
| `LoginImpl` | `controller` | Recebe `POST /auth/login` e `GET /auth/me` |
| `ContractControllerIml` | `controller.contract` | Recebe `POST /contract` |
| `LoginServiceImpl` | `service` | Orquestra autenticação, geração de JWT e sessão |
| `JwtServiceImpl` | `service` | Gera e valida tokens JWT (HS256) |
| `SessionServiceImpl` | `service` | CRUD de sessões no Redis com TTL |
| `LoginRateLimiter` | `service` | Rate limiting via script Lua no Redis |
| `UserDetailsServiceImpl` | `security` | Carrega usuário do banco para o Spring Security |
| `JwtAuthenticationFilter` | `security` | Extrai JWT do header, valida, popula SecurityContext |
| `LoginRateLimitFilter` | `security` | Aplica rate limit no `/auth/login` |
| `ContractAuthorizationFilter` | `security` | Bloqueia `/contract` se `contractService=true` |
| `AuthenticationFailureEventListener` | `security` | Loga falhas de autenticação com IP |
| `UserRepositoryAdapter` | `adapters` | Adaptador entre domínio e JPA |
| `UserAccountRepository` | `repository` | Interface JPA Spring Data |
| `DataInitializer` | `config` | Seed de usuário inicial na inicialização |
| `RedisConfig` | `config` | Configuração do Lettuce + serialização JSON |
| `SecurityConfig` | `config` | Cadeia de filtros Spring Security |

### Padrões de Arquitetura Identificados

- **Microserviço** — serviço único e focado em autenticação
- **Stateless JWT** — token auto-suficiente com claims (`sessionId`, `role`, `contractService`)
- **Session Backing Store** — sessão duplicada no Redis para revogação e validação cruzada
- **Ports & Adapters (Hexagonal)** — interface `UserRepositoryDomain` desacoplada da implementação JPA
- **Filter Chain** — pipeline de segurança com filtros ordenados
- **Fail-Open Rate Limiting** — se Redis cair, requisições passam (não bloqueia usuários legítimos)
- **Distributed Rate Limiting** — script Lua atômico no Redis (sem race condition)

---

## 3. 📐 Diagramas UML

### Diagrama de Classes

```mermaid
classDiagram
    class UserAccount {
        +Long id
        +String username
        +String password
        +String nomeCompleto
        +String email
        +Boolean contractService
        +Role role
    }

    class Role {
        <<enumeration>>
        USER
        ADMIN
    }

    class SessionDTO {
        +String sessionId
        +String username
        +Boolean contractService
        +String symmetricKey
        +String role
    }

    class LoginRequest {
        +String username
        +String password
    }

    class AuthRequest {
        +String email
        +String password
    }

    class AuthResponse {
        +String token
    }

    class MeResponseDTO {
        +String sessionId
        +String username
        +Boolean contractService
        +String role
    }

    class ErrorResponse {
        <<record>>
        +int status
        +String error
        +String message
        +LocalDateTime timestamp
    }

    UserAccount --> Role : has
    LoginRequest ..> AuthResponse : produces (via login)
    SessionDTO ..> MeResponseDTO : projects
```

### Diagrama de Sequência — Fluxo de Login

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant RLF as LoginRateLimitFilter
    participant JAF as JwtAuthenticationFilter
    participant LC as LoginImpl
    participant LS as LoginServiceImpl
    participant AM as AuthenticationManager
    participant UDS as UserDetailsServiceImpl
    participant DB as H2/PostgreSQL
    participant JS as JwtServiceImpl
    participant SS as SessionServiceImpl
    participant RD as Redis

    Cliente->>RLF: POST /api/v1/auth/login\n{"username":"Thiago","password":"231299"}
    RLF->>RD: INCR rate_limit:login:{ip}\nEXPIRE 60s
    RD-->>RLF: counter = 1 (≤5, OK)
    RLF->>JAF: passa (sem Bearer token, continua)
    JAF->>LC: nenhum token, continua
    LC->>LS: login(LoginRequest)
    LS->>AM: authenticate(Thiago, 231299)
    AM->>UDS: loadUserByUsername("Thiago")
    UDS->>DB: findByUsername("Thiago")
    DB-->>UDS: UserAccount{password: BCrypt}
    UDS-->>AM: UserDetails OK
    AM-->>LS: Authentication OK
    LS->>DB: findByUsername("Thiago")
    DB-->>LS: UserAccount{role:USER, contractService:false}
    LS->>JS: generateToken("Thiago", uuid, "USER", false)
    JS-->>LS: JWT (HS256, 5 min)
    LS->>SS: save(SessionDTO, 300000ms)
    SS->>RD: SET session:{uuid} {SessionDTO JSON} EX 300
    RD-->>SS: OK
    LS-->>LC: AuthResponse{token}
    LC-->>Cliente: 200 OK {"token":"eyJ..."}
```

### Diagrama de Sequência — Fluxo de Validação do Token (/me)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant JAF as JwtAuthenticationFilter
    participant JS as JwtService
    participant SS as SessionService
    participant RD as Redis
    participant LC as LoginImpl

    Cliente->>JAF: GET /api/v1/auth/me\nAuthorization: Bearer eyJ...
    JAF->>JS: isTokenValid(token)
    JS-->>JAF: true
    JAF->>JS: getClaims(token)
    JS-->>JAF: Claims{sub:"Thiago", sessionId:"uuid", contractService:false}
    JAF->>SS: find("uuid")
    SS->>RD: GET session:uuid
    RD-->>SS: SessionDTO JSON
    SS-->>JAF: SessionDTO{username:"Thiago", role:"USER"}
    JAF->>JAF: SecurityContext.setAuthentication(session)
    JAF->>LC: GET /auth/me
    LC-->>Cliente: 200 OK {"sessionId":"uuid","username":"Thiago","contractService":false,"role":"USER"}
```

### Diagrama de Componentes — Filtros e Camadas

```mermaid
flowchart LR
    HTTP["HTTP Request"] --> F1

    subgraph "Spring Security Filter Chain"
        F1["LoginRateLimitFilter\n(só /api/v1/auth/login)"]
        F2["JwtAuthenticationFilter\n(todas as rotas)"]
        F3["ContractAuthorizationFilter\n(só POST /api/v1/contract)"]
        F1 --> F2 --> F3
    end

    F3 --> DISP["DispatcherServlet"]

    subgraph "Controller Layer"
        DISP --> AUTH["LoginImpl\n/auth/login\n/auth/me"]
        DISP --> CONT["ContractControllerIml\n/contract"]
    end

    subgraph "Service Layer"
        AUTH --> LS["LoginServiceImpl"]
        CONT --> SS["SessionServiceImpl"]
        LS --> JS["JwtServiceImpl"]
        LS --> SS
    end

    subgraph "Infrastructure"
        SS --> RD[("Redis")]
        LS --> DB[("H2 / PostgreSQL")]
        F1 --> RL["LoginRateLimiter\n(Lua INCR+EXPIRE)"]
        RL --> RD
    end
```

---

## 4. 🔗 Serviços e Comunicação

### Microserviço: LoginService

| Campo | Valor |
|---|---|
| Porta | **8081** |
| Context Path | `/api/v1` |
| Base URL (local) | `http://localhost:8081/api/v1` |

### Endpoints

| Método | URL | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | ❌ Pública | Autentica usuário e retorna JWT |
| `GET` | `/api/v1/auth/me` | ✅ Bearer JWT | Retorna dados da sessão atual |
| `POST` | `/api/v1/contract` | ✅ Bearer JWT | Contrata serviço (se `contractService=false`) |
| `GET` | `/api/v1/actuator/health` | ❌ Pública | Health check da aplicação |
| `GET` | `/api/v1/actuator/info` | ❌ Pública | Informações da aplicação |
| `GET` | `/api/v1/actuator/metrics` | ❌ Pública | Métricas da aplicação |
| `GET` | `/api/v1/h2-console` | ❌ Dev only | Console H2 (apenas perfil local) |

### Comunicação com Redis

O serviço se comunica com o Redis via **Lettuce** (cliente reativo do Spring Data Redis) usando serialização **GenericJackson2JsonRedisSerializer**:

| Operação | Chave | TTL | Descrição |
|---|---|---|---|
| Salvar sessão | `session:{uuid}` | 300 s | JSON do `SessionDTO` |
| Buscar sessão | `session:{uuid}` | — | Validação em cada requisição autenticada |
| Excluir sessão | `session:{uuid}` | — | Logout (quando implementado) |
| Rate limiting | `rate_limit:login:{ip}` | 60 s | Contador de tentativas por janela |

---

## 5. 🗄️ Modelo de Dados

### Entidade JPA — `UserAccount`

**Tabela:** `users`

| Campo | Tipo Java | Coluna SQL | Constraints | Descrição |
|---|---|---|---|---|
| `id` | `Long` | `id` | `PK`, `AUTO_INCREMENT` | Identificador único |
| `username` | `String` | `username` | `UNIQUE`, `NOT NULL` | Nome de usuário para login |
| `password` | `String` | `password` | `NOT NULL` | Hash BCrypt da senha |
| `nomeCompleto` | `String` | `nome_completo` | nullable | Nome completo do usuário |
| `email` | `String` | `email` | nullable | E-mail do usuário |
| `contractService` | `Boolean` | `contract_service` | nullable | Flag de contratação de serviço |
| `role` | `Role` | `role` | `EnumType.STRING` | Papel do usuário: `USER` ou `ADMIN` |

**Enum `Role`:**
```
USER  — usuário padrão
ADMIN — administrador
```

### Estrutura Redis

#### Sessão de usuário autenticado

```
Chave:  session:{uuid-v4}
Valor:  JSON do SessionDTO
TTL:    300 segundos (5 minutos — sincronizado com jwt.expiration-ms)

Exemplo de valor:
{
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "Thiago",
  "contractService": false,
  "symmetricKey": "base64-encoded-32-bytes-random-key",
  "role": "USER"
}
```

#### Rate limiting de login

```
Chave:  rate_limit:login:{ip-do-cliente}
Valor:  inteiro (contador de tentativas)
TTL:    60 segundos (configurável via rate-limit.window-seconds)
Máximo: 5 tentativas (configurável via rate-limit.max-requests)

Exemplos:
  rate_limit:login:192.168.1.100  = 3
  rate_limit:login:10.0.0.1       = 5  (próxima tentativa → 429)
```

### DataInitializer — Seed de Dados

Na inicialização, se o usuário `Thiago` não existir, é criado automaticamente:

```java
username:        "Thiago"
password:        BCrypt("231299")
nomeCompleto:    "Thiago"
email:           null
contractService: false
role:            Role.USER
```

---

## 6. 🔐 Segurança e Autenticação

### Fluxo JWT Completo

#### 1. Geração do Token (login)

```
JWT Header:   { "alg": "HS256" }
JWT Payload:  {
                "sub":             "Thiago",           ← username
                "iat":             1714284702,          ← emitido em
                "exp":             1714285002,          ← expira em (5 min depois)
                "sessionId":       "uuid-da-sessao",    ← ID da sessão Redis
                "role":            "USER",              ← papel do usuário
                "contractService": false                ← flag de contrato
              }
JWT Signature: HMAC-SHA256 com secret "ChangeThisSecretKeyForProdUseAtLeast32Chars!"
```

#### 2. Validação do Token (requisições protegidas)

```
1. Header "Authorization: Bearer <token>" extraído
2. JwtService.isTokenValid(token) — verifica assinatura e expiração
3. JwtService.getClaims(token) — extrai sub, sessionId, contractService
4. SessionService.find(sessionId) — busca SessionDTO no Redis
5. Se sessão nula → SecurityContextHolder.clearContext() → 401
6. Se sessão ok → cria UsernamePasswordAuthenticationToken(session, authorities)
7. SecurityContextHolder recebe o Authentication populado
```

### Filtros de Segurança (ordem de execução)

```
HTTP Request
    │
    ▼
LoginRateLimitFilter          ← apenas POST /api/v1/auth/login
    │  Redis INCR+EXPIRE (Lua script)
    │  Se count > 5 → HTTP 429 Too Many Requests
    │
    ▼
JwtAuthenticationFilter       ← todas as rotas
    │  Extrai "Authorization: Bearer <token>"
    │  Valida JWT, busca sessão Redis
    │  Popula SecurityContext com SessionDTO
    │
    ▼
ContractAuthorizationFilter   ← apenas POST /api/v1/contract
    │  Lê SessionDTO do SecurityContext
    │  Se contractService=true → HTTP 403 Forbidden
    │  Se session=null → HTTP 500
    │
    ▼
DispatcherServlet → Controller
```

### Configuração de Autorização (SecurityConfig)

| Rota | Regra |
|---|---|
| `POST /auth/login` | `permitAll()` — pública |
| `GET /actuator/**` | `permitAll()` — pública (health, info, metrics) |
| Qualquer outra rota | `authenticated()` — exige JWT válido |

- **Criptografia de senha:** BCryptPasswordEncoder
- **Sessão HTTP:** `STATELESS` (sem `HttpSession`)
- **CSRF:** Desabilitado (API REST com JWT)
- **401 Unauthorized:** retornado pelo `authenticationEntryPoint`
- **403 Forbidden:** retornado pelo `accessDeniedHandler`

---

## 7. 🐳 Como Subir o Projeto (Docker)

### Pré-requisitos

| Ferramenta | Versão | Download |
|---|---|---|
| Docker Desktop | 20+ | https://docs.docker.com/desktop/ |
| JDK | 25 | https://adoptium.net/temurin/releases/ |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |

> **Nota:** O Maven Wrapper (`./mvnw`) já está incluído no repositório. Não é necessário instalar o Maven separadamente.

### Passo a Passo

#### 1. Clonar o repositório

```bash
git clone https://github.com/ThiagoCintra/LoginService.git
cd LoginService
```

#### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

O arquivo `.env` gerado é suficiente para desenvolvimento local. Edite apenas se precisar mudar o secret JWT.

#### 3. Subir o Redis (docker-compose)

```bash
docker-compose up -d
```

O `docker-compose.yml` sobe apenas o Redis:

```yaml
services:
  redis:
    image: redis:8.6.2
    container_name: login_redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
```

Verificar se o Redis está saudável:

```bash
docker-compose ps
# Status esperado: "healthy"
```

#### 4. Compilar e executar a aplicação

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

**Windows:**

```cmd
mvnw.cmd spring-boot:run
```

**Ou compilar o JAR e rodar:**

```bash
./mvnw clean package -DskipTests
java -jar target/login-0.0.1-SNAPSHOT.jar
```

#### 5. Verificar se a aplicação está no ar

```bash
curl http://localhost:8081/api/v1/actuator/health
```

Resposta esperada:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### Build da Imagem Docker (opcional)

O `Dockerfile` usa multi-stage build com `eclipse-temurin:25-jdk-jammy`:

```bash
docker build -t loginservice:latest .
docker run -d \
  --name loginservice \
  -p 8081:8081 \
  -e SPRING_REDIS_HOST=host.docker.internal \
  loginservice:latest
```

### Comandos Úteis

```bash
# Ver logs do Redis
docker logs -f login_redis

# Ver logs da aplicação (arquivo)
tail -f logs/login-application.log

# Restart do Redis
docker-compose restart redis

# Parar tudo
docker-compose down

# Parar e remover volumes (limpa dados Redis)
docker-compose down -v

# Acessar CLI do Redis
docker exec -it login_redis redis-cli

# Ver todas as sessões ativas no Redis
docker exec -it login_redis redis-cli keys "session:*"

# Ver rate limits ativos
docker exec -it login_redis redis-cli keys "rate_limit:login:*"

# Ver valor de uma sessão
docker exec -it login_redis redis-cli get "session:<uuid-aqui>"
```

---

## 8. 🧪 Testando as APIs

### Usuário padrão (criado automaticamente no DataInitializer)

```
Username: Thiago
Password: 231299
Role:     USER
```

---

### POST /api/v1/auth/login

**Descrição:** Autentica o usuário e retorna um token JWT.

**Request:**
```http
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{
  "username": "Thiago",
  "password": "231299"
}
```

**curl:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "Thiago", "password": "231299"}'
```

**Resposta 200 OK:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJUaGlhZ28iLCJpYXQiOjE3MTQyODQ3MDIsImV4cCI6MTcxNDI4NTAwMiwic2Vzc2lvbklkIjoiYTFiMmMzZDQtZTVmNi03ODkwLWFiY2QtZWYxMjM0NTY3ODkwIiwicm9sZSI6IlVTRVIiLCJjb250cmFjdFNlcnZpY2UiOmZhbHNlfQ.assinatura"
}
```

**Resposta 401 Unauthorized (senha errada):**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Usuário inexistente ou senha inválida",
  "timestamp": "2024-04-28T05:11:42"
}
```

**Resposta 429 Too Many Requests (rate limit excedido):**
```
HTTP 429 Too Many Requests
```

---

### GET /api/v1/auth/me

**Descrição:** Retorna os dados da sessão autenticada. Requer o token obtido no login.

**Request:**
```http
GET http://localhost:8081/api/v1/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**curl (substitua `SEU_TOKEN` pelo token real retornado no login):**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "Thiago", "password": "231299"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**Resposta 200 OK:**
```json
{
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "Thiago",
  "contractService": false,
  "role": "USER"
}
```

**Resposta 401 Unauthorized (sem token ou token expirado):**
```
HTTP 401 Unauthorized
```

---

### POST /api/v1/contract

**Descrição:** Contrata o serviço para o usuário autenticado. Bloqueado se `contractService=true`.

**Request:**
```http
POST http://localhost:8081/api/v1/contract
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**curl:**
```bash
curl -X POST http://localhost:8081/api/v1/contract \
  -H "Authorization: Bearer $TOKEN"
```

**Resposta 200 OK** (contratação realizada com sucesso)

**Resposta 403 Forbidden** (serviço já contratado — `contractService=true`)

**Resposta 401 Unauthorized** (sem token)

---

### Coleção Postman

Importe a coleção abaixo no Postman (File → Import → Raw Text):

```json
{
  "info": {
    "name": "LoginService",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:8081/api/v1" },
    { "key": "token", "value": "" }
  ],
  "item": [
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/auth/login",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\"username\": \"Thiago\", \"password\": \"231299\"}"
        }
      },
      "event": [{
        "listen": "test",
        "script": {
          "exec": ["pm.collectionVariables.set('token', pm.response.json().token);"]
        }
      }]
    },
    {
      "name": "Me (sessão atual)",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/auth/me",
        "header": [{ "key": "Authorization", "value": "Bearer {{token}}" }]
      }
    },
    {
      "name": "Contract",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/contract",
        "header": [{ "key": "Authorization", "value": "Bearer {{token}}" }]
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/actuator/health"
      }
    }
  ]
}
```

---

## 9. 📁 Estrutura de Pastas

```
LoginService/
├── src/
│   ├── main/
│   │   ├── java/com/br/itau/login/
│   │   │   ├── LoginApplication.java              ← Main class Spring Boot
│   │   │   │
│   │   │   ├── adapters/
│   │   │   │   └── UserRepositoryAdapter.java     ← Ponte entre domínio e JPA
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java           ← Seed do usuário "Thiago/231299"
│   │   │   │   ├── RedisConfig.java               ← Configuração Lettuce + JSON serializer
│   │   │   │   └── SecurityConfig.java            ← Filtros, regras de autorização
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── Login.java                     ← Interface do controller de auth
│   │   │   │   ├── LoginImpl.java                 ← Implementação POST /auth/login, GET /auth/me
│   │   │   │   └── contract/
│   │   │   │       ├── ContractController.java    ← Interface POST /contract
│   │   │   │       └── ContractControllerIml.java ← Implementação do contrato
│   │   │   │
│   │   │   ├── domains/
│   │   │   │   └── UserRepositoryDomain.java      ← Porta (interface de domínio) do repositório
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java    ← @RestControllerAdvice, mapeia exceções
│   │   │   │   └── UserNotFoundException.java     ← Exceção de usuário não encontrado
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── SessionDTO.java                ← DTO da sessão armazenada no Redis
│   │   │   │   ├── entity/
│   │   │   │   │   └── UserAccount.java           ← Entidade JPA, tabela "users"
│   │   │   │   ├── enums/
│   │   │   │   │   └── Role.java                  ← Enum: USER, ADMIN
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequest.java          ← {username, password}
│   │   │   │   │   └── AuthRequest.java           ← {email, password} (não usado no controller atual)
│   │   │   │   └── response/
│   │   │   │       ├── AuthResponse.java          ← {token}
│   │   │   │       ├── MeResponseDTO.java         ← {sessionId, username, contractService, role}
│   │   │   │       └── errors/
│   │   │   │           └── ErrorResponse.java     ← Record {status, error, message, timestamp}
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   └── UserAccountRepository.java     ← JpaRepository<UserAccount, Long>
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── AuthenticationFailureEventListener.java ← Loga falhas de login com IP
│   │   │   │   ├── ContractAuthorizationFilter.java        ← Verifica flag contractService
│   │   │   │   ├── JwtAuthenticationFilter.java            ← Valida JWT, popula SecurityContext
│   │   │   │   ├── LoginRateLimitFilter.java               ← Aplica rate limit no /auth/login
│   │   │   │   └── UserDetailsServiceImpl.java             ← Carrega usuário do banco
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── ContractService.java           ← Interface de contrato
│   │   │   │   ├── ContractServiceImpl.java       ← Implementação (em desenvolvimento)
│   │   │   │   ├── JwtService.java                ← Interface JWT
│   │   │   │   ├── JwtServiceImpl.java            ← HS256, 5 min, claims: sub/sessionId/role/contractService
│   │   │   │   ├── LoginRateLimiter.java          ← Lua INCR+EXPIRE no Redis
│   │   │   │   ├── LoginService.java              ← Interface de login
│   │   │   │   ├── LoginServiceImpl.java          ← Orquestra autenticação + JWT + sessão
│   │   │   │   ├── SessionService.java            ← Interface de sessão
│   │   │   │   └── SessionServiceImpl.java        ← CRUD de sessões no Redis
│   │   │   │
│   │   │   └── utils/
│   │   │       ├── IpUtils.java                   ← Extrai IP real (X-Forwarded-For, X-Real-IP)
│   │   │       └── SessionUtils.java              ← Helpers: UUID, symmetric key, save+token
│   │   │
│   │   └── resources/
│   │       ├── application.yaml                   ← Configuração principal
│   │       └── logback-spring.xml                 ← Configuração de logging
│   │
│   └── test/
│       ├── java/com/br/itau/login/
│       │   ├── LoginApplicationTests.java
│       │   ├── config/
│       │   │   └── TestRedisConfig.java           ← Mock Redis para testes
│       │   └── service/
│       │       ├── JwtServiceImplTest.java
│       │       ├── LoginServiceImplTest.java
│       │       └── SessionServiceImplTest.java
│       └── resources/
│           └── application-test.yaml             ← Config de testes (H2 + sem Redis)
│
├── docker-compose.yml                            ← Redis 8.6.2 na porta 6379
├── Dockerfile                                    ← Multi-stage: eclipse-temurin:25-jdk-jammy
├── pom.xml                                       ← Spring Boot 4.0.5, Java 25
├── .env.example                                  ← Template de variáveis de ambiente
├── logs/                                         ← Logs da aplicação (login-application.log)
├── relatorios/                                   ← Relatórios PDF e MD de segurança e stress
└── tests/                                        ← Scripts Python de teste (EHT, stress)
```

---

## 10. ⚙️ Variáveis de Ambiente

### Arquivo `.env` (carregado pelo Spring Boot via relaxed binding)

| Variável | Valor padrão | Descrição |
|---|---|---|
| `SPRING_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_REDIS_PORT` | `6379` | Porta do Redis |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:alunos_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | URL do banco de dados |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | *(vazio)* | Senha do banco |
| `JWT_SECRET` | `ChangeThisSecretKeyForDev_UseYourOwnInProd_AtLeast32Chars!` | Secret HMAC-SHA256 (mín. 32 chars) |
| `JWT_EXPIRATION_MS` | `300000` | Expiração do JWT em ms (5 minutos) |
| `SERVER_PORT` | `8081` | Porta HTTP da aplicação |
| `SERVER_SERVLET_CONTEXT_PATH` | `/api/v1` | Context path |
| `LOGGING_FILE_NAME` | `logs/login-application.log` | Caminho do arquivo de log |

### Configurações adicionais em `application.yaml`

| Propriedade YAML | Valor padrão | Descrição |
|---|---|---|
| `rate-limit.max-requests` | `5` | Máximo de tentativas de login por janela |
| `rate-limit.window-seconds` | `60` | Tamanho da janela de rate limit (segundos) |
| `threads.virtual.enabled` | `true` | Ativa Virtual Threads (Java 21+) |
| `app.datasource.hikari.maximum-pool-size` | `20` | Pool máximo de conexões HikariCP |
| `app.datasource.hikari.minimum-idle` | `5` | Pool mínimo idle HikariCP |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | Endpoints do Actuator expostos |

> ⚠️ **Produção:** Sempre substitua `JWT_SECRET` por um valor aleatório forte (mínimo 32 caracteres). Use Kubernetes Secrets, AWS Secrets Manager ou HashiCorp Vault para injeção segura.

---

## 11. 🔧 Troubleshooting

### ❌ 401 Unauthorized

**Sintomas:** Qualquer endpoint protegido retorna `{"status":401,"error":"Unauthorized"}`

**Causas e soluções:**

| Causa | Diagnóstico | Solução |
|---|---|---|
| Token não enviado | `curl` sem `-H "Authorization: Bearer ..."` | Adicionar header `Authorization: Bearer <token>` |
| Token expirado (5 min) | Decode JWT em jwt.io — verificar campo `exp` | Fazer novo login para obter token fresco |
| Sessão expirou no Redis | `redis-cli get session:<uuid>` retorna `nil` | Fazer novo login |
| JWT secret diferente | Token gerado com outro secret | Verificar se `JWT_SECRET` é o mesmo em todas as instâncias |
| Token malformado | Erro de parsing no `JwtServiceImpl` | Verificar se copiou o token completo sem espaços extras |

**Verificar sessão no Redis:**
```bash
docker exec -it login_redis redis-cli get "session:a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

---

### ❌ Redis connection refused

**Sintomas:** Aplicação não inicia ou `/actuator/health` mostra `redis.status: DOWN`

**Causas e soluções:**

| Causa | Diagnóstico | Solução |
|---|---|---|
| Redis não está rodando | `docker-compose ps` — status não é "healthy" | `docker-compose up -d redis` |
| Porta 6379 bloqueada | `nc -zv localhost 6379` — connection refused | Liberar porta no firewall ou mudar `SPRING_REDIS_PORT` |
| Host incorreto | Aplicação no Docker tentando `localhost` | Usar `host.docker.internal` ou nome do serviço |
| Redis reiniciando | `docker logs login_redis` — erros de memória | Aumentar memória disponível para o Docker |

**Verificar conexão:**
```bash
# Teste direto no CLI
docker exec -it login_redis redis-cli ping
# Esperado: PONG

# Testar porta do host
nc -zv localhost 6379
```

**Nota:** O `RedisConfig` usa o profile `!test` — em testes unitários o Redis é excluído automaticamente via `TestRedisConfig`.

---

### ❌ JWT signature does not match

**Sintomas:** Token retorna `401` mesmo sendo recente

**Causa:** O `JWT_SECRET` usado para gerar o token é diferente do secret atual da aplicação.

**Solução:**
```bash
# Verificar o secret configurado
grep -r "jwt.secret" src/main/resources/application.yaml

# Se reiniciou a aplicação com secret diferente, fazer novo login:
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "Thiago", "password": "231299"}'
```

---

### ❌ 429 Too Many Requests

**Sintomas:** `POST /auth/login` retorna `HTTP 429` após algumas tentativas

**Causa:** Rate limit de 5 tentativas por 60 segundos por IP foi atingido.

**Diagnóstico:**
```bash
# Ver contador atual do seu IP
docker exec -it login_redis redis-cli get "rate_limit:login:127.0.0.1"

# Ver TTL restante da janela
docker exec -it login_redis redis-cli ttl "rate_limit:login:127.0.0.1"
```

**Solução:** Aguardar a janela de 60 segundos expirar, ou durante desenvolvimento aumentar o limite:
```yaml
# application.yaml
rate-limit:
  max-requests: 100
  window-seconds: 60
```

---

### ❌ Console H2 não abre

**Causa:** O context path `/api/v1` é necessário na URL.

**URL correta:** `http://localhost:8081/api/v1/h2-console`
- JDBC URL: `jdbc:h2:mem:alunos_db`
- Username: `sa`
- Password: *(deixar em branco)*

---

## 12. 🚀 Próximos Passos e Melhorias

### Segurança

| Prioridade | Melhoria | Justificativa |
|---|---|---|
| 🔴 Alta | Remover `symmetricKey` do endpoint `/me` | A chave simétrica não deve ser exposta para clientes |
| 🔴 Alta | Mover `JWT_SECRET` para variável de ambiente obrigatória | Secret hardcoded em `application.yaml` é risco crítico de segurança |
| 🔴 Alta | Habilitar HTTPS/TLS | Tokens JWT trafegam em plaintext no HTTP |
| 🟠 Média | Implementar logout com invalidação de sessão Redis | Hoje o token permanece válido até expirar mesmo após "logout" |
| 🟠 Média | Adicionar CORS configuration explícita | Falta controle de origens permitidas |
| 🟡 Baixa | Adicionar fingerprint de device/User-Agent ao token | Previne roubo de token |

### Funcionalidades

| Prioridade | Melhoria | Código relacionado |
|---|---|---|
| 🟠 Média | Completar `ContractServiceImpl` | Classe possui lógica comentada aguardando implementação |
| 🟠 Média | Implementar refresh token | Token expira em 5 min — usuários precisam relogar frequentemente |
| 🟡 Baixa | Endpoint de logout (`DELETE /auth/session`) | `SessionService.delete()` já existe, falta o endpoint |
| 🟡 Baixa | Endpoint admin para listar/revogar sessões | Útil para gestão de segurança |
| 🟡 Baixa | Utilizar o driver PostgreSQL em produção | Hoje apenas H2 é configurado no `application.yaml` |

### Observabilidade

| Prioridade | Melhoria | Justificativa |
|---|---|---|
| 🟠 Média | Adicionar Micrometer + Prometheus/Grafana | Actuator já expõe `/metrics`, falta exportar para sistema de monitoramento |
| 🟠 Média | Adicionar correlation ID nos logs | Rastreabilidade de requisições em produção |
| 🟡 Baixa | Estruturar logs em JSON (Logstash encoder) | Facilita ingestão em Elasticsearch/Kibana |

### Resiliência

| Prioridade | Melhoria | Justificativa |
|---|---|---|
| 🟠 Média | Redis em modo Sentinel ou Cluster | Redis single-node é ponto único de falha |
| 🟡 Baixa | Circuit Breaker no acesso ao Redis | Hoje o rate limiter falha aberto (correto), mas a sessão pode falhar fechado |

---

## 📊 Relatórios e Avaliações

Os relatórios gerados estão na pasta [`relatorios/`](relatorios/):

| Arquivo | Descrição |
|---|---|
| [`relatorio_seguranca_eht.pdf`](relatorios/relatorio_seguranca_eht.pdf) | Relatório de testes de segurança (EHT) em português |
| [`relatorio_stress.pdf`](relatorios/relatorio_stress.pdf) | Relatório de testes de stress com múltiplos usuários |
| [`nota_desenho_de_solucao.pdf`](relatorios/nota_desenho_de_solucao.pdf) | Avaliação do desenho de solução com problemas e roadmap |
| [`nota_desenho_de_solucao.md`](relatorios/nota_desenho_de_solucao.md) | Versão Markdown da avaliação do desenho de solução |

### Scripts de Teste

Os scripts estão na pasta [`tests/`](tests/):

| Script | Descrição |
|---|---|
| `security_eht_test.py` | Testes de segurança (EHT) — força bruta, injection, headers, JWT |
| `stress_test.py` | Testes de stress — múltiplos usuários simultâneos |
| `gerar_relatorios_pdf.py` | Gerador dos PDFs a partir dos resultados JSON |

