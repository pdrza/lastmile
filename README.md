# Last Mile

> **Projeto Acadêmico** — Trabalho de conclusão de curso / projeto de disciplina.
> Não se destina a uso comercial ou produção.

API REST de roteirização *last-mile* desenvolvida em Java com Spring Boot. Recebe endereços de entrega, converte em coordenadas geográficas via geocoding e calcula a rota mais eficiente entre os pontos. Acompanha interface web em React com mapa interativo.

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
│                     React + Vite                            │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP + JWT
┌─────────────────────────▼───────────────────────────────────┐
│                   SPRING BOOT API                           │
│                                                             │
│  Controller → Service → Repository → JPA/Hibernate         │
│                   │           │                             │
│              Redis Cache   PostGIS                          │
└──────┬──────────────────────────────┬───────────────────────┘
       │                              │
┌──────▼──────┐               ┌──────▼──────┐
│  PostgreSQL │               │    Redis    │
│  + PostGIS  │               │   Cache     │
└─────────────┘               └─────────────┘
       │
┌──────▼──────────────────────┐
│   OpenRouteService API      │
│   Geocoding + Otimização    │
└─────────────────────────────┘
```

---

## Funcionalidades

- **Autenticação JWT** — cadastro e login com token stateless
- **Multitenancy** — cada loja acessa apenas seus próprios dados
- **Geocoding automático** — endereço em texto → coordenadas via OpenRouteService
- **Cache Redis** — resultados de geocoding cacheados para evitar chamadas repetidas à API externa
- **Otimização de rotas** — cálculo de rota eficiente via OpenRouteService (VROOM)
- **Persistência geoespacial** — coordenadas armazenadas com PostGIS
- **Interface interativa** — mapa com pins por status e rota traçada

---

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker Desktop
- Node.js 18+
- Chave gratuita da [OpenRouteService API](https://openrouteservice.org/)

---

## Como rodar

### 1. Banco de dados e Redis

```powershell
docker compose up -d
```

Sobe PostgreSQL + PostGIS e Redis. O Flyway cria o schema automaticamente na primeira execução.

### 2. Backend

```powershell
$env:ORS_API_KEY = "sua_chave_ors_aqui"
mvn spring-boot:run
```

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

---

## Endpoints

A API expõe três grupos de recursos:

- **Autenticação** — cadastro e login de lojas
- **Entregas** — CRUD de entregas com atualização de status *(requer token)*
- **Rotas** — otimização e consulta de rotas calculadas *(requer token)*

Todas as rotas protegidas exigem `Authorization: Bearer <token>` no header.

---

## Fluxo de status

```
PENDING → IN_TRANSIT → DELIVERED
                    ↘ FAILED
```

---

## Testes

```powershell
mvn test
```

Testes unitários cobrindo camadas de serviço com JUnit 5 + Mockito. Sem chamadas de rede nos testes (dependências externas são mockadas).

---

## Contexto acadêmico

Projeto desenvolvido para demonstrar integração de tecnologias modernas em problemas logísticos reais:

- Arquitetura REST em camadas (Controller → Service → Repository)
- Segurança stateless com JWT e Spring Security
- Persistência geoespacial com PostGIS
- Estratégia de cache (Cache-aside) com Redis
- Integração com API externa (OpenRouteService / VROOM)
- Testes unitários com isolamento via Mockito
- Interface com React, Tailwind CSS e Leaflet.js
