# Guia Completo do Código — OptiRoute API
> Explicação para quem nunca programou antes + seção de estudos

---

## 1. O que é este projeto?

Uma padaria tem 10 entregas para fazer hoje. Qual é a ordem ideal para o motoboy gastar menos tempo e gasolina?

Este projeto é uma **API** que resolve esse problema: recebe endereços de entrega de uma loja, converte em coordenadas geográficas e calcula a sequência mais eficiente usando um serviço externo de mapas.

**É um projeto acadêmico** — sem fins comerciais.

---

## 2. O que é uma API?

Pense num restaurante:
- O **cliente** (Postman, app, outro sistema) faz o pedido
- O **garçom** (API) anota e entrega
- A **cozinha** (backend) processa

Os "pratos do cardápio" são os **endpoints** — endereços que a API escuta:

| Endpoint | O que faz |
|---|---|
| `POST /api/auth/register` | Cadastrar loja |
| `POST /api/auth/login` | Fazer login |
| `POST /api/deliveries` | Cadastrar entrega |
| `GET /api/deliveries` | Listar entregas |
| `PATCH /api/deliveries/{id}/status` | Atualizar status |
| `POST /api/routes/optimize` | Calcular rota otimizada |
| `GET /api/routes/{id}` | Ver detalhes da rota |
| `DELETE /api/test/reset` | Resetar dados de teste |

---

## 3. Arquitetura: como as peças se encaixam

```
┌──────────────────────────────────────────────────────┐
│                 POSTMAN / APP / SISTEMA               │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP
┌──────────────────────▼───────────────────────────────┐
│                NOSSA API (Spring Boot)                │
│                                                       │
│  Controller → Service → Repository → Banco           │
│       ↑           ↓                                   │
│    JWT Auth    Redis Cache                            │
└───────────────────────────────────────────────────────┘
         │ JPA/Hibernate          │ RestClient
┌────────▼────────┐    ┌──────────▼──────────┐
│  PostgreSQL +   │    │  OpenRouteService   │
│    PostGIS      │    │  (geocoding + rota) │
└─────────────────┘    └─────────────────────┘
```

**Fluxo de uma entrega:**
1. Loja faz login → recebe token JWT
2. Loja cadastra entrega com endereço em texto
3. Sistema verifica Redis: endereço já foi geocodificado?
   - SIM → usa coordenadas do cache
   - NÃO → chama ORS API → salva no Redis → usa as coordenadas
4. Salva entrega no PostgreSQL com status PENDENTE
5. Loja chama `/optimize` → sistema busca entregas PENDENTES da loja
6. Manda coordenadas para ORS → recebe sequência ideal
7. Cria Rota no banco, muda entregas para EM_TRANSITO com ordem 1, 2, 3...

---

## 4. Estrutura de pastas

```
src/main/java/com/lastmile/optiroute/
├── OptiRouteApplication.java     ← ponto de entrada
├── domain/
│   ├── entity/
│   │   ├── Store.java            ← tabela lojas
│   │   ├── Delivery.java         ← tabela entregas
│   │   └── Route.java            ← tabela rotas
│   └── enums/
│       ├── DeliveryStatus.java   ← PENDING, IN_TRANSIT, DELIVERED, FAILED
│       └── RouteStatus.java      ← ACTIVE, COMPLETED
├── repository/
│   ├── StoreRepository.java
│   ├── DeliveryRepository.java
│   └── RouteRepository.java
├── security/
│   ├── JwtService.java           ← gera e valida tokens
│   ├── JwtAuthFilter.java        ← intercepta requisições
│   ├── SecurityConfig.java       ← configuração de segurança
│   └── CustomUserDetailsService.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── DeliveryRequest.java
│   ├── DeliveryResponse.java
│   ├── RouteResponse.java
│   └── StatusUpdateRequest.java
├── client/
│   ├── GeocodingClient.java      ← chama ORS para geocoding
│   └── OptimizationClient.java   ← chama ORS para otimizar rota
├── service/
│   ├── AuthService.java
│   ├── DeliveryService.java
│   ├── RouteService.java
│   └── GeocodingCacheService.java
├── controller/
│   ├── AuthController.java
│   ├── DeliveryController.java
│   ├── RouteController.java
│   └── TestController.java
└── exception/
    ├── ResourceNotFoundException.java
    ├── ForbiddenException.java
    └── GlobalExceptionHandler.java
```

