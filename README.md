# Big Chat Brasil (BCB) - Desafio Backend

Implementação do desafio técnico **Backend** da plataforma Big Chat Brasil: um sistema de filas para processamento de mensagens de chat entre empresas e seus clientes, com priorização (normal/urgente) e validação financeira por plano (pré-pago e pós-pago).

> **Status:** em desenvolvimento. Este README será atualizado conforme o projeto avança.

## Tecnologias

- Java (Spring Boot)
- Maven
- PostgreSQL
- Flyway (migrations)
- Docker e Docker Compose
- JUnit 5 e Mockito (testes)

> A lista será ajustada ao final para refletir exatamente o que foi utilizado.

## Pré-requisitos

- Java 21 (ou a versão definida no `pom.xml`)
- Maven 3.9+
- Docker e Docker Compose

## Como executar

> Instruções definitivas serão adicionadas junto com o `docker-compose.yml`.

```bash
# Clonar o repositório
git clone https://github.com/joao-ehlers/desafio-tecnico.git
cd desafio-tecnico

# Subir a aplicação e o banco (após a configuração do Docker)
docker compose up --build
```

A API ficará disponível em `http://localhost:8086`.

## Como executar os testes

```bash
./mvnw test
```

## Funcionalidades

### Parte 1 - Essenciais
- [ ] Autenticação simples do cliente por CPF/CNPJ
- [ ] Gerenciamento de clientes
- [ ] Envio de mensagens
- [ ] Validação de saldo (pré-pago) e limite (pós-pago)
- [ ] Fila de mensagens em memória (FIFO)
- [ ] Registro de status das mensagens
- [ ] Listagem de conversas e histórico de mensagens

### Parte 2 - Aprimoramentos
- [ ] Fila com prioridade (normal/urgente)
- [ ] Mecanismo anti-starvation
- [ ] Status detalhado de mensagens (enviada, entregue, lida)

### Parte 3 - Adicionais
- [ ] A definir conforme o tempo disponível

## Endpoints

> A documentação completa será adicionada conforme a implementação.

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth` | Autenticação do cliente |
| GET | `/conversations` | Lista as conversas do cliente |
| GET | `/conversations/{id}/messages` | Mensagens de uma conversa |
| POST | `/messages` | Envia uma nova mensagem |
| GET | `/messages/{id}/status` | Consulta o status de uma mensagem |
| GET | `/queue/status` | Estatísticas da fila |

## Arquitetura

> Estrutura de pacotes e diagrama serão adicionados após a definição das camadas.

## Decisões técnicas

> A preencher durante o desenvolvimento: escolha da estrutura de dados da fila, estratégia anti-starvation, tratamento de concorrência e regras de cobrança.

## Limitações conhecidas

> A preencher ao final do desenvolvimento.

## Trabalho futuro

> A preencher ao final do desenvolvimento.