Especificação Técnica: API de Roteirização Last-Mile (OptiRoute API)

> **Contexto Acadêmico:** Este projeto foi desenvolvido exclusivamente para fins acadêmicos, como trabalho de conclusão de curso / projeto de disciplina. Não se destina à comercialização, uso em produção ou fins lucrativos.

1. Visão Geral do Sistema

O sistema é uma API RESTful projetada para demonstrar, em contexto acadêmico, a solução de problemas logísticos de "Última Milha" (Last-Mile Delivery). O objetivo é receber uma lista de endereços de entrega de uma loja simulada, transformar esses endereços em coordenadas geográficas e calcular a rota mais eficiente (problema do caixeiro viajante).

O sistema é multitenant, garantindo que cada loja gerencie apenas as suas próprias entregas de forma isolada e segura.
2. Stack Tecnológico e Ambiente

    Linguagem: Java 21 (Eclipse Temurin)

    Framework: Spring Boot 3

    Gerenciador de Dependências: Maven

    Banco de Dados: PostgreSQL 15 com extensão PostGIS 3.3 (para cálculos espaciais)

    Cache e Alta Performance: Redis

    Segurança: Spring Security com JWT (JSON Web Tokens)

    Integração Externa: OpenRouteService API (via Spring RestClient)

    Testes Automatizados: JUnit 5 + Mockito

    Infraestrutura Local: Docker e Docker Compose

    Ambiente de Desenvolvimento: Windows 10, VS Code, Claude Code CLI.

    Disco de Trabalho: D:\ — todo o projeto, dependências, caches e instalações de ferramentas devem ser configurados para usar o disco D:. O disco C: é reservado exclusivamente para o sistema operacional.

    Finalidade: Projeto acadêmico — sem fins comerciais ou de produção.

3. Regras de Negócio e Funcionalidades Core
3.1. Autenticação e Isolamento (Multitenancy)

    R1: Lojas devem se cadastrar na plataforma fornecendo nome, e-mail, senha e endereço físico (origem das rotas).

    R2: Ao fazer login, a loja recebe um Token JWT com validade estipulada (ex: 24 horas).

    R3: Todas as rotas de negócio (entregas e rotas) devem exigir o envio deste Token no cabeçalho Authorization: Bearer <token>.

    R4: O sistema deve extrair o ID da loja logada diretamente do Token JWT para garantir que uma loja (ex: Floricultura A) jamais veja, edite ou exclua entregas de outra loja (ex: Padaria B).

3.2. Gestão de Entregas e Geocoding

    R5: Quando a loja cadastra uma nova entrega, ela envia apenas os dados do cliente e o endereço em texto (ex: "Rua XV de Novembro, 1000, Curitiba, PR").

    R6: O sistema deve consumir uma API externa de Geocoding para transformar esse endereço em Latitude e Longitude, salvando esses dados como um tipo espacial Point no PostGIS.

    R7 (Estratégia de Cache): Para economizar a cota de requisições da API externa, antes de fazer a busca, o sistema deve verificar no Redis se aquele endereço exato já foi geocodificado. Se sim, retorna do cache. Se não, busca na API externa e salva no cache (Redis) e no banco.

3.3. Motor de Otimização de Rotas

    R8: A loja aciona o endpoint de otimização de rotas. O sistema deve buscar no banco de dados todas as entregas daquela loja que estejam com o status PENDENTE.

    R9: O sistema monta um payload contendo a coordenada de origem (endereço da Loja) e as coordenadas de destino (Entregas).

    R10: O sistema envia essa matriz para a API de Otimização (OpenRouteService).

    R11: A API devolve a sequência ideal (ex: [Origem, Entrega 3, Entrega 1, Entrega 2]). O sistema cria um registro de Route no banco, vincula as entregas a essa rota atualizando a ordem de entrega (índice) e salva a distância e tempo totais estimados.

3.4. Fluxo de Status

    R12: Uma entrega possui os seguintes status permitidos: PENDENTE, EM_ROTA, ENTREGUE, FALHA.

    R13: Ao gerar uma rota otimizada, o status das entregas envolvidas muda automaticamente para EM_ROTA.

    R14: O sistema deve possuir um endpoint para o motoboy atualizar o status de uma entrega específica (ex: de EM_ROTA para ENTREGUE).

4. Modelagem de Dados (Entidades Principais)
4.1. Tabela stores (Lojas)

    id (UUID, Primary Key)

    name (Varchar)

    email (Varchar, Unique)

    password (Varchar, Bcrypt Hash)

    address_text (Varchar)

    location (Geometry/Point PostGIS) - Coordenada da loja.

    created_at (Timestamp)

4.2. Tabela routes (Rotas Calculadas)

    id (UUID, Primary Key)

    store_id (UUID, Foreign Key)

    total_distance_meters (Integer)

    total_time_seconds (Integer)

    status (Enum: ACTIVE, COMPLETED)

    created_at (Timestamp)