---

## 5. Os arquivos de configuração

### pom.xml — lista de dependências (Maven)

```xml
spring-boot-starter-web       → faz o programa virar servidor HTTP
spring-boot-starter-data-jpa  → conecta Java com banco de dados
hibernate-spatial             → suporte a coordenadas geográficas
spring-boot-starter-security  → autenticação e autorização
spring-boot-starter-validation → valida campos dos formulários
spring-boot-starter-data-redis → cache em memória (Redis)
postgresql                    → driver para conectar no PostgreSQL
flyway-core                   → controle de versão do banco
jjwt-api/impl/jackson         → geração e validação de tokens JWT
lombok                        → elimina código repetitivo
```

### docker-compose.yml — os containers

```yaml
postgres:  PostgreSQL 15 com PostGIS 3.3 — banco de dados principal
redis:     Redis 7 Alpine — cache em memória
```

Um `docker compose up -d` sobe os dois.

### application.yml — variáveis de configuração

Usa o padrão `${VARIAVEL:valor_padrão}`:
- Em **desenvolvimento**: usa os valores padrão (localhost, senha simples)
- Em **produção**: define variáveis de ambiente reais

```yaml
spring.datasource → conexão com PostgreSQL
spring.data.redis → conexão com Redis
spring.jpa.hibernate.ddl-auto: validate → Hibernate só valida, não cria tabelas
spring.flyway → controle de migrações do banco
app.jwt.secret → chave para assinar os tokens JWT
app.geocoding.api-key → chave da API OpenRouteService
```

---

## 6. Banco de dados (V1__create_initial_schema.sql)

### Por que Flyway?

Sem controle de versão, se você mudar o banco e um colega não souber, o projeto quebra. O Flyway executa os arquivos SQL numerados em ordem — todo mundo fica com o mesmo banco.

### As tabelas

**stores** (lojas)
```
id           → UUID gerado automaticamente (identificador único)
name         → nome da loja
email        → único — não pode ter dois iguais
password     → hash bcrypt da senha (nunca texto puro)
address_text → endereço em texto ("Rua XV, 100, Curitiba, PR")
location     → ponto geográfico PostGIS (latitude/longitude)
created_at   → data de criação automática
```

**deliveries** (entregas)
```
id             → UUID
store_id       → qual loja criou essa entrega (FK → stores)
route_id       → qual rota ela pertence — NULO até ser otimizada (FK → routes)
customer_name  → nome do cliente
address_text   → endereço de entrega em texto
location       → coordenadas do destino (PostGIS Point)
delivery_order → posição na rota: 1=primeira, 2=segunda...
status         → PENDING | IN_TRANSIT | DELIVERED | FAILED
created_at     → data de criação
```

**routes** (rotas calculadas)
```
id                    → UUID
store_id              → qual loja gerou essa rota (FK → stores)
total_distance_meters → distância total em metros
total_time_seconds    → tempo estimado em segundos
status                → ACTIVE | COMPLETED
created_at            → data de criação
```

**Índices:**
```sql
-- índices normais: buscas por store_id, status, route_id ficam rápidas
CREATE INDEX idx_deliveries_store_id ON deliveries(store_id);
CREATE INDEX idx_deliveries_status   ON deliveries(status);

-- índices espaciais GIST: buscas geográficas ficam rápidas
CREATE INDEX idx_stores_location     ON stores USING GIST(location);
CREATE INDEX idx_deliveries_location ON deliveries USING GIST(location);
```

---

## 7. As Entidades (Domain Layer)

Entidade = classe Java que espelha uma tabela do banco. O JPA traduz automaticamente.

