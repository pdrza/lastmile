# OptiRoute API

> **Projeto Acadêmico** — Trabalho de conclusão de curso / projeto de disciplina.
> Não se destina a uso comercial ou produção.

Sistema de roteirização *last-mile* (última milha) desenvolvido em Java com Spring Boot. Recebe uma lista de endereços de entrega de uma loja, transforma os endereços em coordenadas geográficas e calcula a rota mais eficiente usando a API OpenRouteService. Possui interface web em React com mapa interativo e estética cyberpunk.

---

## Stack

**Backend**

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_15_PostGIS-4169E1?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=flat&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

**Frontend**

![React](https://img.shields.io/badge/React_19-20232A?style=flat&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite_8-646CFF?style=flat&logo=vite&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS_3-06B6D4?style=flat&logo=tailwindcss&logoColor=white)
![Leaflet](https://img.shields.io/badge/Leaflet-199900?style=flat&logo=leaflet&logoColor=white)

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENTE (Browser)                        │
│              React + Vite  localhost:5173                   │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP + JWT
┌─────────────────────────▼───────────────────────────────────┐
│                   SPRING BOOT API                           │
│                    localhost:8080                           │
│                                                             │
│  Controller → Service → Repository → JPA/Hibernate         │
│                   │           │                             │
│              Redis Cache   PostGIS                          │
└──────┬──────────────────────────────┬───────────────────────┘
       │                              │
┌──────▼──────┐               ┌──────▼──────┐
│  PostgreSQL │               │    Redis    │
│  + PostGIS  │               │   Cache     │
│  porta 5432 │               │  porta 6379 │
└─────────────┘               └─────────────┘
       │
┌──────▼──────────────────────┐
│   OpenRouteService API      │
│   Geocoding + Otimização    │
│   api.openrouteservice.org  │
└─────────────────────────────┘
```

---

## Funcionalidades

- **Autenticação JWT** — cadastro e login de lojas com token de 24h
- **Multitenancy** — cada loja acessa apenas as próprias entregas
- **Geocoding automático** — endereço em texto → coordenadas (lat/lng) via ORS
- **Cache Redis** — endereços já pesquisados não chamam a API novamente (TTL 30 dias)
- **Otimização de rotas** — algoritmo de roteamento via OpenRouteService (formato VROOM)
- **Persistência geoespacial** — coordenadas salvas como `GEOMETRY(Point, 4326)` no PostGIS
- **Interface cyberpunk** — dashboard com mapa interativo, pins coloridos por status e rota traçada

---

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker Desktop (para PostgreSQL + Redis)
- Node.js 18+
- Chave gratuita da [OpenRouteService API](https://openrouteservice.org/)

---

## Como rodar

### 1. Banco de dados e Redis

```powershell
docker compose up -d
```

Isso sobe:
- PostgreSQL 15 + PostGIS na porta `5432`
- Redis 7 na porta `6379`

O Flyway cria o schema automaticamente na primeira execução.

### 2. Backend

```powershell
$env:ORS_API_KEY = "sua_chave_ors_aqui"
mvn spring-boot:run
```

API disponível em `http://localhost:8080`

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Interface disponível em `http://localhost:5173`

---

## Endpoints da API

### Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/auth/register` | Cadastra nova loja (geocodifica o endereço) |
| `POST` | `/api/auth/login` | Login — retorna token JWT |

### Entregas *(requer Bearer token)*

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/deliveries` | Cadastra entrega (geocodifica endereço do cliente) |
| `GET` | `/api/deliveries` | Lista entregas da loja (`?status=PENDING`) |
| `PATCH` | `/api/deliveries/{id}/status` | Atualiza status de uma entrega |

### Rotas *(requer Bearer token)*

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/routes/optimize` | Calcula rota otimizada para entregas PENDING |
| `GET` | `/api/routes/{id}` | Busca detalhes de uma rota |

### Utilitários *(testes)*

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `DELETE` | `/api/test/reset` | Reseta entregas para PENDING e apaga rotas |

---

## Fluxo de status das entregas

```
PENDING → IN_TRANSIT → DELIVERED
                    ↘ FAILED
```

- `PENDING` — entrega cadastrada, aguardando otimização
- `IN_TRANSIT` — rota gerada, motoboy a caminho
- `DELIVERED` — entrega concluída
- `FAILED` — entrega falhou

---

## Variáveis de ambiente

| Variável | Obrigatório | Padrão (dev) | Descrição |
|----------|-------------|--------------|-----------|
| `ORS_API_KEY` | ✅ | — | Chave da OpenRouteService |
| `JWT_SECRET` | Não | fallback inseguro | Segredo HMAC-SHA256 para JWT |
| `DB_URL` | Não | `localhost:5432/optiroute` | URL do PostgreSQL |
| `DB_USERNAME` | Não | `optiroute` | Usuário do banco |
| `DB_PASSWORD` | Não | `optiroute123` | Senha do banco |
| `REDIS_HOST` | Não | `localhost` | Host do Redis |

---

## Testes

```powershell
mvn test
```

22 testes unitários cobrindo as camadas de serviço com JUnit 5 + Mockito.
Nenhum teste faz chamadas de rede (GeocodingClient e Redis são mockados).

---

## Estrutura do projeto

```
last-mile/
├── src/
│   ├── main/java/com/lastmile/optiroute/
│   │   ├── client/         # Integração com OpenRouteService
│   │   ├── controller/     # Endpoints REST
│   │   ├── domain/         # Entidades JPA e enums
│   │   ├── dto/            # Objetos de transferência de dados
│   │   ├── exception/      # Tratamento global de erros (RFC 7807)
│   │   ├── repository/     # Interfaces Spring Data JPA
│   │   ├── security/       # JWT, filtro de autenticação, Spring Security
│   │   └── service/        # Regras de negócio
│   └── main/resources/
│       ├── application.yml
│       └── db/migration/   # Scripts Flyway
├── frontend/
│   └── src/
│       ├── components/     # StatusBadge, DeliveryCard, RouteMap, NewDeliveryForm
│       ├── pages/          # Login, Dashboard
│       └── services/       # Axios configurado com JWT
├── docker-compose.yml
└── pom.xml
```

---

## Contexto acadêmico

Projeto desenvolvido para demonstrar, em ambiente acadêmico, a integração de tecnologias modernas na solução de problemas logísticos reais:

- Arquitetura REST com separação de camadas (Controller → Service → Repository)
- Segurança stateless com JWT e Spring Security
- Persistência geoespacial com PostGIS
- Estratégia de cache (Cache-aside) com Redis
- Integração com API externa (OpenRouteService / VROOM)
- Testes unitários com isolamento via Mockito
- Interface moderna com React, Tailwind CSS e Leaflet.js