4.3. Tabela deliveries (Entregas/Pacotes)

    id (UUID, Primary Key)

    store_id (UUID, Foreign Key)

    route_id (UUID, Foreign Key, Nullable)

    customer_name (Varchar)

    address_text (Varchar)

    location (Geometry/Point PostGIS) - Coordenada de destino.

    delivery_order (Integer, Nullable) - A ordem de parada após o cálculo da rota.

    status (Enum: PENDING, IN_TRANSIT, DELIVERED, FAILED)

    created_at (Timestamp)

5. Especificação da API (Endpoints)
Auth

    POST /api/auth/register

        Body: name, email, password, address.

        Ação: Cria a loja, faz hash da senha, geocodifica o endereço da loja.

    POST /api/auth/login

        Body: email, password.

        Response: { "token": "eyJhbGciOiJIUzI1..." }

Deliveries (Requer JWT)

    POST /api/deliveries

        Body: { "customer_name": "João", "address": "Rua X..." }

        Ação: Associa à loja do token, checa o cache no Redis, busca Lat/Long, salva como PENDENTE.

    GET /api/deliveries

        Query Params (Opcionais): ?status=PENDING

        Response: Lista das entregas da loja logada.

    PATCH /api/deliveries/{id}/status

        Body: { "status": "DELIVERED" }

        Ação: Atualiza o status do pacote.

Routes (Requer JWT)

    POST /api/routes/optimize

        Ação: Coleta as entregas PENDING da loja, consome a API de rotas, cria o objeto Route, muda os pacotes para IN_TRANSIT com a ordem correta e retorna a rota estruturada.

    GET /api/routes/{id}

        Ação: Retorna os detalhes de uma rota específica contendo o array de entregas ordenadas.

6. Tratamento de Erros e Exceções (Global Exception Handler)

O sistema implementará um @ControllerAdvice para capturar exceções e retornar JSONs padronizados (Padrão RFC 7807 - Problem Details):

    400 Bad Request: Dados inválidos no cadastro (ex: e-mail mal formatado).

    401 Unauthorized: Token inválido ou ausente.

    403 Forbidden: Loja tentando acessar a entrega de outra loja.

    404 Not Found: Recurso não existe.

    502 Bad Gateway: Falha ao comunicar com a API externa (OpenRouteService).

    504 Gateway Timeout: API externa demorando para responder.

7. Estratégia de Testes

    Cobertura: Testes focados nas regras de negócio principais usando JUnit 5.

    Isolamento: Uso de Mockito para mockar a camada de repositório e, principalmente, a comunicação com a API Externa de Geocoding (para não fazer chamadas de rede durante os testes) e a camada do Redis.

    Alvos Principais: Serviços de otimização de rota, checagem de propriedade de dados (garantir que um usuário não altera dados de outro) e serviços de cache.

8. Diretriz de Disco e Ambiente

DIRETRIZ DE DISCO: O disco C: é exclusivo para o sistema operacional. Todo o restante deve ser instalado e configurado no disco D:. Isso inclui obrigatoriamente:

- Código-fonte do projeto: D:\pedro\last-mile\ (backend) e D:\pedro\last-mile\frontend\ (React)
- Cache do npm: D:\npm-cache
- Pacotes globais npm: D:\npm-global
- Repositório local Maven (JARs): D:\.m2\repository
- Dados do Docker Desktop (imagens, volumes, WSL2): D:\docker\
- Arquivos temporários do sistema: D:\tmp
- Qualquer nova ferramenta, SDK ou biblioteca instalada para o projeto deve ser configurada para usar o disco D:

Configurações já realizadas no ambiente de desenvolvimento:
- npm config: cache=D:\npm-cache, prefix=D:\npm-global
- Maven settings.xml: localRepository=D:\.m2\repository (em C:\Users\pedro\.m2\settings.xml)
- Variáveis de ambiente: TEMP=D:\tmp, TMP=D:\tmp, MAVEN_USER_HOME=D:\.m2
- Docker Desktop: CustomWslDistroDir=D:\docker\DockerDesktopWSL

9. Diretriz de Idioma

DIRETRIZ DE IDIOMA: Todo o código gerado deve ser escrito estritamente em Português. Isso inclui obrigatoriamente: nomes de variáveis, funções, classes, nomes de tabelas e colunas do banco de dados, além de todos os comentários e documentações. As únicas exceções permitidas são as palavras reservadas da própria linguagem de programação e chamadas de bibliotecas/APIs externas que exijam o inglês.

---

10. Interface Frontend — OptiRoute Dashboard

10.1. Conceito Visual

A interface adota uma estética cyberpunk/retro-tecnológica inspirada na identidade visual do PlayStation 2: fundo escuro profundo, elementos com brilho neon, linhas de grade pixeladas e tipografia monoespaçada. O resultado é um painel de controle que parece pertencer a um sistema de despacho logístico do futuro — tecnológico, denso de informação e visualmente impactante.

Referências visuais:
- Fundo escuro com leve textura de grade (grid de pixels finos)
- Elementos com glow/blur suave em roxo neon e magenta
- Bordas com efeito "scanline" ou "glitch" em hover
- Cards e painéis com borda sólida fina neon, sem arredondamento excessivo
- Tipografia: fonte monoespaçada (JetBrains Mono ou similar) para dados numéricos; sans-serif clean para textos

