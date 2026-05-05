# LoginService

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-brightgreen?logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-jjwt%200.11.5-blue?logo=jsonwebtokens)
![Redis](https://img.shields.io/badge/Redis-Lettuce-red?logo=redis)
![H2](https://img.shields.io/badge/H2-in--memory-lightgrey)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-driver-blue?logo=postgresql)
![Lombok](https://img.shields.io/badge/Lombok-1.18.46-pink)
![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-yellow)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)

Microserviço de autenticação e gestão de sessões baseado em **Spring Boot 3.3.5 + Java 21**, utilizando **JWT (HS256)** para autenticação stateless, **Redis** como store de sessões e rate-limiter distribuído, e **H2** (dev/local) / **PostgreSQL** (produção) como banco relacional.

---

## 📋 Sumário

- [1. Visão Geral](#1-visão-geral)
- [2. Tecnologias](#2-tecnologias)
- [3. Arquitetura e Componentes](#3-arquitetura-e-componentes)
- [4. Endpoints](#4-endpoints)
- [5. Fluxo de Autenticação](#5-fluxo-de-autenticação)
- [6. Fluxo de Acesso a Dados](#6-fluxo-de-acesso-a-dados)
- [7. Modelo de Dados](#7-modelo-de-dados)
- [8. Segurança](#8-segurança)
- [9. Rate Limiting](#9-rate-limiting)
- [10. Como Executar](#10-como-executar)
- [11. Variáveis de Ambiente](#11-variáveis-de-ambiente)
- [12. Estrutura de Pastas](#12-estrutura-de-pastas)
- [13. Pendências e Melhorias](#13-pendências-e-melhorias)

---

## 1. Visão Geral

**Artifact ID:** `login` | **Group ID:** `com.br.itau` | **Versão:** `0.0.2`

O **LoginService** é um microserviço de **autenticação e autorização** que:

- Autentica usuários via `username` + `password` (senha armazenada em BCrypt)
- Emite **tokens JWT (HS256)** com TTL configurável via `jwt.expiration-ms`
- Armazena sessões autenticadas no **Redis** com TTL sincronizado ao token
- Aplica **rate limiting distribuído** por IP no endpoint de login (padrão: 5 tentativas / 60 s)
- Expõe `/auth/me` para consulta da sessão autenticada
- Controla acesso ao `/contract` via flag `contractService` presente na sessão

---

## 2. Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | **21** | Linguagem principal |
| Spring Boot | **3.3.5** | Framework base |
| Spring Security | (Boot 3.3.5) | Filtros, autenticação e autorização |
| Spring Data JPA | (Boot 3.3.5) | Acesso relacional via Hibernate |
| Spring Data Redis | (Boot 3.3.5) | Store de sessões e rate limiting |
| Spring Actuator | (Boot 3.3.5) | Health check (`/actuator/health`) |
| JJWT | **0.11.5** | Geração e validação de JWT |
| Lettuce | (Boot 3.3.5) | Cliente Redis reativo |
| H2 Database | runtime | Banco em memória (dev/local) |
| PostgreSQL | runtime | Banco relacional (produção) |
| MapStruct | **1.6.3** | Mapeamento de DTOs |
| Lombok | **1.18.46** | Redução de boilerplate |
| Maven | — | Build tool |
| Docker Compose | — | Containerização |

---

## 3. Arquitetura e Componentes

### Visão de Camadas

```
┌─────────────────────────────────────────────────────────┐
│                    Security Filter Chain                │
│  LoginRateLimitFilter → JwtAuthenticationFilter         │
│  → ContractAuthorizationFilter                          │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                   Controller Layer                      │
│  LoginImpl  (/auth/login, /auth/me)                     │
│  ContractControllerIml  (/contract)                     │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                    Service Layer                        │
│  LoginServiceImpl  JwtServiceImpl  SessionServiceImpl   │
│  MeServiceImpl     LoginRateLimiter                     │
└──────┬──────────────────────────────────────────┬───────┘
       │                                          │
┌──────▼──────┐                          ┌────────▼───────┐
│  H2 / PG    │  (via UserRepositoryAdapter + JPA)  │  Redis  │
└─────────────┘                          └────────────────┘
```

### Descrição dos Componentes

| Classe | Pacote | Responsabilidade |
|---|---|---|
| `LoginImpl` | `controller` | Recebe `POST /auth/login` e `GET /auth/me` |
| `ContractControllerIml` | `controller.contract` | Recebe `POST /contract` |
| `LoginServiceImpl` | `service` | Orquestra autenticação, sessão e emissão de JWT |
| `JwtServiceImpl` | `service` | Gera e valida tokens JWT HS256 |
| `SessionServiceImpl` | `service` | CRUD de sessões no Redis com TTL |
| `MeServiceImpl` | `service` | Busca dados do usuário autenticado |
| `LoginRateLimiter` | `service` | Rate limiting distribuído via script Lua no Redis |
| `UserDetailsServiceImpl` | `security` | Carrega usuário do banco para o Spring Security |
| `JwtAuthenticationFilter` | `security` | Extrai JWT, valida, busca sessão e popula `SecurityContext` |
| `LoginRateLimitFilter` | `security` | Aplica rate limit no endpoint de login |
| `ContractAuthorizationFilter` | `security` | Bloqueia `/contract` se `contractService=true` na sessão |
| `AuthenticationFailureEventListener` | `security` | Registra em log as tentativas de login com falha |
| `UserRepositoryAdapter` | `adapters` | Adaptador entre a interface de domínio e o repositório JPA |
| `UserAccountRepository` | `repository` | Interface `JpaRepository<UserAccount, Long>` |
| `SecurityConfig` | `config` | Cadeia de filtros Spring Security, BCrypt, `AuthenticationManager` |
| `RedisConfig` | `config` | `LettuceConnectionFactory` + `GenericJackson2JsonRedisSerializer` |
| `DataInitializer` | `config` | Seed de usuário inicial na inicialização (`CommandLineRunner`) |
| `GlobalExceptionHandler` | `exception` | Tratamento centralizado de exceções (`@RestControllerAdvice`) |

### Padrões de Arquitetura

- **Ports & Adapters (Hexagonal)** — interface `UserRepositoryDomain` desacopla serviços da implementação JPA
- **Filter Chain** — pipeline de segurança com filtros ordenados e responsabilidades separadas
- **Stateful JWT** — token JWT + sessão espelhada no Redis (permite revogação)
- **Fail-Open Rate Limiting** — se o Redis estiver indisponível, requisições passam (evita bloqueio de usuários legítimos)

---

## 4. Endpoints

| Método | Caminho | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | ❌ Pública | Autentica usuário e retorna token JWT |
| `GET` | `/auth/me` | ✅ Bearer JWT | Retorna dados da sessão autenticada |
| `POST` | `/contract` | ✅ Bearer JWT | Acesso ao serviço de contrato (bloqueado se já contratado) |
| `GET` | `/actuator/health` | ❌ Pública | Health check da aplicação |

### POST `/auth/login`

**Request:**
```json
{
  "username": "Thiago",
  "password": "231299"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Possíveis erros:**

| Status | Situação |
|---|---|
| `401 Unauthorized` | Credenciais inválidas |
| `429 Too Many Requests` | Rate limit excedido para o IP |
| `400 Bad Request` | Validação de campos falhou |

---

### GET `/auth/me`

**Header:** `Authorization: Bearer <token>`

**Response `200 OK`:**
```json
{
  "sessionId": "uuid",
  "username": "Thiago",
  "contractService": false,
  "role": "USER",
  "id": 1,
  "channel": "MOBILE"
}
```

**Possíveis erros:**

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente, inválido ou sessão expirada |

---

### POST `/contract`

**Header:** `Authorization: Bearer <token>`

**Response `200 OK`:** corpo vazio (serviço de contrato disponível)

**Possíveis erros:**

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente ou inválido |
| `403 Forbidden` | Serviço já contratado (`contractService=true` na sessão) |
| `500 Internal Server Error` | Sessão não encontrada no contexto |

---

## 5. Fluxo de Autenticação

```
Cliente
  │
  ▼ POST /auth/login { username, password }
LoginRateLimitFilter
  │── Redis INCR rate_limit:login:{ip} (Lua INCR+EXPIRE)
  │── contador > 5? → HTTP 429
  ▼
JwtAuthenticationFilter
  │── sem Bearer token → continua sem autenticação
  ▼
LoginImpl.login(LoginRequest)
  ▼
LoginServiceImpl.login()
  │── AuthenticationManager.authenticate(username, password)
  │     └── UserDetailsServiceImpl.loadUserByUsername()
  │           └── UserAccountRepository.findByUsername()  →  Banco
  │     └── BCrypt.verify(password, hash)
  │── credenciais inválidas? → BadCredentialsException → HTTP 401
  │── UserRepositoryDomain.findByUsername()  →  Banco
  │── SessionUtils.generateSessionId()  →  UUID aleatório
  │── SessionUtils.generateSymmetricKey()  →  32 bytes Base64
  │── SessionDTO { sessionId, username, contractService, symmetricKey, role }
  │── SessionUtils.saveSession()
  │     └── SessionServiceImpl.save(SessionDTO, ttlMs)
  │           └── Redis SET session:{uuid} {JSON} EX {ttl}
  │── JwtService.generateToken(username, sessionId, role, contractService)
  │     └── Claims: sub, iat, exp, sessionId, role, contractService, channel="MOBILE"
  │     └── Assina com HS256 usando jwt.secret
  ▼
AuthResponse { token }  →  HTTP 200
```

---

## 6. Fluxo de Acesso a Dados

### GET `/auth/me` — do Controller ao Banco

```
Cliente  →  GET /auth/me  (Authorization: Bearer <token>)
  ▼
JwtAuthenticationFilter
  │── JwtService.isTokenValid(token)  →  verifica assinatura e expiração
  │── JwtService.getClaims(token)  →  extrai username, sessionId, contractService
  │── SessionService.find(sessionId)
  │     └── Redis GET session:{uuid}  →  SessionDTO (ou null)
  │── sessão nula? → SecurityContext.clearContext() → HTTP 401
  │── cria UsernamePasswordAuthenticationToken(session, authorities)
  │── SecurityContext.setAuthentication(auth)
  ▼
LoginImpl.me(@AuthenticationPrincipal SessionDTO session)
  │── session == null? → HTTP 500
  ▼
MeServiceImpl.getUserInfo(sessionId, sessionId, username)
  │── SessionService.find(sessionId)  →  valida sessão novamente
  │── UserRepositoryDomain.findByUsername(username)  →  Banco
  │── Monta MeResponseDTO { id, sessionId, username, contractService, role, channel }
  ▼
LoginImpl  →  dto.setChannel("MOBILE")
  ▼
HTTP 200  →  MeResponseDTO
```

---

## 7. Modelo de Dados

### Entidade JPA — `UserAccount` (tabela `users`)

| Campo | Tipo Java | Restrições | Descrição |
|---|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` | Identificador único gerado automaticamente |
| `username` | `String` | `UNIQUE`, `NOT NULL` | Nome de usuário (chave de login) |
| `password` | `String` | `NOT NULL` | Hash BCrypt da senha |
| `nomeCompleto` | `String` | — | Nome completo do usuário |
| `email` | `String` | — | E-mail do usuário |
| `contractService` | `Boolean` | — | Flag indicando se o serviço já foi contratado |
| `role` | `Role` | `EnumType.STRING` | Papel do usuário: `USER` ou `ADMIN` |

**Enum `Role`:** `USER` | `ADMIN`

### Estrutura Redis

| Chave | Tipo | TTL | Conteúdo |
|---|---|---|---|
| `session:{uuid}` | String (JSON) | `jwt.expiration-ms` | `SessionDTO` serializado com `GenericJackson2JsonRedisSerializer` |
| `rate_limit:login:{ip}` | String (contador) | `rate-limit.window-seconds` (padrão 60 s) | Contador de requisições por janela deslizante |

### SessionDTO (armazenado no Redis)

| Campo | Tipo | Descrição |
|---|---|---|
| `sessionId` | `String` | UUID da sessão |
| `username` | `String` | Username do usuário autenticado |
| `contractService` | `Boolean` | Flag de contratação |
| `symmetricKey` | `String` | Chave simétrica aleatória (32 bytes, Base64) |
| `role` | `String` | Role como string (`USER` ou `ADMIN`) |

---

## 8. Segurança

### SecurityFilterChain

Configurado em `SecurityConfig` (`@Configuration`, `@EnableMethodSecurity`):

| Regra | Configuração |
|---|---|
| CSRF | Desabilitado (`csrf.disable()`) |
| Sessão HTTP | `STATELESS` — Spring não cria HttpSession |
| `/auth/login` | `permitAll()` — sem autenticação |
| `/actuator/health` | `permitAll()` — sem autenticação |
| Qualquer outra rota | `authenticated()` — requer JWT válido + sessão Redis |
| Erro 401 | Responde com `401 Unauthorized` |
| Erro 403 | Responde com `403 Forbidden` |

### Ordem dos Filtros

```
LoginRateLimitFilter
    ↓
JwtAuthenticationFilter
    ↓
(UsernamePasswordAuthenticationFilter — posição de referência)
    ↓
ContractAuthorizationFilter
```

### JWT

- Algoritmo: **HS256**
- Chave: configurada em `${jwt.secret}` (bytes UTF-8)
- Claims incluídos no token: `sub` (username), `iat`, `exp`, `sessionId`, `role`, `contractService`, `channel`
- Biblioteca: `io.jsonwebtoken` (JJWT) versão 0.11.5

### Encoder de Senha

`BCryptPasswordEncoder` — configurado como bean em `SecurityConfig`.

### Tratamento de Erros de Autenticação

`GlobalExceptionHandler` (`@RestControllerAdvice`) cobre:

| Exceção | Status HTTP | Mensagem |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Detalhes de validação por campo |
| `IllegalArgumentException` | 400 | Mensagem da exceção |
| `UserNotFoundException` | 404 | Mensagem da exceção |
| `BadCredentialsException` | 401 | "Usuário inexistente ou senha inválida" |
| `AuthenticationException` | 401 | "Usuário inexistente ou senha inválida" |
| `Exception` (genérico) | 500 | Mensagem da exceção |

---

## 9. Rate Limiting

`LoginRateLimiter` implementa rate limiting distribuído usando **script Lua atômico no Redis**:

```lua
local c = redis.call('INCR', KEYS[1])
if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return c
```

| Configuração | Propriedade | Padrão |
|---|---|---|
| Máximo de requisições por janela | `rate-limit.max-requests` | `5` |
| Duração da janela | `rate-limit.window-seconds` | `60` |
| Prefixo da chave Redis | `rate_limit:login:{ip}` | — |

**Comportamento em falha:** se o Redis estiver indisponível (`count == null`), o limiter **falha aberto** — a requisição é permitida.

A extração do IP do cliente é feita por `IpUtils.getClientIp()`, que respeita os headers `X-Forwarded-For` e `X-Real-IP` (proxies).

> ⚠️ **Nota:** `LoginRateLimitFilter.shouldNotFilter()` verifica o path `/api/v1/auth/login`, mas o endpoint está mapeado em `/auth/login`. O rate limit pode não estar sendo aplicado dependendo do context path configurado.

---

## 10. Como Executar

### Pré-requisitos

- Docker e Docker Compose instalados
- JDK 21 (para build local sem Docker)
- Maven 3.9+ (para build local)

### Com Docker Compose

```bash
# Copiar variáveis de ambiente
cp .env.example .env
# Editar .env com suas configurações

# Subir todos os serviços (app + Redis + PostgreSQL)
docker-compose up --build
```

### Build local (sem Docker)

```bash
# Build do projeto
./mvnw clean package -DskipTests

# Executar com perfil local (usa H2)
./mvnw spring-boot:run
```

A aplicação sobe na porta configurada em `application.yaml` (padrão `8080`).

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## 11. Variáveis de Ambiente

Referência baseada em `.env.example`:

| Variável | Descrição | Exemplo |
|---|---|---|
| `jwt.secret` | Chave secreta para assinar o JWT (HS256) | `sua-chave-secreta-longa` |
| `jwt.expiration-ms` | Validade do token em milissegundos | `300000` (5 min) |
| `spring.redis.host` | Host do Redis | `localhost` |
| `spring.redis.port` | Porta do Redis | `6379` |
| `spring.datasource.url` | URL do banco de dados (produção) | `jdbc:postgresql://localhost:5432/db` |
| `spring.datasource.username` | Usuário do banco | `postgres` |
| `spring.datasource.password` | Senha do banco | `senha` |
| `rate-limit.max-requests` | Máximo de tentativas de login por IP/janela | `5` |
| `rate-limit.window-seconds` | Duração da janela de rate limit (segundos) | `60` |

---

## 12. Estrutura de Pastas

```
src/main/java/com/br/itau/login/
├── LoginApplication.java          # Classe principal Spring Boot
├── adapters/
│   └── UserRepositoryAdapter.java # Adaptador Hexagonal JPA
├── config/
│   ├── DataInitializer.java       # Seed de usuário inicial
│   ├── RedisConfig.java           # Configuração do Redis/Lettuce
│   └── SecurityConfig.java        # SecurityFilterChain, BCrypt, AuthManager
├── controller/
│   ├── Login.java                 # Interface do controller /auth
│   ├── LoginImpl.java             # Implementação @RestController
│   └── contract/
│       ├── ContractController.java
│       └── ContractControllerIml.java
├── domains/
│   └── UserRepositoryDomain.java  # Interface de domínio (Port)
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── UserNotFoundException.java
├── model/
│   ├── SessionDTO.java
│   ├── entity/
│   │   └── UserAccount.java       # Entidade JPA (@Entity)
│   ├── enums/
│   │   └── Role.java              # Enum USER, ADMIN
│   ├── request/
│   │   ├── AuthRequest.java       # (não utilizado em controllers)
│   │   └── LoginRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── MeResponseDTO.java
│       └── errors/
│           └── ErrorResponse.java
├── repository/
│   └── UserAccountRepository.java # JpaRepository
├── security/
│   ├── AuthenticationFailureEventListener.java
│   ├── ContractAuthorizationFilter.java
│   ├── JwtAuthenticationFilter.java
│   ├── LoginRateLimitFilter.java
│   └── UserDetailsServiceImpl.java
├── service/
│   ├── ContractService.java
│   ├── ContractServiceImpl.java   # ⚠️ Implementação incompleta
│   ├── JwtService.java
│   ├── JwtServiceImpl.java
│   ├── LoginRateLimiter.java
│   ├── LoginService.java
│   ├── LoginServiceImpl.java
│   ├── MeService.java
│   ├── MeServiceImpl.java
│   ├── SessionService.java
│   └── SessionServiceImpl.java
└── utils/
    ├── IpUtils.java               # Extração de IP (X-Forwarded-For)
    └── SessionUtils.java          # Geração de sessionId, symmetricKey, token
```

---

## 13. Pendências e Melhorias

Os itens abaixo foram identificados com base exclusivamente no código atual:

| # | Pendência | Impacto |
|---|---|---|
| 1 | **Endpoint `/auth/logout` ausente** — `SessionService.delete()` existe mas nenhum controller o invoca | Alto — sem logout, sessões só expiram pelo TTL do Redis |
| 2 | **Endpoint `/auth/refresh` ausente** — não há lógica de renovação de token | Médio — usuário precisa fazer login novamente ao expirar |
| 3 | **`LoginRateLimitFilter` usa path `/api/v1/auth/login`** mas o endpoint real é `/auth/login` — rate limit pode não estar sendo aplicado | Alto — segurança |
| 4 | **`ContractAuthorizationFilter` usa path `/api/v1/contract`** mas o endpoint real é `/contract` — filtro de autorização pode não executar | Alto — segurança |
| 5 | **`ContractServiceImpl` com implementação vazia** — corpo do método `contract()` e injeção de dependência estão comentados | Médio |
| 6 | **`AuthRequest` (email + password) não utilizado** — classe existe mas nenhum controller a referencia | Baixo — dead code |
| 7 | **Role `ADMIN` sem proteção de rota** — declarada no enum mas sem `hasRole("ADMIN")` aplicado em nenhuma rota | Médio |
| 8 | **Credenciais hardcoded em `DataInitializer`** — `username=Thiago`, `password=231299` fixas no código-fonte | Alto — segurança em produção |
| 9 | **`MeServiceImpl` recebe `sessionId` no lugar do token JWT** — `LoginImpl` passa `session.getSessionId()` como argumento `token`, então `extractChannelFromToken` sempre retorna `null`; o channel é sobrescrito para `"MOBILE"` no controller | Baixo — funcional, mas lógica inconsistente |

---

## Resumo Rápido (YAML)

```yaml
sistema:
  nome: LoginService
  grupo: com.br.itau
  versao: 0.0.2

autenticacao:
  tipo: JWT
  algoritmo: HS256
  sessao_backend: Redis (stateful)
  encoder_senha: BCrypt

endpoints:
  login: "POST /auth/login"
  me: "GET /auth/me"
  contract: "POST /contract"
  health: "GET /actuator/health"
  logout: NÃO IMPLEMENTADO
  refresh: NÃO IMPLEMENTADO

roles:
  - USER
  - ADMIN

banco_de_dados:
  producao: PostgreSQL
  dev_local: H2 (in-memory)
  entidade_principal: UserAccount (tabela: users)

cache_sessao:
  tecnologia: Redis via Lettuce
  chave_sessao: "session:{sessionId}"
  chave_rate_limit: "rate_limit:login:{ip}"
```
