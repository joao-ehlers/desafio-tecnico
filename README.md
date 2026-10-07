# Big Chat Brasil (BCB) — Desafio Backend

API para comunicação entre empresas clientes e destinatários finais, com fila em
memória, priorização, processamento assíncrono simulado e cobrança por mensagem.

O projeto evoluiu de FIFO síncrono para duas filas com balanceamento de prioridades
e um worker em background. A versão atual inclui retry limitado e histórico
persistido no PostgreSQL. O envio não utiliza um provedor real de SMS ou WhatsApp.

## Tecnologias

- Java e Spring Boot — versões definidas no `pom.xml`.
- Spring Web MVC, Spring Data JPA e Bean Validation.
- PostgreSQL e Flyway.
- Lombok, Maven e Docker Compose.
- JUnit Jupiter, Mockito e AssertJ.
- Testcontainers para testes de integração com PostgreSQL.

## Execução

Para executar com containers, é necessário Docker com suporte a Linux containers
e Docker Compose. Para executar Maven fora do container, utilize o JDK configurado
no `pom.xml`.

Na raiz do repositório, copie `.env.example` para `.env`:

```bash
cp .env.example .env
```

No PowerShell:

```powershell
Copy-Item .env.example .env
```

Preencha as variáveis exigidas pelo Compose, incluindo a senha do PostgreSQL.
O arquivo `.env` não deve ser versionado. Em seguida:

```bash
docker compose up --build
```

Endereço utilizado no desenvolvimento: `http://localhost:8086`. Confira o
mapeamento de portas no Compose caso tenha sido alterado.

```bash
docker compose logs -f
docker compose down
```

O Flyway aplica as migrations durante a inicialização. Novas mudanças de schema
são feitas em novas migrations; arquivos já aplicados não devem ser editados.

## Identificação das requisições

`POST /auth` identifica um cliente ativo pelo CPF/CNPJ. Não emite JWT nem cria sessão.

Os endpoints de mensagens e conversas recebem o documento em cada requisição:

```http
X-Client-Document: 52998224725
```

O backend normaliza e valida o documento, localiza o cliente e verifica se está
ativo. O ID interno desse cliente é utilizado nas consultas por proprietário.
O corpo de envio não recebe `clientId` nem permite escolher o status inicial.

Essa identificação é uma simplificação do desafio: o documento não é um segredo
nem comprova identidade. Quem conhece o documento pode se identificar como o cliente.
Não há um sistema completo de permissões administrativas implementado.

## Endpoints

| Método | Rota | Finalidade |
|---|---|---|
| POST | `/auth` | Identificar cliente por documento |
| GET | `/clients` | Listar clientes |
| POST | `/clients` | Cadastrar cliente |
| GET | `/clients/{id}` | Consultar cliente |
| PUT | `/clients/{id}` | Atualizar dados cadastrais |
| GET | `/clients/{id}/balance` | Consultar saldo ou limite/consumo/disponível |
| POST | `/clients/{id}/credits` | Adicionar saldo pré-pago |
| PUT | `/clients/{id}/credit-limit` | Definir limite pós-pago |
| POST | `/messages` | Registrar, cobrar e enfileirar mensagem |
| GET | `/messages` | Listar mensagens com filtros |
| GET | `/messages/{id}` | Consultar mensagem do cliente identificado |
| GET | `/messages/{id}/status` | Consultar status |
| POST | `/messages/{id}/delivery-confirmation` | Simular confirmação de entrega |
| POST | `/messages/{id}/read-confirmation` | Simular confirmação de leitura |
| GET | `/conversations` | Listar conversas do cliente identificado |
| GET | `/conversations/{id}` | Consultar conversa |
| GET | `/conversations/{id}/messages` | Consultar histórico |
| GET | `/queue/status` | Consultar tamanho e contadores da fila |

As operações de clientes e fila refletem o controle de acesso efetivamente
implementado nos controllers; sua finalidade administrativa não implica proteção
por um papel `ADMIN`. Um modelo completo de autorização permanece como evolução.

As listagens de mensagens e conversas utilizam `page` a partir de zero e `size`
entre 1 e 100 (padrão 20). A listagem de mensagens aceita `conversationId`, `status`,
`priority` e `channel`. Enums são enviados como `NORMAL`, `URGENT`, `SMS`, `WHATSAPP`,
`SENT` etc., conforme os tipos Java.

## Exemplo de utilização

Cadastre um cliente pré-pago:

```json
{
  "name": "Empresa de exemplo",
  "documentId": "52998224725",
  "documentType": "CPF",
  "planType": "PREPAID"
}
```

Use o ID retornado para `POST /clients/{id}/credits`:

```json
{"amount": 10.00}
```

