# Nota de Avaliação — Desenho de Solução

**Serviço avaliado:** LoginService  
**Data:** 24 de Abril de 2026  
**Arquitetura analisada:**

```
LoginService → Redis → JWT
PixService   → chama /me → SQN
GameService  → consome evento
```

---

## 1. Visão Geral da Arquitetura

O LoginService é um serviço de autenticação baseado em Spring Boot (Java 21) que utiliza JWT (JSON Web Tokens) para autenticação stateless e Redis como store de sessões. A arquitetura envolve três serviços interdependentes: **LoginService**, **PixService** e **GameService**, comunicando-se por chamadas HTTP e eventos.

---

## 2. Pontos Positivos

- **Sessão stateless com JWT:** Arquitetura correta para microsserviços. O JWT carrega as informações necessárias (sessionId, role, contractService), evitando consultas desnecessárias ao banco em cada requisição.
- **Redis como store de sessão:** Uso inteligente do Redis para invalidação de sessão sem depender de estado in-memory. Permite escalar horizontalmente o LoginService sem perda de sessões.
- **BCrypt para senhas:** Uso de BCryptPasswordEncoder é a prática recomendada para hash de senhas.
- **Spring Security + filtro JWT customizado:** Integração adequada com o ecossistema Spring Security.
- **Threads virtuais (Java 21):** Uso de threads virtuais melhora a capacidade de concorrência sem overhead de threads do sistema operacional.
- **Separação de responsabilidades:** Boa separação entre controller, service, domain e repository (arquitetura hexagonal parcial).

---

## 3. Problemas Identificados e Recomendações

### 3.1 CRÍTICO — symmetricKey exposta no endpoint /me

**Problema:**  
O endpoint `GET /auth/me` retorna o `SessionDTO` diretamente, incluindo o campo `symmetricKey`. Essa chave é um segredo interno gerado por sessão e **não deve ser exposta à camada de transporte**.

**Impacto:**  
Qualquer serviço que chame `/me` (como o PixService) receberá essa chave simétrica. Se o tráfego não for criptografado (HTTPS ausente), essa chave pode ser interceptada. Além disso, a exposição desnecessária de segredos viola o princípio do menor privilégio.

**Recomendação:**  
Criar um `MeResponseDTO` sem o campo `symmetricKey` e retorná-lo no endpoint `/me`.

```java
// MeResponseDTO (sem symmetricKey)
public record MeResponseDTO(String sessionId, String username, Boolean contractService, String role) {}
```

---

### 3.2 CRÍTICO — JWT secret hardcoded no application.yaml

**Problema:**  
O segredo JWT (`ChangeThisSecretKeyForProdUseAtLeast32Chars!`) está hardcoded no `application.yaml` e no `.env`. Qualquer pessoa com acesso ao repositório pode forjar tokens JWT válidos.

**Impacto:**  
Comprometimento total da autenticação. Um atacante pode gerar JWTs válidos para qualquer usuário.

**Recomendação:**  
- Usar variável de ambiente injetada pelo orquestrador (Kubernetes Secrets, AWS Secrets Manager, HashiCorp Vault).
- Nunca commitar segredos no repositório, mesmo em arquivos `.env`.

---

### 3.3 ALTO — Ausência de rate limiting no endpoint de login

**Problema:**  
O endpoint `POST /auth/login` não possui proteção contra ataques de força bruta. Nos testes realizados, foi possível realizar 10+ tentativas consecutivas sem qualquer bloqueio ou throttling.

**Impacto:**  
Vulnerabilidade a ataques de força bruta e credential stuffing, especialmente para o usuário `Thiago` com senha numérica simples (`231299`).

**Recomendação:**  
Implementar rate limiting com `Bucket4j` + Redis ou no nível do API Gateway. Sugestão: máximo 5 tentativas por IP em 60 segundos.

---

### 3.4 ALTO — Ausência de HTTPS em produção

**Problema:**  
O serviço não possui configuração de TLS/SSL. Todo o tráfego, incluindo tokens JWT, é transmitido em texto claro.

