# TODO — ms-chatbot

## Fase atual: integração com ms-erp-api + painel de atendimento

---

## 1. Persistência de mensagens (base de tudo) ✅

Hoje o chatbot só persiste `Session` (step atual + dados coletados). Sem histórico de mensagens não é possível montar o chat no frontend nem auditar conversas.

- [x] Criar entidade `Message` (MySQL)
  - `id`, `sessionId` (FK), `direction` (INBOUND/OUTBOUND), `text`, `createdAt`
- [x] Gravar mensagem INBOUND ao receber webhook (`ProcessMessageUseCase`)
- [x] Gravar mensagem OUTBOUND ao enviar via `MessagingGateway`
- [x] `GET /api/sessions/{id}/messages` — histórico paginado de uma sessão
- [x] `GET /api/sessions` — lista de sessões ativas com última mensagem e status

---

## 2. Handoff — ativar / desativar pessoa do fluxo ✅

Permite que o atendente assuma a conversa e o bot pare de processar aquela sessão.

- [x] Adicionar `HANDOFF` ao enum `SessionStatus` (NEW / ACTIVE / COMPLETED / **HANDOFF**)
- [x] `ProcessMessageUseCase`: se `status == HANDOFF`, apenas gravar mensagem (não processar flow)
- [x] `PATCH /api/sessions/{id}/handoff` — atendente assume
- [x] `PATCH /api/sessions/{id}/reativar` — devolve ao bot (volta para ACTIVE no step atual)
- [x] Notificar via WebSocket quando mensagem chegar em sessão HANDOFF

---

## 3. Responder via frontend (atendente manual) ✅

- [x] `POST /api/sessions/{id}/mensagem` — `{ "texto": "..." }`
  - Chama `MessagingGateway.sendText` para enviar via Evolution API
  - Grava como `direction=OUTBOUND` no histórico
  - Retorna 409 se sessão não estiver em HANDOFF, 404 se não existir

---

## 4. WebSocket — chat em tempo real ✅

Sem isso o frontend precisa fazer polling, o que não escala.

- [x] Adicionar dependência `spring-boot-starter-websocket`
- [x] Configurar STOMP broker (`/topic/sessions/{sessionId}`) com SockJS
- [x] Publicar evento a cada mensagem nova (INBOUND e OUTBOUND) no tópico da sessão
- [x] Publicar no tópico global `/topic/inbox` quando nova sessão iniciar ou status mudar
- [x] Autenticação no WebSocket (verificar `X-Internal-Key` no handshake STOMP via `StompAuthChannelInterceptor`)

---

## 5. Conectar WhatsApp via frontend ✅

O atendente/admin precisa conectar a instância sem acessar a Evolution API diretamente.

- [x] `GET  /api/whatsapp/status?companyId=` — retorna estado da conexão (`open` / `connecting` / `close`)
- [x] `POST /api/whatsapp/connect?companyId=` — inicia conexão na Evolution API (retorna QR ou `connected: true`)
- [x] `GET  /api/whatsapp/qrcode?companyId=` — re-busca QR em base64 para exibir no frontend
- [x] `POST /api/whatsapp/disconnect?companyId=` — desconecta instância (logout)
- [x] Autenticação via header `X-Internal-Key` (configurável por `INTERNAL_API_KEY`)

---

## 6. Integração com ms-erp-api (agendamento via WhatsApp) ✅

### 6.1 Vínculo Company ↔ Empresa

- [x] Adicionar coluna `erp_empresa_id` na entidade `Company`
- [x] Adicionar campo `erpEmpresaId` no `CompanyRequest` DTO
- [x] Atualizar `CompanyController.create` para persistir o campo

### 6.2 ActionType no FlowStep

- [x] Criar enum `ActionType`: `LISTAR_SERVICOS`, `BUSCAR_OU_CRIAR_CLIENTE`, `VERIFICAR_DISPONIBILIDADE`, `CRIAR_AGENDAMENTO`
- [x] Adicionar coluna `action_type` (`VARCHAR(50)`, nullable) na entidade `FlowStep`
- [x] Adicionar campo `actionType` no `FlowStepRequest` DTO