### Store.java
```java
@Entity              → "esta classe é uma tabela"
@Table(name="stores")→ nome da tabela no banco
@Getter / @Setter    → Lombok gera getId(), getName(), setId(), setName()...
@Builder             → permite Store.builder().name("X").email("Y").build()
@NoArgsConstructor   → construtor vazio (JPA exige)
@AllArgsConstructor  → construtor com todos os campos

@Id + @GeneratedValue → chave primária gerada como UUID
@Column(unique=true)  → email único no banco
@Column(name="address_text") → campo Java "addressText" = coluna "address_text"
geometry(Point, 4326) → tipo PostGIS; 4326 = sistema GPS (WGS84)

@PrePersist → método que roda automaticamente antes de salvar no banco
```

### Delivery.java
```java
@ManyToOne → muitas entregas pertencem a uma loja
FetchType.LAZY → só carrega a loja do banco quando você chamar .getStore()
@JoinColumn(name="route_id") → sem nullable=false → pode ser null (entrega sem rota)
@Enumerated(EnumType.STRING) → salva "PENDING" no banco, não número
@Builder.Default → valor padrão do Builder = PENDING
```

### Route.java
```java
@OneToMany(mappedBy="route") → uma rota tem muitas entregas
                               "mappedBy" diz quem é o dono da relação
@OrderBy("deliveryOrder ASC") → quando buscar entregas, ordena por ordem crescente
```

### DeliveryStatus / RouteStatus
```
enum = tipo com valores fixos
Evita salvar "pendente", "Pendente", "PENDENTE" aleatoriamente
Força sempre um valor válido do conjunto definido
```

---

## 8. Repositórios (Repository Layer)

Interface que diz "quero buscar assim do banco" — o JPA gera o SQL sozinho.

```java
// Spring Data JPA lê o nome do método e gera o SQL
findByEmail(String email)
→ SELECT * FROM stores WHERE email = ?

findByStoreIdAndStatus(UUID storeId, DeliveryStatus status)
→ SELECT * FROM deliveries WHERE store_id = ? AND status = ?

findByIdAndStoreId(UUID id, UUID storeId)
→ SELECT * FROM deliveries WHERE id = ? AND store_id = ?
// garante que a entrega pertence à loja — segurança multitenancy

// query manual com JOIN FETCH — carrega a rota e suas entregas em 1 SQL
@Query("SELECT r FROM Route r LEFT JOIN FETCH r.deliveries WHERE r.id=:id AND r.store.id=:storeId")
findByIdAndStoreIdWithDeliveries(...)
// sem isso, o Hibernate carrega lazy e pode retornar lista vazia por cache
```

---

## 9. Segurança — JWT (security/)

### O que é JWT?

Quando você faz login no Instagram, ele te dá um "cartão de acesso" que fica no seu celular. Toda vez que você abre o app, apresenta esse cartão. JWT é esse cartão — um token criptografado.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQw...
─────────────────── ─────────────────────── ─────────
    cabeçalho              payload             assinatura
  (algoritmo)          (dados: id da loja)   (prova que é válido)
```

### JwtService.java
```java
generateToken(Store store)
→ cria token com o id da loja dentro, assinado com chave HMAC-SHA256
→ expira em 24h (configurado no application.yml)

extractStoreId(token)
→ abre o token e retorna o id da loja que está dentro

isValid(token)
→ verifica se o token não foi adulterado e não expirou
```

### JwtAuthFilter.java
Roda em toda requisição antes de chegar no Controller:
```
1. Pega o header "Authorization: Bearer eyJ..."
2. Remove o "Bearer " → fica só o token
3. Valida o token
4. Extrai o id da loja
5. Coloca no contexto de segurança do Spring
→ agora o Controller sabe quem está fazendo a requisição
```

### SecurityConfig.java
```java
.csrf(disable)              → API sem cookies não precisa de CSRF
.sessionManagement(STATELESS) → não guarda sessão no servidor (cada req traz o token)
.requestMatchers("/api/auth/**").permitAll() → login e cadastro são públicos
.anyRequest().authenticated()               → todo resto exige token
```

### CustomUserDetailsService.java
O Spring Security pede: "como eu carrego o usuário do banco?"
→ Respondemos: "busca a loja pelo UUID que está no token"

---

## 10. DTOs (dto/)

**DTO = Data Transfer Object** — objetos simples para trafegar dados pela API.

Por que não usar as entidades diretamente?
- Entidade tem `password` — você não quer expor isso na resposta
- Entidade tem relacionamentos lazy — pode dar erro de serialização
- DTO é o "formulário" padronizado de entrada/saída

```java
// Java 16+: record = classe imutável sem boilerplate
public record RegisterRequest(
    @NotBlank String name,      // @NotBlank = não pode ser vazio
    @NotBlank @Email String email,  // @Email = valida formato email
    @NotBlank @Size(min=6) String password,
    @NotBlank String address
) {}