**Impacto:**  
Tokens JWT podem ser interceptados em ataques man-in-the-middle. O header `Strict-Transport-Security` (HSTS) também está ausente.

**Recomendação:**  
Configurar TLS no Load Balancer/API Gateway que faz frente ao serviço, ou configurar SSL diretamente no Spring Boot com `server.ssl.*`.

---

### 3.5 ALTO — PixService chama /me sem contrato claro de versionamento

**Problema:**  
O `PixService` chama `/me` para obter informações do usuário autenticado. Se o contrato do endpoint `/me` mudar (ex: renomear campos, remover `symmetricKey`), o PixService pode quebrar sem aviso.

**Impacto:**  
Acoplamento forte entre PixService e LoginService. Uma mudança no LoginService pode derrubar o PixService em produção.

**Recomendação:**  
- Versionar o endpoint: `/api/v1/auth/me` → `/api/v2/auth/me`.
- Criar um contrato OpenAPI (Swagger) versionado.
- Implementar testes de contrato (Consumer-Driven Contract Testing, ex: Pact).

---

### 3.6 ALTO — Redis sem alta disponibilidade

**Problema:**  
O Redis está configurado como instância única (single-node). Toda a gestão de sessões depende deste único nó.

**Impacto:**  
Uma falha no Redis derruba toda a capacidade de autenticação. Nenhum usuário conseguirá fazer login ou ter sessões validadas.

**Recomendação:**  
Configurar Redis em modo Sentinel (1 master + 2 replicas) ou Redis Cluster para alta disponibilidade. Configurar `spring.data.redis.sentinel.*` ou `spring.data.redis.cluster.*`.

---

### 3.7 MÉDIO — GameService consome eventos sem rastreabilidade

**Problema:**  
O `GameService` consome eventos (presumivelmente de uma fila/tópico), mas o desenho de solução não especifica: o mecanismo de transporte (Kafka, RabbitMQ, SQS?), schema dos eventos, versionamento de eventos, dead-letter queue (DLQ), ou rastreabilidade (correlation ID).

**Impacto:**  
Eventos perdidos, sem DLQ, podem causar perda silenciosa de dados. Sem correlation ID, é impossível rastrear uma transação end-to-end nos logs.

**Recomendação:**  
- Definir o mecanismo de mensageria (Kafka recomendado para volume alto).
- Definir schema dos eventos com Apache Avro ou JSON Schema.
- Implementar DLQ para reprocessamento de eventos com falha.
- Incluir `correlationId` e `traceId` em todos os eventos.

---

### 3.8 MÉDIO — Fluxo PixService → /me → SQN sem fallback

**Problema:**  
O PixService chama `/me` do LoginService de forma síncrona. Se o LoginService estiver indisponível, toda a operação PIX falha. Não há indicação de circuit breaker, retry ou fallback.

**Impacto:**  
Falha em cascata: uma indisponibilidade do LoginService derruba o PixService.

**Recomendação:**  
- Implementar circuit breaker com Resilience4j (`@CircuitBreaker`).
- Cache local no PixService para sessões recentemente validadas (TTL curto).
- Retry com backoff exponencial para falhas transitórias.

---

### 3.9 MÉDIO — SessionUtils com estado estático mutável

**Problema:**  
A classe `SessionUtils` utiliza variáveis estáticas (`static SessionService`, `static JwtService`) para manter referências a beans Spring. Isso é anti-padrão e não é thread-safe em cenários de múltiplos contextos Spring (ex: testes paralelos, hot-reload).

**Impacto:**  
Condições de corrida em ambientes com múltiplos context loaders. Dificulta testes unitários isolados.

**Recomendação:**  
Eliminar o uso de estado estático. Injetar `SessionService` e `JwtService` diretamente em `LoginServiceImpl` via construtor (já é feito parcialmente, mas a dependência de `SessionUtils` estático pode ser removida).

---

### 3.10 BAIXO — Dupla consulta ao banco no login

**Problema:**  
Em `LoginServiceImpl.login()`, há duas consultas ao banco:
1. O `AuthenticationManager.authenticate()` internamente chama `UserDetailsServiceImpl` que faz um `findByUsername`.
2. Após a autenticação, há uma segunda chamada explícita `userRepositoryPort.findByUsername()`.