10.2. Paleta de Cores

| Token          | Hex       | Uso                                      |
|----------------|-----------|------------------------------------------|
| `--bg-deep`    | `#0d0010` | Fundo principal da aplicação             |
| `--bg-surface` | `#160020` | Cards, painéis, modais                   |
| `--bg-border`  | `#2a004a` | Bordas de componentes                    |
| `--primary`    | `#bf00ff` | Botões principais, títulos, ícones ativos|
| `--accent`     | `#ff006e` | Status crítico, alertas, badges          |
| `--text-main`  | `#e8d0ff` | Texto principal                          |
| `--text-muted` | `#7a5a99` | Labels, placeholders, texto secundário   |
| `--success`    | `#00ffaa` | Status DELIVERED, confirmações           |
| `--warning`    | `#ffaa00` | Status IN_TRANSIT                        |

10.3. Stack Tecnológica Frontend

| Ferramenta       | Versão  | Função                                                         |
|------------------|---------|----------------------------------------------------------------|
| React            | 18+     | Biblioteca de interface — componentes reutilizáveis            |
| Vite             | 5+      | Bundler e servidor de desenvolvimento (substitui Create React App) |
| React Router     | 6+      | Navegação entre páginas (login, dashboard, detalhes de rota)   |
| Axios            | 1+      | Requisições HTTP para a API — mais simples que fetch nativo    |
| Tailwind CSS     | 3+      | Estilização utilitária — classes no HTML, sem CSS separado     |
| Leaflet.js       | 1+      | Mapa interativo gratuito (OpenStreetMap) — sem custo de API    |
| React-Leaflet    | 4+      | Wrapper React para o Leaflet                                   |

Estrutura de diretórios do frontend:
```
frontend/
├── src/
│   ├── pages/
│   │   ├── Login.jsx          — tela de login e cadastro de loja
│   │   └── Dashboard.jsx      — painel principal (entregas + rota)
│   ├── components/
│   │   ├── DeliveryCard.jsx   — card de uma entrega com status colorido
│   │   ├── RouteMap.jsx       — mapa Leaflet com pins e rota traçada
│   │   ├── NewDeliveryForm.jsx — formulário de cadastro de entrega
│   │   └── StatusBadge.jsx    — badge colorido por status (PENDING/IN_TRANSIT/etc)
│   ├── services/
│   │   └── api.js             — instância Axios configurada com baseURL e token JWT
│   └── main.jsx               — entry point
├── index.html
└── vite.config.js
```

10.4. Telas e Fluxo de Navegação

Tela 1 — Login / Cadastro (/login)
- Dois modos alternáveis: "Entrar" e "Cadastrar"
- Campos: e-mail, senha (+ nome e endereço no modo cadastro)
- Ao logar: salva token JWT no localStorage e redireciona para /dashboard
- Estética: formulário centralizado, fundo com grade animada sutil, logo "OptiRoute" em roxo neon

Tela 2 — Dashboard (/dashboard)
Layout dividido em duas colunas:
- Coluna esquerda (40%): lista de entregas com filtro por status, botão "+ Nova Entrega", botão "Otimizar Rota"
- Coluna direita (60%): mapa Leaflet em tema escuro com pins coloridos por status

Ao clicar em "Otimizar Rota":
- Chama POST /api/routes/optimize
- Atualiza o mapa com a rota traçada (linha conectando os pontos na ordem otimizada)
- Exibe painel inferior com distância total, tempo estimado e a sequência numerada de paradas

10.5. Mapa Interativo (Leaflet)

- Tile layer: CartoDB Dark Matter (tema escuro gratuito, compatível com a paleta cyberpunk)
- Marcador da loja (origem): ícone roxo/neon, tooltip "Ponto de partida"
- Marcadores de entrega: ícone colorido por status
  - PENDING → roxo (#bf00ff)
  - IN_TRANSIT → laranja (#ffaa00)
  - DELIVERED → verde (#00ffaa)
  - FAILED → vermelho (#ff006e)
- Rota otimizada: polyline magenta conectando os pontos na ordem retornada pela API
- Popup em cada marcador: nome do cliente, endereço, status, ordem de entrega

10.6. Comunicação com a API

O frontend se comunica exclusivamente com a API Spring Boot (porta 8080).
O token JWT obtido no login é enviado em todas as requisições protegidas:

```
Authorization: Bearer <token>
```

Configuração do Axios (services/api.js):
- baseURL: http://localhost:8080
- Interceptor automático: adiciona o header Authorization em toda requisição
- Interceptor de resposta: se receber 401, redireciona para /login e limpa o localStorage

10.7. Regras de UX

- Status das entregas exibido sempre com badge colorido, nunca só texto
- Formulário de nova entrega: campo de endereço é texto livre (a API faz o geocoding)
- Botão "Otimizar Rota" desabilitado se não houver entregas com status PENDING
- Após otimizar: mapa centraliza automaticamente nos pontos da rota
- Mensagens de erro da API (4xx/5xx) exibidas em toast notification no canto superior direito