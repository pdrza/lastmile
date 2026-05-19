# Status do Projeto — OptiRoute API
**Última atualização:** 2026-05-19

---

## Estado atual: FUNCIONAL + FRONTEND EM DESENVOLVIMENTO ✅

API completamente operacional e testada via Postman. Testes unitários implementados. Interface React criada e rodando em localhost:5173.

---

## O que foi implementado

### Infraestrutura Backend
| Arquivo | Status |
|---|---|
| `pom.xml` | ✅ Dependências completas |
| `docker-compose.yml` | ✅ PostgreSQL 15 PostGIS + Redis 7 |
| `application.yml` | ✅ Configuração via env vars com fallback dev |
| `V1__create_initial_schema.sql` | ✅ Schema completo com índices GIST |

### Domain Layer
| Arquivo | Status |
|---|---|
| `Store.java` | ✅ Entidade JPA com Point PostGIS |
| `Delivery.java` | ✅ FK para Store e Route (nullable), enum status |
| `Route.java` | ✅ OneToMany ordenado por deliveryOrder |
| `DeliveryStatus.java` | ✅ PENDING, IN_TRANSIT, DELIVERED, FAILED |
| `RouteStatus.java` | ✅ ACTIVE, COMPLETED |

### Repository Layer
| Arquivo | Status |
|---|---|
| `StoreRepository.java` | ✅ findByEmail |
| `DeliveryRepository.java` | ✅ findByStoreId, findByStoreIdAndStatus, findByIdAndStoreId |
| `RouteRepository.java` | ✅ findByIdAndStoreId, findByIdAndStoreIdWithDeliveries (JOIN FETCH) |

