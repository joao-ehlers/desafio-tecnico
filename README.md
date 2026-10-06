## Parte 1 — Escopo implementado

- Identificação simples de clientes por CPF/CNPJ em `POST /auth`.
- Cadastro, atualização e consulta de clientes.
- Recarga de saldo pré-pago e ajuste de limite pós-pago.
- Envio de mensagens com criação de conversa e destinatário quando necessário.
- Cobrança por mensagem e persistência do histórico financeiro.
- Fila em memória com processamento FIFO síncrono.
- Simulação de envio com resultado `SENT` ou `FAILED`.
- Consultas paginadas de mensagens, conversas e histórico.
- Consulta de estatísticas da fila.

### Endpoints

| Método | Rota | Finalidade |
|--------|------|------------|
| POST | `/auth` | Identificação simples por CPF/CNPJ |
| GET | `/clients` | Listar clientes |
| POST | `/clients` | Cadastrar cliente |
| GET | `/clients/{id}` | Consultar cliente |
| PUT | `/clients/{id}` | Atualizar dados cadastrais |
| GET | `/clients/{id}/balance` | Consultar saldo ou limite, consumo e disponível |
| POST | `/clients/{id}/credits` | Adicionar saldo pré-pago |
| PUT | `/clients/{id}/credit-limit` | Definir limite pós-pago |
| POST | `/messages` | Registrar, cobrar e processar uma mensagem |
| GET | `/messages` | Listar mensagens com filtros e paginação |
| GET | `/messages/{id}` | Consultar uma mensagem |
| GET | `/messages/{id}/status` | Consultar o status de uma mensagem |
| GET | `/conversations` | Listar conversas |
| GET | `/conversations/{id}` | Consultar uma conversa |
| GET | `/conversations/{id}/messages` | Consultar o histórico da conversa |
| GET | `/queue/status` | Consultar mensagens pendentes e contadores de processamento |

Nesta etapa, as consultas de mensagens e conversas recebem `clientId` como parâmetro. No envio, ele é informado no corpo da requisição. A identificação por CPF/CNPJ no header está prevista para a Parte 2.

### Fluxo de envio

1. Localizar e validar o cliente.
2. Resolver o destinatário e a conversa.
3. Criar a mensagem com o custo correspondente à prioridade.
4. Aplicar a cobrança e registrar a transação financeira.
5. Confirmar a transação do banco.
6. Enfileirar o ID da mensagem.
7. Processar a fila na mesma requisição HTTP.
8. Retornar o ID e o status final da mensagem.

A criação da mensagem, o débito ou consumo e o registro financeiro participam da mesma transação. Uma falha nessa etapa desfaz as alterações no banco.

### Regras financeiras

**Pré-pago:** o custo é descontado do saldo antes do enfileiramento.

**Pós-pago:** o custo é somado ao consumo do mês, respeitando o limite configurado. O limite permanece inalterado durante a cobrança.

- Mensagem normal: R$0,25.
- Mensagem urgente: R$0,50.
- A referência mensal utiliza o fuso `America/Sao_Paulo`.
- O primeiro consumo de um novo mês inicia um novo acumulado.
- Consultas financeiras consideram consumo zero quando o período armazenado é anterior ao mês atual.
- A redução de limite não apaga o consumo existente. Quando o consumo supera o novo limite, o disponível apresentado é zero e novos envios são bloqueados.
- Ajustar limite não gera uma recarga financeira.
- Falhas no envio simulado não geram estorno automático.
- A renovação mensal do disponível não representa quitação de débitos.

### Fila e monitoramento

A fila mantém IDs de mensagens em memória e os processa em ordem FIFO. Nesta etapa, a prioridade influencia o preço, mas não a ordem de processamento.

`GET /queue/status` retorna:

- `size`: quantidade de mensagens aguardando na fila.
- `processed`: soma dos processamentos concluídos com sucesso ou falha.
- `sent`: processamentos concluídos com sucesso.
- `failed`: processamentos concluídos com falha de envio.

Os contadores representam a execução atual da aplicação e são reiniciados com ela. O tamanho da fila não inclui mensagens já retiradas para processamento. Por ser síncrono, o fluxo normalmente deixa a fila vazia ao terminar a requisição.

### Interpretações do enunciado

- O escopo implementado contempla envio e consulta de mensagens. O recebimento de mensagens externas não possui fluxo implementado.
- O destinatário é representado por uma entidade própria, distinta do cliente pagante.
- A conversa é criada no fluxo da primeira mensagem.
- O resumo da conversa é calculado a partir das mensagens persistidas.
- `unreadCount` representa mensagens enviadas pelo cliente nos estados `SENT` ou `DELIVERED`, ainda não marcadas como `READ`.
- A listagem de conversas é ordenada por ID decrescente; o histórico de mensagens, por data e ID crescentes.

### Limitações e próximas etapas

- A identificação por documento ou `clientId` não comprova a identidade do solicitante e não equivale a autenticação de produção.
- As operações administrativas não possuem um fluxo completo de usuários e permissões administrativas.
- A entrega é simulada, sem integração com provedores externos.
- A fila em memória é local a uma instância e não sobrevive a reinicializações.
- A confirmação no banco e o enfileiramento não são atômicos. Uma interrupção entre essas etapas pode deixar mensagens persistidas sem processamento.
- Conversão entre planos e liquidação de consumo pós-pago permanecem pendentes.
- A Parte 2 acrescentará identificação por header, prioridade com prevenção de starvation e evolução dos status.
- A Parte 3 contempla processamento em background e reprocessamento de falhas.