Para pós-pago, cadastre com `POSTPAID` e defina o limite por
`PUT /clients/{id}/credit-limit`:

```json
{"newLimit": 100.00}
```

Envie `POST /messages` com o header de identificação. Para criar uma conversa e
um destinatário no mesmo fluxo:

```json
{
  "recipientName": "Maria",
  "recipientPhone": "+5544999990000",
  "content": "Olá!",
  "priorityType": "NORMAL",
  "channelType": "WHATSAPP"
}
```

Para uma conversa existente, informe `conversationId`; para um destinatário
existente sem informar conversa, utilize `recipientId`. IDs informados e não
encontrados são rejeitados, sem criação silenciosa de um substituto.

A resposta inicial usa `201 Created`:

```json
{
  "messageId": 42,
  "statusType": "QUEUED"
}
```

O worker pode avançar o estado antes mesmo de a resposta chegar ao solicitante.
Consulte `/messages/42/status` para obter o estado atual.

Após `SENT`, execute a confirmação de entrega; após `DELIVERED`, a confirmação de
leitura. Essas operações não recebem corpo e utilizam o mesmo header. Não cobram
novamente. Confirmações repetidas/inválidas são rejeitadas pela regra de transição.

## Arquitetura e responsabilidades

- `Client`: regras de saldo, limite e consumo mensal.
- `Message`: estados, custo, contagem de tentativas e elegibilidade de retry.
- `Conversation` e `Recipient`: relacionamento com o cliente e destinatário final.
- `FinancialTransaction`: histórico das recargas e cobranças.
- `MessageRegistrationService`: cadastro e cobrança na mesma transação.
- `BillingService`: escolha da regra pré/pós-paga e registro financeiro.
- `MessageService`: entrada do envio e confirmações simuladas.
- `InMemoryMessageQueue`: duas filas FIFO e escolha da próxima mensagem.
- `MessageQueueWorker`: reagendamento e consumo em background.
- `SingleMessageProcessingService`: coordena uma tentativa de envio.
- `MessageStateService`: persiste cada transição em uma transação própria.
- `MessageRetryService`: busca tentativas vencidas e as recoloca na fila.
- Services de consulta: DTOs, paginação e filtros por proprietário.

Controllers recebem requisições e delegam. Entidades preservam regras de negócio;
services coordenam transações. A API retorna DTOs, sem expor entidades JPA.

## Cobrança e transações

| Prioridade | Custo |
|---|---|
| NORMAL | R$0,25 |
| URGENT | R$0,50 |

Pré-pago: desconta o custo do saldo na aceitação da mensagem.
Pós-pago: soma o custo ao consumo mensal, respeitando o limite configurado.
O limite não diminui a cada cobrança.

O mês de referência usa `America/Sao_Paulo`. A primeira cobrança aceita de um mês
posterior inicia um novo acumulado. Nas consultas, consumo de um mês anterior é
considerado zero sem atualizar a entidade. Uma referência anterior ao período
registrado é rejeitada na cobrança.

É permitido reduzir o limite abaixo do consumo existente, inclusive para zero.
Isso não apaga consumo: o disponível é apresentado como zero e novos envios são
bloqueados até haver limite disponível. Ajustar limite não é uma recarga e não
produz uma transação `CREDIT`.

Cadastro de mensagem, débito/consumo e registro financeiro são confirmados juntos.
Se a cobrança falhar, a transação desfaz também mensagem e conversa/destinatário
criados naquele fluxo. O `@Version` de Client detecta atualizações concorrentes;
o conflito deve ser tratado, não implica sucesso automático das duas operações.

A cobrança ocorre uma vez, antes do enfileiramento. Retries reutilizam a mensagem
original. Falha de envio não provoca estorno automático. Renovação de limite
mensal não representa quitação financeira.

## Fila, prioridade e worker

A fila armazena IDs em duas `ArrayDeque`, uma normal e uma urgente. Cada uma
preserva FIFO. Com ambas abastecidas, atende até três urgentes antes de atender
uma normal. Com apenas uma abastecida, continua processando-a.

A decisão e o contador de urgentes consecutivas ficam no `dequeue()`. Operações
na fila são sincronizadas. O contador não é reiniciado entre lotes; é zerado ao
atender uma normal ou observar ambas as filas vazias.

O worker utiliza `@Scheduled` e uma execução coordenada por vez na instância.
Primeiro busca até 100 retries vencidos e depois processa até 100 itens da fila.
Ao terminar, aguarda o intervalo antes da próxima execução:

```properties
queue.worker.delay-ms=500
```

Esse intervalo não é uma espera entre mensagens. O envio HTTP não chama mais o
processamento. A confirmação de `PROCESSING`, o envio simulado e a confirmação do
resultado são etapas separadas; o envio ocorre fora da transação de estado.