### Security Layer
| Arquivo | Status |
|---|---|
| `JwtService.java` | ✅ generateToken, extractStoreId, isValid — HMAC-SHA256 |
| `JwtAuthFilter.java` | ✅ OncePerRequestFilter — extrai token do header |
| `SecurityConfig.java` | ✅ Stateless, /api/auth/** público, CORS configurado para localhost:5173 |
| `CustomUserDetailsService.java` | ✅ Carrega Store por UUID |

### DTO Layer
| Arquivo | Status |
|---|---|
| `RegisterRequest.java` | ✅ Validações @NotBlank, @Email, @Size |
| `LoginRequest.java` | ✅ |
| `LoginResponse.java` | ✅ |
| `DeliveryRequest.java` | ✅ |
| `DeliveryResponse.java` | ✅ Inclui latitude, longitude, deliveryOrder |
| `RouteResponse.java` | ✅ Inclui lista de entregas ordenadas |
| `StatusUpdateRequest.java` | ✅ |

### Client Layer (Integração Externa)
| Arquivo | Status |
|---|---|
| `GeocodingClient.java` | ✅ ORS /geocode/search — retorna [lon, lat] |
| `OptimizationClient.java` | ✅ ORS /optimization formato VROOM com profile "driving-car" |

### Service Layer
| Arquivo | Status |
|---|---|
| `GeocodingCacheService.java` | ✅ Redis check → ORS → Redis set (TTL 30 dias) |
| `AuthService.java` | ✅ register (geocodifica loja) + login (BCrypt) |
| `DeliveryService.java` | ✅ create, list, updateStatus com ownership check |
| `RouteService.java` | ✅ optimize (@Transactional), getById — resposta montada em memória |

### Controller Layer
| Arquivo | Status |
|---|---|
| `AuthController.java` | ✅ POST /api/auth/register, /login |
| `DeliveryController.java` | ✅ POST, GET, PATCH /api/deliveries |
| `RouteController.java` | ✅ POST /api/routes/optimize, GET /api/routes/{id} |
| `TestController.java` | ✅ DELETE /api/test/reset (facilita testes) |

### Exception Layer
| Arquivo | Status |
|---|---|
| `ResourceNotFoundException.java` | ✅ 404 |
| `ForbiddenException.java` | ✅ 403 |
| `GlobalExceptionHandler.java` | ✅ RFC 7807, captura RestClientException (502), loga erros |

### Testes Unitários
| Arquivo | Status |
|---|---|
| `OptiRouteApplicationTests.java` | ✅ context loads |
| `AuthServiceTest.java` | ✅ 5 testes — registro, login, email duplicado, senha errada |
| `DeliveryServiceTest.java` | ✅ 7 testes — criar, listar, filtrar, atualizar, ownership |
| `GeocodingCacheServiceTest.java` | ✅ 4 testes — cache hit/miss, normalização de chave |
| `RouteServiceTest.java` | ✅ 5 testes — ordem das entregas, status IN_TRANSIT, 404 |

**Total: 22 testes — BUILD SUCCESS**

### Frontend React (em desenvolvimento)
| Arquivo | Status |
|---|---|
| `package.json` | ✅ React 19 + Vite 8 + Tailwind 3 + Leaflet + Axios + React Router |
| `tailwind.config.js` | ✅ Paleta cyberpunk configurada (roxo/magenta) |
| `vite.config.js` | ✅ Plugin React |
| `src/index.css` | ✅ Tailwind + grade de pixels no fundo + scrollbar customizada |
| `src/main.jsx` | ✅ Router + proteção de rota (redireciona para /login sem token) |
| `src/services/api.js` | ✅ Axios com interceptor JWT e redirecionamento 401 |
| `src/pages/Login.jsx` | ✅ Toggle Entrar/Cadastrar, estética cyberpunk |
| `src/pages/Dashboard.jsx` | ✅ Lista entregas + mapa + botão otimizar + filtros |
| `src/components/StatusBadge.jsx` | ✅ Badge colorido por status |
| `src/components/DeliveryCard.jsx` | ✅ Card clicável com highlight |
| `src/components/NewDeliveryForm.jsx` | ✅ Formulário de cadastro com feedback |
| `src/components/RouteMap.jsx` | ✅ Mapa Leaflet tema escuro + pins coloridos + polyline rota |

---

## Bugs corrigidos durante desenvolvimento

| Bug | Causa | Solução |
|---|---|---|
| `deliveries: []` no optimize | Hibernate 1st-level cache retornava Route com lista vazia | Montagem da resposta em memória com dados já carregados |
| `Invalid profile: car` (ORS 400) | Faltava campo `profile` no VroomVehicle | Adicionado `"driving-car"` no payload |
| Porta 8080 em uso | Processo anterior não encerrado corretamente | Kill explícito do processo antes de reiniciar |
| Import não usado no TestController | `@Profile` importado sem uso | Removido, adicionado `@Transactional` |
| Tailwind v4 incompatível com Node v24 | `@tailwindcss/oxide` sem versão válida | Downgrade para Tailwind v3 com postcss |
| react-leaflet peer dep conflict | React 19 + react-leaflet peer deps | Instalação com `--legacy-peer-deps` |

---

## Configuração do Ambiente de Disco

Todo o projeto usa o disco D: para não consumir o C: (reservado para o OS).

| Configuração | Valor |
|---|---|
| npm cache | `D:\npm-cache` |
| npm prefix (globais) | `D:\npm-global` |
| Maven repositório | `D:\.m2\repository` |
| Docker WSL2 data | `D:\docker\DockerDesktopWSL` |
| TEMP / TMP | `D:\tmp` |
| Projeto backend | `D:\pedro\last-mile\` |
| Projeto frontend | `D:\pedro\last-mile\frontend\` |

**Ação pendente (manual):** após fechar o Claude Code e parar o Spring Boot, deletar:
- `C:\Users\pedro\AppData\Roaming\npm` (~640 MB — claude.exe trava enquanto Claude roda)
- `C:\Users\pedro\.m2\repository` (~92 MB — JARs travados pela JVM)

---

## Limitações conhecidas

| Limitação | Motivo |
|---|---|
| `totalDistanceMeters: 0` | ORS optimization free tier não retorna distância no formato VROOM básico |
| Nomes de classes/tabelas em inglês | Refatorar para português exigiria renomear 30+ arquivos e criar nova migration Flyway — risco alto para projeto funcional |

---

## Gap vs Especificação

| Requisito | Status |
|---|---|
| R1: Cadastro de lojas com geocoding | ✅ |
| R2: Token JWT 24h no login | ✅ |
| R3: Proteção com Bearer token | ✅ |
| R4: Isolamento por loja (multitenancy) | ✅ |
| R5: Cadastro de entrega com endereço texto | ✅ |
| R6: Geocoding → Point PostGIS | ✅ |
| R7: Cache Redis antes de chamar ORS | ✅ |
| R8: Buscar entregas PENDING para otimizar | ✅ |
| R9: Montar payload origem + destinos | ✅ |
| R10: Chamar ORS Optimization | ✅ |
| R11: Criar Route, vincular entregas com ordem | ✅ |
| R12: Status PENDING/IN_TRANSIT/DELIVERED/FAILED | ✅ |
| R13: Mudar status para IN_TRANSIT ao gerar rota | ✅ |
| R14: Endpoint para motoboy atualizar status | ✅ |
| Seção 9: Diretriz de disco (tudo no D:) | ✅ Configurado |
| Seção 10: Interface React cyberpunk | 🔄 Em desenvolvimento |
| Seção 10: Mapa Leaflet interativo | ✅ Implementado |
| Testes unitários JUnit + Mockito | ✅ 22 testes — todos passando |

---

## Como rodar

```powershell
# 1. Docker já deve estar rodando (PostgreSQL + Redis)
docker compose up -d

# 2. Subir o backend com chave ORS
$env:ORS_API_KEY = "SUA_CHAVE_ORS"
mvn spring-boot:run

# 3. Subir o frontend (outro terminal)
cd frontend
npm run dev

# 4. Acessar no navegador
# http://localhost:5173
```

## Variáveis de ambiente

| Variável | Obrigatório | Descrição |
|---|---|---|
| `ORS_API_KEY` | ✅ Sim | Chave da API OpenRouteService |
| `JWT_SECRET` | Não (dev) | Segredo JWT (dev usa fallback inseguro) |
| `DB_URL` | Não (dev) | URL do banco (dev usa localhost:5432) |
