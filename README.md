# ms-chatbot

Microserviço responsável pelo chatbot WhatsApp da plataforma Dias. Faz parte do **Plano Gold** e processa mensagens recebidas via [Evolution API](https://github.com/EvolutionAPI/evolution-api), executando fluxos de conversa configuráveis por empresa (multi-tenant).

## Stack

- Java 21 + Spring Boot 3.4.5
- Spring Data JPA + MySQL 8
- Evolution API (canais Baileys e WhatsApp Business API)

## Como rodar

### Pré-requisitos

- Java 21
- MySQL 8 rodando na porta `3306`
- Evolution API acessível

### Configuração

Copie o arquivo de exemplo e preencha as variáveis:

```bash
cp .env.example .env
```

| Variável | Descrição |
|---|---|
| `DB_USERNAME` | Usuário do banco MySQL |
| `DB_PASSWORD` | Senha do banco MySQL |
| `EVOLUTION_BASE_URL` | URL base da Evolution API (ex: `http://localhost:8080`) |
| `EVOLUTION_GLOBAL_API_KEY` | Global API Key da instância Evolution |

O banco `chatbot_db` é criado automaticamente na primeira execução.

### Executar

```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta **8085**.

## API

### Empresas

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/companies` | Cadastra uma empresa/tenant |
| `POST` | `/api/companies/{id}/steps` | Adiciona um passo ao fluxo da empresa |
| `GET` | `/api/companies/{id}/steps` | Lista os passos do fluxo da empresa |

### Webhooks

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/webhook/{instanceName}` | Recebe eventos do canal Baileys |
| `POST` | `/webhook/business/{instanceName}` | Recebe eventos do WhatsApp Business API |

Configure a URL de webhook na Evolution API apontando para um desses endpoints conforme o tipo de canal da empresa.

## Fluxo de mensagens

```
Evolution API → Webhook → Normalização do payload → ProcessMessageUseCase
    → FlowEngineService → resolve próximo passo → envia resposta via Evolution API
```

Cada empresa possui um conjunto de `FlowStep`s cadastrados via API. O motor de fluxo (`FlowEngineService`) avança a sessão do usuário entre os passos conforme as transições configuradas.

### Tipos de passo (`StepType`)

| Tipo | Comportamento |
|---|---|
| `MENU` | Exibe opções; avança conforme o trigger digitado |
| `INPUT` | Coleta e valida uma entrada livre do usuário |
| `ACTION` | Executa uma ação e avança automaticamente |
| `END` | Finaliza a sessão |

### Tipos de canal (`ChannelType`)

| Tipo | Gateway |
|---|---|
| `BAILEYS` | Instância Baileys via Evolution API |
| `BUSINESS` | WhatsApp Business API via Evolution API |

## Testes

```bash
./mvnw test
```

Os testes usam banco H2 em memória — nenhuma dependência externa necessária.