**Impacto:**  
Latência desnecessária no login e carga extra no banco de dados.

**Recomendação:**  
Usar o `UserDetails` retornado pela autenticação ou cachear o resultado da primeira consulta.

---

### 3.11 BAIXO — H2 Console e usuário inicial hardcoded

**Problema:**  
O `DataInitializer` cria um usuário `Thiago` com senha `231299` hardcoded. O H2 console está habilitado em `application.yaml`.

**Impacto:**  
Credenciais padrão conhecidas e console de banco expostos em produção.

**Recomendação:**  
- Remover `DataInitializer` ou torná-lo condicional (`@Profile("dev")`).
- Desabilitar H2 console em produção (`spring.h2.console.enabled: false`).

---

## 4. Avaliação do Desenho de Solução

| Dimensão | Nota | Justificativa |
|---|---|---|
| Autenticação (JWT + Redis) | 7/10 | Correto e moderno, mas com falhas críticas (secret hardcoded, symmetricKey exposta) |
| Segurança | 5/10 | Ausência de rate limiting, HTTPS, HSTS e headers de segurança |
| Resiliência | 5/10 | Redis single-node, sem circuit breaker entre serviços, sem DLQ |
| Rastreabilidade | 4/10 | Logs básicos presentes, mas sem correlationId, tracing distribuído ou APM |
| Acoplamento entre serviços | 5/10 | PixService acoplado sincronamente ao LoginService sem fallback |
| Qualidade do código | 7/10 | Boa estrutura, mas SessionUtils estático e dupla consulta ao banco |
| **Nota Geral** | **5.5/10** | **Base sólida com problemas críticos a corrigir antes de produção** |

---

## 5. Diagrama de Problemas

```
                    ┌─────────────────────────────────────────┐
                    │           PROBLEMAS CRÍTICOS            │
                    └─────────────────────────────────────────┘

[LoginService] ──→ [Redis (single-node)] ──→ [JWT (secret hardcoded)]
      │                    │                         │
      │              SEM HA/Sentinel           NÃO usar .yaml
      │
      ├──→ POST /auth/login   ←── SEM rate limiting (força bruta)
      │
      └──→ GET /auth/me       ←── EXPÕE symmetricKey (CRÍTICO)
                │
                ↓
         [PixService] ──→ SQN  ←── SEM circuit breaker / fallback
                │
                │         ┌──────────────────────────────────┐
                │         │   Acoplamento síncrono rígido    │
                └─────────→ Se LoginService cair → PIX cai  │
                          └──────────────────────────────────┘

[GameService] ←── Consome eventos
                    │
                    └──→ Mecanismo não especificado
                         SEM DLQ, SEM schema, SEM correlationId
```

---

## 6. Roadmap de Correções Recomendado

### Sprint 1 (Bloqueadores para Produção)
- [ ] Remover `symmetricKey` do response do `/me` → criar `MeResponseDTO`
- [ ] Mover JWT secret para variável de ambiente segura
- [ ] Implementar rate limiting no `/auth/login` (Bucket4j)
- [ ] Configurar HTTPS/TLS (no mínimo no API Gateway)

### Sprint 2 (Resiliência e Segurança)
- [ ] Redis Sentinel ou Cluster
- [ ] Circuit breaker no PixService (Resilience4j)
- [ ] Headers de segurança: HSTS, CSP, Referrer-Policy
- [ ] Remover `DataInitializer` ou torná-lo `@Profile("dev")`

### Sprint 3 (Qualidade e Observabilidade)
- [ ] Definir mecanismo de mensageria do GameService (Kafka + DLQ)
- [ ] Implementar tracing distribuído (OpenTelemetry + Jaeger/Zipkin)
- [ ] Contrato OpenAPI versionado + testes de contrato (Pact)
- [ ] Eliminar SessionUtils estático
- [ ] Corrigir dupla consulta ao banco no login

---

*Documento gerado por análise de código-fonte, testes de segurança (EHT) e testes de stress.*  
*LoginService — Itaú — Avaliação de Arquitetura*
