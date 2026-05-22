# Manual do Lojista — Sistema de Entregas Last Mile

Guia simples, sem termos técnicos, para instalar e usar o sistema no seu computador.

O Last Mile organiza suas entregas: você cadastra os endereços e o sistema calcula
a **melhor ordem** para o entregador fazer o trajeto gastando menos tempo e combustível.

---

## O que você precisa

- Um computador com **Windows 10 ou 11**
- **Conexão com a internet** (o sistema usa mapas online)
- Cerca de **15 minutos** para a primeira instalação

---

## Passo 1 — Instalar o Docker Desktop (só uma vez)

O sistema roda dentro de um programa chamado **Docker Desktop**. Ele é gratuito.

1. Acesse: https://www.docker.com/products/docker-desktop
2. Clique em **Download for Windows** e instale (avance com as opções padrão).
3. Reinicie o computador se for solicitado.
4. Abra o **Docker Desktop** e aguarde o ícone da baleia ficar **verde / "Running"**.

> Você só faz este passo uma vez. Nas próximas vezes, o Docker já estará pronto.

---

## Passo 2 — Iniciar o sistema

1. Abra a pasta do sistema que você recebeu.
2. Dê **duplo clique** no arquivo **`instalar.bat`**.
3. Uma janela preta vai abrir. **Na primeira vez demora alguns minutos** — é normal,
   ele está preparando tudo. Não feche a janela.
4. Quando terminar, o navegador abre sozinho em **http://localhost:8080**.

Pronto — o sistema está no ar.

> Se o navegador não abrir sozinho, abra o Chrome/Edge e digite: `localhost:8080`

---

## Passo 3 — Usar o sistema

### Criar a conta da sua loja
1. Na tela inicial, clique em **Cadastrar**.
2. Preencha: nome da loja, e-mail, senha e o **endereço da loja** (o ponto de partida
   das entregas).
3. Clique em cadastrar — você já entra direto no painel.

### Cadastrar entregas
1. No painel, use o formulário de **nova entrega**.
2. Informe o **nome do cliente** e o **endereço de entrega**.
3. Repita para cada entrega do dia.

### Calcular a melhor rota
1. Com as entregas cadastradas, clique em **Otimizar rota**.
2. O sistema calcula a ordem ideal e desenha o trajeto no mapa.
3. As entregas aparecem numeradas na sequência que o entregador deve seguir.

### Acompanhar as entregas
Cada entrega tem um status com cor própria: pendente, em trânsito, entregue ou
falhou. Atualize o status conforme o dia avança.

---

## Parar e religar o sistema

- **Para parar:** dê duplo clique em **`parar.bat`**. Seus dados ficam salvos.
- **Para religar:** dê duplo clique em **`instalar.bat`** de novo (das próximas vezes
  é rápido).

O sistema também volta sozinho quando você liga o computador, desde que o Docker
Desktop esteja aberto.

---

## Desinstalar

Se quiser remover tudo, dê duplo clique em **`desinstalar.bat`** e confirme.

> **Atenção:** isso apaga **todas as lojas, entregas e rotas** cadastradas. Não tem volta.

---

## Problemas comuns

| Problema | O que fazer |
|---|---|
| "Docker Desktop não encontrado" | Faça o Passo 1 (instalar o Docker Desktop). |
| "Docker Desktop não está rodando" | Abra o Docker Desktop e espere o ícone ficar verde. |
| O navegador não abriu | Abra o navegador e digite `localhost:8080` manualmente. |
| Página não carrega na 1ª vez | Aguarde 1-2 minutos e atualize (F5) — o sistema ainda está iniciando. |
| Erro ao cadastrar endereço | Verifique a conexão com a internet e se o endereço está completo (rua, número, cidade). |
| "Porta 8080 em uso" | Outro programa está usando a porta. Feche-o ou reinicie o computador. |

Em caso de dúvida, entre em contato com o responsável que entregou o sistema.