## Retry e estados

Fluxo de sucesso: `QUEUED → PROCESSING → SENT → DELIVERED → READ`.

Em falha de envio prevista (`MessageDeliveryException`):

- Até três tentativas totais, incluindo a primeira.
- Intervalo de cinco segundos entre tentativas (política atual no service).
- `attempts` aumenta somente ao entrar em `PROCESSING`.
- Uma falha recuperável permanece `FAILED` com `nextAttemptAt` preenchido.
- Ao vencer o horário: `FAILED → QUEUED`, limpando a data antes de enfileirar.
- Na terceira falha, `nextAttemptAt` fica nulo e não há novo agendamento.

`ProcessingResult` distingue sucesso, retry agendado e falha definitiva para os
contadores; não substitui o status persistido. Entrega e leitura são confirmações
simuladas por endpoints, não acontecimentos comprovados por um provedor externo.

## Monitoramento

Exemplo de `/queue/status`:

```json
{"size": 3, "processed": 12, "sent": 10, "failed": 2}
```

- `size`: IDs aguardando nas duas filas; não inclui processamento ou espera por retry.
- `sent`: mensagens cujo processamento terminou com sucesso nesta execução.
- `failed`: mensagens que esgotaram as tentativas nesta execução.
- `processed`: `sent + failed`; falha temporária não incrementa esses totais.

Os contadores são em memória e reiniciam com a aplicação. Entrega/leitura não
incrementam sucesso novamente. O tamanho é lido separadamente do snapshot dos
contadores; não representa uma fotografia atômica de todo o sistema.

## Histórico e decisões de domínio

A conversa nasce no fluxo da primeira mensagem. Não há endpoint para criar uma
conversa vazia. Destinatário final (`Recipient`) e cliente pagante (`Client`) são
entidades distintas. `Message` referencia `Conversation`; não é necessário carregar
uma coleção de todas as mensagens para consultar uma conversa.

Última mensagem e contador são calculados em consultas. `unreadCount` representa
mensagens de saída em `SENT` ou `DELIVERED`, ainda não lidas pelo destinatário.
Não representa uma caixa de entrada. A conversa é ordenada por ID decrescente;
o histórico é ordenado por timestamp e ID crescentes.

## Testes

```bash
./mvnw test
```

No PowerShell:

```powershell
.\mvnw.cmd test
```

As suítes unitárias utilizam entidades reais ou mocks conforme a responsabilidade.
`MessageFixtures` é somente uma fábrica de objetos de teste em `src/test/java`;
não é um componente Spring nem grava dados no banco.

A suíte `AsyncBillingIntegrationTest` exige Docker acessível e cria um PostgreSQL
isolado com Testcontainers. Aplica migrations reais, usa relógio controlado e
substitui o worker automático e o sender por mocks. Os services de processamento,
retry e cobrança são reais. Não há `sleep` para esperar cinco segundos.

```bash
./mvnw -Dtest=AsyncBillingIntegrationTest test
```

O teste valida transações e retry invocando os services; não comprova o disparo de
`@Scheduled`. Este pode ser verificado pelo fluxo manual de envio e consulta.

Os testes de integração ainda precisam ser executados com Docker acessível antes
da entrega: a última execução relatada falhou na descoberta do ambiente Docker,
antes de executar os cenários. Não há afirmação de cobertura percentual neste README.

## Swagger / OpenAPI

A documentação fica disponível em:

- Swagger UI: `http://localhost:8086/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8086/v3/api-docs`

O header é documentado nos endpoints que efetivamente o recebem. A documentação
não adiciona autenticação nem autorização à aplicação.

## Limitações e trabalho futuro

- Escopo focado em envio e consulta; recebimento externo não foi implementado.
- Integração SMS/WhatsApp real, pagamento de faturas, estorno e conversão entre
  planos não foram implementados.
- Não há autorização administrativa completa.
- Fila e contadores são locais a uma instância, sem suporte a múltiplas réplicas.
- Restart perde a fila. O banco mantém as mensagens, mas não há reconstrução automática.
- Commit no banco e enfileiramento não são atômicos; interrupções entre as etapas
  podem deixar mensagens fora da fila.
- Falhas inesperadas de banco/envio e interrupção após `PROCESSING` não têm recuperação
  automática. O retry atual trata a exceção de entrega prevista.
- Não há garantia de envio exatamente uma vez; integrações reais exigiriam idempotência.
- Cache, métricas de latência e monitoramento avançado permanecem como evolução.

Essas limitações mantêm o escopo centrado na estrutura de fila e nas regras de
processamento, conforme a flexibilidade permitida no FAQ do desafio.
