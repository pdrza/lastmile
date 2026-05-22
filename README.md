# Last Mile

> **Projeto Acadêmico** — Trabalho de conclusão de curso / projeto de disciplina.
> Não se destina a uso comercial ou produção.

---

## O que é

Last Mile é uma API REST de roteirização de entregas urbanas (*last-mile delivery*). O termo "última milha" se refere ao trecho final da cadeia logística — o percurso do centro de distribuição até o destinatário — que costuma ser o mais caro e complexo de otimizar.

O sistema resolve um problema prático: dada uma lista de endereços de entrega, qual é a ordem mais eficiente de visita para minimizar distância e tempo percorridos? A API automatiza desde a conversão de endereços em coordenadas geográficas até o cálculo da sequência ideal de paradas.

---

## Como funciona

O fluxo principal segue três etapas:

**1. Geocoding**
Quando uma entrega é cadastrada via endereço textual, a API consulta o serviço OpenRouteService para converter o endereço em coordenadas geográficas (latitude/longitude). Essas coordenadas são persistidas no banco usando PostGIS, uma extensão do PostgreSQL para dados espaciais. Resultados de geocoding são cacheados no Redis para evitar chamadas repetidas à API externa para endereços já conhecidos.

**2. Otimização de rota**
Com as coordenadas de todas as entregas pendentes em mãos, a API envia o problema para o motor de otimização VROOM (via OpenRouteService). O VROOM resolve o problema do caixeiro-viajante (TSP) e retorna a sequência de visitas que minimiza a distância total percorrida, junto com a geometria da rota para exibição no mapa.

**3. Execução e rastreamento**
Cada entrega percorre um ciclo de status: `PENDING → IN_TRANSIT → DELIVERED` (ou `FAILED`). A loja acompanha o progresso em tempo real pelo dashboard, que exibe as entregas no mapa com cores distintas por status e a rota traçada sobre o mapa.

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

O backend segue arquitetura em camadas: os Controllers recebem as requisições HTTP, delegam a lógica para os Services, que por sua vez acessam o banco via Repositories (Spring Data JPA). A autenticação é stateless via JWT — cada requisição carrega o token no header, sem necessidade de sessão no servidor.

O sistema suporta múltiplas lojas (multitenancy): cada loja enxerga apenas suas próprias entregas e rotas, isoladas por conta de usuário.

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

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker Desktop
- Node.js 20+
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

O frontend usa um *proxy* do Vite: as chamadas `/api` são encaminhadas para o
backend em `localhost:8080`, então não é preciso configurar CORS em dev.

---

## Instalação como pacote (Docker)

Para rodar o sistema completo — API + frontend + banco + cache — em uma única
máquina (ex.: um teste com um pequeno comércio), sem instalar Java, Node ou
PostgreSQL separadamente.

**Pré-requisito:** apenas o [Docker Desktop](https://www.docker.com/products/docker-desktop).

```powershell
# 1. Copie .env.example para .env e preencha ORS_API_KEY
#    (DB_PASSWORD e JWT_SECRET podem ser quaisquer valores fortes)

# 2. Suba tudo com um comando:
docker compose -f docker-compose.full.yml up -d --build
```

Acesse **http://localhost:8080** — o Spring Boot serve o frontend React e a API
na mesma origem.

Para usuários não técnicos há scripts de duplo clique: **`instalar.bat`**,
**`parar.bat`** e **`desinstalar.bat`**. O passo a passo completo está em
[`MANUAL_DO_LOJISTA.md`](MANUAL_DO_LOJISTA.md).

Diferenças no pacote (perfil `docker`): `JWT_SECRET` é obrigatório (sem fallback
inseguro) e o endpoint auxiliar `DELETE /api/test/reset` fica desativado.

---

## Endpoints

A API expõe quatro grupos de recursos:

- **Autenticação** — cadastro e login de lojas
- **Loja** — dados da loja autenticada *(requer token)*
- **Entregas** — CRUD de entregas com atualização de status *(requer token)*
- **Rotas** — otimização e consulta de rotas calculadas *(requer token)*

Todas as rotas protegidas exigem `Authorization: Bearer <token>` no header.

A documentação interativa (Swagger UI) fica em **`/swagger-ui/index.html`** — lista
todos os endpoints e permite testá-los direto pelo navegador.

---

## Testes

```powershell
mvn test
```

Testes unitários cobrindo camadas de serviço com JUnit 5 + Mockito. Sem chamadas de rede nos testes — dependências externas (geocoding, cache) são mockadas.

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