### 6.3 ErpClient

- [x] Criar `ErpProperties` (`@ConfigurationProperties(prefix = "erp")`)
- [x] Criar `ErpClient` usando `RestClient` com autenticação `X-Internal-Key`
  - `listarServicos`, `buscarOuCriarCliente`, `listarDisponibilidade`, `criarAgendamento`
- [x] Adicionar `ERP_BASE_URL` e `ERP_INTERNAL_API_KEY` ao `.env.example`

### 6.4 FlowEngineService — executar ACTION

- [x] Criar `ErpActionExecutor` (service isolado, retorna boolean sucesso/falha)
- [x] `LISTAR_SERVICOS`: monta menu numerado → salva `servicos_menu` + `servico_id_{n}` na session
- [x] `BUSCAR_OU_CRIAR_CLIENTE`: salva `cliente_id` na session
- [x] `VERIFICAR_DISPONIBILIDADE`: monta lista de slots → salva `slots_menu` + `slot_horario_{n}`
- [x] `CRIAR_AGENDAMENTO`: salva `confirmacao` e `agendamento_id` na session
- [x] Fallback de erro: envia mensagem de falha, reverte session ao step anterior (usuário pode tentar novamente)
- [x] Menu dinâmico (sem transições + `sessionDataKey`): aceita qualquer número positivo → `defaultNextStepKey`
- [x] MENU step com `sessionDataKey`: armazena a opção escolhida pelo usuário automaticamente

---

## 7. Flow builder via frontend ✅

O frontend (ms-erp-app) precisará de um editor de fluxos. O backend do chatbot já tem criação de steps; falta o CRUD completo e autenticação nas rotas.

- [x] Adicionar autenticação nas rotas `/api/companies/**` (hoje abertas)
  - Sugestão: `X-Internal-Key` para chamadas do ERP (o front passa pelo ERP)
- [x] `PUT  /api/companies/{id}/steps/{stepId}` — editar step
- [x] `DELETE /api/companies/{id}/steps/{stepId}` — remover step
- [x] `PUT  /api/companies/{id}` — editar dados da Company (nome, welcomeStepKey, ativo)
- [x] `GET  /api/companies/{id}` — buscar Company por ID
- [x] Validar que step `defaultNextStepKey` referencia um step existente (evitar flow quebrado)

---

## 8. Informações da empresa no fluxo ✅

Serviços e preços vêm do ERP via `LISTAR_SERVICOS`. Outros dados precisam ser configurados.

- [x] Adicionar campos à `Company`: `endereco`, `horarioFuncionamento`, `telefoneContato`
  - Colunas individuais no banco (`TEXT` para endereco/horario, `VARCHAR` para telefone)
- [x] Expor via `GET /api/companies/{id}` para o flow builder usar nos templates
- [x] Templates de step podem usar `{{endereco}}`, `{{horario}}`, `{{telefone_contato}}`, `{{nome_empresa}}` — o ACTION `CARREGAR_DADOS_EMPRESA` (novo no `ActionType`) popula esses valores na session sem necessidade de `erpEmpresaId`

---

## Dependências entre itens

```
[1] Mensagens  →  [4] WebSocket
[1] Mensagens  →  [2] Handoff
[2] Handoff    →  [3] Responder via frontend
[6.1] + [6.2]  →  [6.3] ErpClient  →  [6.4] FlowEngineService
[7] Flow builder  →  depende de auth nas rotas
```

## Ordem de implementação sugerida

1. `[1]` Modelo Message + histórico — habilita o resto
2. `[2]` Handoff + `[3]` Responder — entrega valor imediato ao atendente
3. `[6]` Integração ERP — agendamento via WhatsApp
4. `[4]` WebSocket — tempo real
5. `[5]` Conectar WhatsApp via frontend
6. `[7]` Flow builder
7. `[8]` Dados da empresa no fluxo