// factory method — converte Entidade → DTO
public static DeliveryResponse from(Delivery d) {
    return new DeliveryResponse(d.getId(), d.getCustomerName(), ...);
}
```

---

## 11. Clients — Integração Externa (client/)

### GeocodingClient.java
```
Endereço em texto → Latitude + Longitude
"Rua Augusta, 500, São Paulo" → [-23.550078, -46.646495]

Endpoint ORS: GET /geocode/search?api_key=X&text=ENDERECO&size=1
Resposta: { "features": [{ "geometry": { "coordinates": [lon, lat] } }] }
```

### OptimizationClient.java
```
Coordenadas → Sequência otimizada

Usa formato VROOM (padrão internacional de otimização de rotas com veículos)
Endpoint ORS: POST /optimization

Payload enviado:
{
  "jobs": [
    { "id": 0, "location": [-46.64, -23.55] },  ← entrega 1
    { "id": 1, "location": [-46.67, -23.56] },  ← entrega 2
    { "id": 2, "location": [-46.69, -23.57] }   ← entrega 3
  ],
  "vehicles": [
    { "id": 1, "profile": "driving-car", "start": [-46.64, -23.55], "end": [...] }
  ]
}

Resposta recebida:
{ "routes": [{ "steps": [{ "type": "job", "id": 2 }, { "type": "job", "id": 0 }, ...] }] }
→ sequência ideal: entrega 2, depois 0, depois 1
```

---

## 12. Services (service/)

A camada com as regras de negócio. Controllers chamam Services. Services chamam Repositories e Clients.

### GeocodingCacheService.java
```
Recebe endereço
→ Normaliza: "Rua X" → "rua x" (lowercase, sem espaços extras)
→ Verifica Redis com chave "geocode:rua x, 500..."
   → Cache HIT: retorna coordenadas salvas (não gasta requisição ORS)
   → Cache MISS: chama ORS API → salva no Redis por 30 dias → retorna
```

### AuthService.java
```
register():
1. Verifica se email já existe → erro 400 se sim
2. Geocodifica endereço da loja
3. Cria ponto geográfico PostGIS
4. Salva loja com senha criptografada (BCrypt)
5. Gera e retorna token JWT

login():
1. Busca loja pelo email → erro 401 se não existe
2. Compara senha com hash BCrypt → erro 401 se não bate
3. Gera e retorna token JWT
```

### DeliveryService.java
```
create(): geocodifica endereço → salva entrega com status PENDING
list():   busca entregas da loja (com filtro opcional por status)
updateStatus(): busca por id AND store_id → garante que loja só mexe nas próprias entregas
```

### RouteService.java
```
optimize() — o método mais complexo do projeto:
1. Busca entregas PENDING da loja
2. Monta lista de jobs com índice como ID (0, 1, 2...)
3. Chama ORS → recebe ordem ideal (ex: [2, 0, 1])
4. Salva Route no banco
5. Para cada entrega na ordem:
   - Define deliveryOrder = 1, 2, 3...
   - Muda status para IN_TRANSIT
   - Vincula à rota
6. Constrói resposta com dados já em memória
   (evita re-buscar no banco e ter problema de cache do Hibernate)
```

---

## 13. Controllers (controller/)

Recebem requisições HTTP e delegam para os Services.

```java
@RestController          → classe que responde requisições HTTP com JSON
@RequestMapping("/api/deliveries") → prefixo de todos os endpoints desta classe

@PostMapping             → responde a POST
@GetMapping              → responde a GET
@PatchMapping("/{id}/status") → responde a PATCH /api/deliveries/{id}/status

@AuthenticationPrincipal UserDetails userDetails
→ Spring injeta automaticamente o usuário logado
→ userDetails.getUsername() = id da loja (vem do token JWT)

@Valid @RequestBody DeliveryRequest request
→ @Valid aciona as validações (@NotBlank, @Email, etc.)
→ @RequestBody lê o JSON do body da requisição
```

---

## 14. Tratamento de Erros (exception/)

Sem um handler global, erros retornariam HTML ou stack trace para o cliente.

```java
@RestControllerAdvice    → intercepta exceções de todos os controllers
@ExceptionHandler(Tipo.class) → captura esse tipo específico de exceção

// Retorna JSON no padrão RFC 7807 (Problem Details):
{
  "type": "urn:problem:not-found",
  "title": "Não Encontrado",
  "status": 404,
  "detail": "Entrega não encontrada"
}
```

| Exceção | HTTP | Quando acontece |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Campo inválido no JSON |
| `IllegalArgumentException` | 400 | Email já cadastrado |
| `BadCredentialsException` | 401 | Senha errada |
| `ResourceNotFoundException` | 404 | Entrega/rota não existe |
| `ForbiddenException` | 403 | Loja acessando dado de outra loja |
| `IllegalStateException` | 422 | Sem entregas PENDING para otimizar |
| `RestClientException` | 502 | Falha na API externa (ORS) |
| `Exception` (genérico) | 500 | Qualquer outro erro |

---

## 15. Estado atual do código vs Especificação

### ✅ Implementado e funcionando
- Cadastro e login com JWT
- Geocoding com cache Redis
- Otimização de rota via ORS
- Multitenancy (isolamento por loja)
- Tratamento de erros RFC 7807

### ⚠️ Gap: Diretriz de Idioma (Seção 8 da spec)
A especificação exige nomes em Português. O código atual usa convenção Java (inglês para nomes de classes/tabelas).

**O que está em Português:** todos os comentários, mensagens de erro, variáveis locais nos métodos.

**O que ficou em inglês (convenção Java/Spring):**
- Nomes de classes: `Store`, `Delivery`, `Route` (mudar exigiria renomear 30+ arquivos)
- Nomes de tabelas/colunas: `stores`, `deliveries`, `store_id` (mudar exigiria nova migration Flyway)
- Valores de enum: `PENDING`, `IN_TRANSIT` (mudar exigiria nova migration pois são salvos como String)

Esta é uma exceção consciente — refatorar causaria mais risco do que benefício para um projeto acadêmico já funcional.

---

## 16. Conceitos e Ferramentas para Estudar

### 🔵 Fundamentos de Backend

**REST (Representational State Transfer)**
- Estilo de arquitetura para APIs via HTTP
- Usa URLs para representar recursos: `/api/deliveries` = "coleção de entregas"
- Usa verbos HTTP para ações: GET=buscar, POST=criar, PATCH=atualizar parcialmente, DELETE=deletar
- Stateless: cada requisição é independente, servidor não guarda sessão
- Recursos para estudar: "REST API Tutorial" (restapitutorial.com)

**HTTP**
- Protocolo de comunicação da web
- Requisição: método + URL + headers + body
- Resposta: status code + headers + body
- Status codes importantes: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Server Error
- Recursos: MDN Web Docs — HTTP

**JSON (JavaScript Object Notation)**
- Formato de texto para trocar dados
- `{ "chave": "valor", "numero": 42, "lista": [1,2,3] }`
- É o "idioma" que a API fala — entrada e saída

---

### 🟢 Java e Spring

**Java 21**
- Linguagem de programação orientada a objetos
- Fortemente tipada: toda variável tem um tipo definido
- Records (Java 16+): classe imutável sem boilerplate — `public record Ponto(int x, int y) {}`
- Var: inferência de tipo local — `var lista = new ArrayList<String>()`
- Lambdas: funções anônimas — `lista.stream().filter(x -> x > 5).toList()`

**Spring Boot**
- Framework que configura automaticamente um servidor web Java
- Inversão de controle (IoC): você declara as dependências, o Spring as injeta
- `@SpringBootApplication` = ponto de entrada + scan automático de componentes
- `@Component`, `@Service`, `@Repository`, `@Controller` = tipos de componentes gerenciados pelo Spring
- `@Autowired` / injeção por construtor = Spring fornece as dependências

**Spring MVC / @RestController**
- Camada web do Spring
- `@RestController` = `@Controller` + `@ResponseBody` (serializa retorno como JSON)
- `@RequestMapping`, `@GetMapping`, `@PostMapping` etc. = mapeiam URLs para métodos
- `@PathVariable` = parâmetro da URL: `/api/deliveries/{id}`
- `@RequestParam` = query parameter: `/api/deliveries?status=PENDING`
- `@RequestBody` = lê o JSON do body da requisição
- `@ResponseStatus` = define o código HTTP da resposta

**Spring Data JPA**
- Abstração sobre JPA/Hibernate
- Repositórios: `extends JpaRepository<Entidade, TipoDaChave>`
- Métodos gerados por nome: `findByEmail`, `findByStoreIdAndStatus`
- `@Query` para consultas JPQL personalizadas
- `@Transactional` = agrupa operações em uma única transação de banco

**Spring Security**
- Framework de autenticação e autorização
- Filter Chain: cadeia de filtros que processa cada requisição
- `SecurityFilterChain` = define as regras de segurança
- `UserDetailsService` = como carregar o usuário do banco
- `PasswordEncoder` = criptografia de senhas (BCrypt)
- `Authentication` = objeto que representa quem está logado
- Session STATELESS = não guarda estado no servidor

---

### 🟡 Banco de Dados

**PostgreSQL**
- Banco de dados relacional open-source
- Tabelas, linhas, colunas, índices, foreign keys
- UUID como chave primária: identificador único global de 128 bits
- `gen_random_uuid()` = gera UUID automaticamente

**PostGIS**
- Extensão espacial do PostgreSQL
- Adiciona tipos: `GEOMETRY`, `POINT`, `POLYGON`, etc.
- SRID 4326 = sistema de coordenadas WGS84 (o mesmo do GPS)
- `GEOMETRY(Point, 4326)` = ponto geográfico (longitude, latitude)
- Índices GIST = índices especializados para dados geográficos

**JPA / Hibernate (ORM)**
- ORM = Object-Relational Mapping
- Traduz objetos Java ↔ tabelas do banco automaticamente
- `@Entity` = classe é uma tabela
- `@Column` = campo é uma coluna
- `@ManyToOne` = chave estrangeira (N registros → 1 registro)
- `@OneToMany` = relacionamento inverso (1 registro → N registros)
- `FetchType.LAZY` = só carrega do banco quando acessado (evita carregar tudo)
- Hibernate 1st-level cache = objetos salvos na sessão atual são cacheados (pode causar bugs)

**Flyway**
- Controle de versão para banco de dados
- Arquivos SQL nomeados: `V1__descricao.sql`, `V2__descricao.sql`...
- Executa cada arquivo uma vez, em ordem, e registra o histórico
- Garante que todos os ambientes (dev, teste, produção) tenham o mesmo schema

---

### 🔴 Segurança

**JWT (JSON Web Token)**
- Token criptografado com 3 partes: header.payload.signature
- Header: algoritmo usado (HMAC-SHA256)
- Payload: dados (id do usuário, email, expiração)
- Signature: garante que o token não foi adulterado
- Stateless: o servidor não precisa guardar sessão — tudo está no token
- Expiração: token tem validade (aqui 24h)

**BCrypt**
- Algoritmo de hash para senhas
- Unidirecional: impossível reverter o hash para obter a senha original
- Salt automático: mesmo a mesma senha gera hashes diferentes
- `passwordEncoder.encode("senha")` → `$2a$10$...` (hash)
- `passwordEncoder.matches("senha", hash)` → true/false

**CSRF (Cross-Site Request Forgery)**
- Ataque onde um site malicioso faz requisições em nome do usuário
- APIs com JWT não precisam de proteção CSRF (sem cookies de sessão)
- Por isso desabilitamos: `.csrf(disable)`

---

### 🟠 Performance e Cache

**Redis**
- Banco de dados em memória RAM (muito mais rápido que disco)
- Estruturas: String, Hash, List, Set, Sorted Set
- TTL (Time To Live): dado expira automaticamente após um tempo
- Uso neste projeto: cache de geocoding (endereço → coordenadas)
- Estratégia Cache-aside: checar cache → se não tem, buscar fonte → salvar no cache

**Cache Strategy (Cache-aside / Lazy Loading)**
```
MISS (primeiro acesso):         HIT (acesso seguinte):
1. Verifica Redis → não tem     1. Verifica Redis → TEM!
2. Busca na API ORS             2. Retorna do Redis (rápido)
3. Salva no Redis (30 dias)     Não chama a API externa
4. Retorna resultado
```

---

### 🟣 Infraestrutura

**Docker**
- Cria containers: ambientes isolados que rodam qualquer programa
- Imagem: "receita" do container (ex: `postgis/postgis:15-3.3`)
- Container: instância rodando da imagem
- Volume: persiste dados mesmo se o container for reiniciado
- Healthcheck: verifica se o serviço está pronto

**Docker Compose**
- Orquestra múltiplos containers
- Define serviços, redes, volumes em um arquivo YAML
- `docker compose up -d` = sobe todos os containers em background
- `docker compose down` = para e remove os containers

**Maven**
- Gerenciador de dependências e build para Java
- `pom.xml` = arquivo de configuração com dependências
- Repositório central: baixa bibliotecas da internet
- Ciclo de vida: `compile` → `test` → `package` → `install`
- `mvn spring-boot:run` = compila e roda o projeto

---

### 🔷 Design Patterns usados neste projeto

**MVC (Model-View-Controller)**
- Model = entidades e DTOs
- Controller = recebe HTTP e chama Services
- View = o próprio JSON de resposta (sem frontend)

**Repository Pattern**
- Abstrai o acesso ao banco
- Controller e Service não sabem SQL — usam o Repository
- Facilita trocar o banco de dados sem mudar a lógica

**DTO Pattern (Data Transfer Object)**
- Separa o que é "interno" (entidade com senha) do que é "externo" (resposta sem senha)
- Controla exatamente o que entra e sai da API

**Builder Pattern**
- `Store.builder().name("X").email("Y").build()`
- Cria objetos complexos de forma legível
- Lombok gera automaticamente com `@Builder`

**Filter Chain Pattern**
- Cadeia de filtros que processa cada requisição
- `JwtAuthFilter` → valida o token antes de chegar no Controller

**Dependency Injection (IoC)**
- Spring injeta as dependências nos construtores
- Não precisa fazer `new Servico()` — o Spring cuida disso
- Facilita testes: você pode injetar um mock em vez do objeto real

---

### 🌐 APIs Externas

**OpenRouteService (ORS)**
- Serviço de mapas open-source (alternativa ao Google Maps)
- Endpoint de geocoding: endereço em texto → coordenadas
- Endpoint de otimização: coordenadas → sequência ideal de visita
- Formato VROOM: padrão de otimização de rotas com veículos (Vehicle Routing Problem)

**RestClient (Spring 6)**
- Cliente HTTP moderno do Spring
- Substitui `RestTemplate` e `WebClient` para chamadas síncronas
- Builder pattern: `RestClient.builder().baseUrl(url).build()`
- `.get().uri(...).retrieve().body(Classe.class)` = faz GET e deserializa resposta

---

### 📐 Boas Práticas aplicadas

- **Multitenancy**: isolamento por loja — uma loja nunca vê dados de outra
- **Segurança de ownership**: `findByIdAndStoreId` garante que loja só acessa seus dados
- **Fail-fast**: validação na entrada (`@Valid`) antes de processar
- **Mensagens de erro padronizadas**: RFC 7807 Problem Details
- **Configuração por ambiente**: variáveis de ambiente para segredos
- **Cache inteligente**: não chama API externa se já tem o resultado
- **Transações atômicas**: `@Transactional` garante consistência no banco
