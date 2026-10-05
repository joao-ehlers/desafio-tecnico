Big Chat Brasil (BCB) — Desafio Backend
Implementação do desafio técnico backend da plataforma Big Chat Brasil, com foco em processamento de mensagens, filas e validação financeira.
O desenvolvimento é incremental. A primeira etapa contempla o plano pré-pago e uma fila FIFO com processamento síncrono. A evolução prevista inclui prioridade de mensagens e prevenção de starvation.
Status: em desenvolvimento. A modelagem, as migrations e a base de gerenciamento de clientes foram implementadas. O fluxo de recarga e o processamento de mensagens são os próximos passos.

Tecnologias
- Java e Spring Boot
- Maven e Maven Wrapper
- Spring Data JPA e Hibernate
- Jakarta Bean Validation
- Lombok
- PostgreSQL
- Flyway
- Docker e Docker Compose
- JUnit, Mockito e AssertJ para os testes unitários propostos
  As versões de Java e das dependências são definidas no pom.xml e no Dockerfile.
  Como executar
  Com Docker Compose
  Pré-requisitos: Git, Docker e Docker Compose. Não é necessário instalar Java ou Maven localmente quando o build é realizado pelo Dockerfile.
  git clone https://github.com/joao-ehlers/desafio-tecnico.git
  cd desafio-tecnico
  Crie o arquivo .env a partir de .env.example e preencha as variáveis utilizadas pelo Compose.
  Linux/macOS:
  cp .env.example .env
  Windows PowerShell:
  Copy-Item .env.example .env
  Em seguida:
  docker compose up --build
  A API é exposta em http://localhost:8086, conforme o mapeamento de portas do Compose.
  O arquivo .env não deve ser versionado. O .env.example documenta as variáveis necessárias sem credenciais reais. As variáveis de conexão da aplicação devem corresponder ao banco, usuário e senha configurados no serviço PostgreSQL.
  Para encerrar os serviços:
  docker compose down
  Pela IDE ou Maven Wrapper
  Utilize a versão de Java definida no projeto e mantenha o PostgreSQL disponível. Configure na execução da aplicação as variáveis de ambiente referenciadas pelo application.properties.
  O Spring Boot não carrega automaticamente o .env do Compose ao executar pela IDE. Nesse cenário, o endereço do banco deve usar localhost e a porta publicada pelo Compose; dentro da rede do Compose, deve usar o nome do serviço PostgreSQL e sua porta interna.
  Linux/macOS:
  ./mvnw spring-boot:run
  Windows PowerShell:
  .\mvnw.cmd spring-boot:run
  A porta da execução local é definida pela configuração da aplicação; o mapeamento externo 8086 do Compose não a altera.
  Banco de dados
  O schema é versionado por migrations Flyway em:
  src/main/resources/db/migration
  As migrations criam as tabelas clients, recipients, conversations, messages e transactions, com chaves estrangeiras, restrições e índices de consulta.
  O Flyway é responsável pela evolução do schema. O Hibernate deve verificar sua compatibilidade com as entidades:
  spring.jpa.hibernate.ddl-auto=validate
  Na primeira execução, utilize um banco sem tabelas prévias da aplicação. Se já existir um schema criado por outro mecanismo, ele precisa ser reconciliado antes de aplicar as migrations. Não altere migrations já aplicadas em ambientes compartilhados; crie uma nova versão.
  Estado da implementação
  Implementado no código
- Entidades Client, Recipient, Conversation, Message e FinancialTransaction.
- Migrations iniciais do PostgreSQL.
- Cadastro de clientes com saldo inicial zero e plano pré-pago.
- Listagem e consulta de clientes.
- Atualização dos dados cadastrais, sem conversão de plano.
- Consulta de saldo.
- Lógica de identificação por CPF/CNPJ e rejeição de cliente inativo.
- Normalização e validação de CPF e CNPJ, incluindo CNPJ alfanumérico.
- Verificação de documento duplicado no cadastro e na atualização.
- Tratamento centralizado de exceções com ProblemDetail.
- Regras de crédito e débito na entidade Client.
- Custo por prioridade e transições de status na entidade Message.
- Campo de versão em Client para controle otimista de concorrência.
  Regras implementadas nas entidades não significam que o fluxo completo de envio ou recarga já esteja disponível. A validação integrada desses fluxos faz parte das próximas etapas.

Próximas etapas — Parte 1
- [ ] Disponibilizar recarga com registro financeiro na mesma transação.
- [ ] Disponibilizar consulta do histórico financeiro.
- [ ] Implementar envio com validação e débito do saldo.
- [ ] Implementar fila FIFO em memória e processamento síncrono.
- [ ] Simular o envio e persistir seu resultado.
- [ ] Disponibilizar consultas de mensagens, conversas e histórico.
- [ ] Validar a integração com PostgreSQL e os conflitos concorrentes.
  Parte 2
- [ ] Identificar o cliente por CPF/CNPJ no header.
- [ ] Implementar prioridade normal/urgente na fila.
- [ ] Implementar balanceamento para evitar starvation.
- [ ] Simular atualizações de entrega e leitura.
  Evoluções posteriores
- Plano pós-pago com limite e consumo por competência mensal.
- Conversão de planos com tratamento de saldo ou consumo pendente.
- Processamento assíncrono, recuperação de pendências e reprocessamento.
- Estatísticas e monitoramento da fila.
  Endpoints
  Gerenciamento de clientes
  Método	Rota	Descrição
  GET	/clients	Lista clientes; retorna uma lista vazia se não houver registros
  GET	/clients/{id}	Consulta um cliente
  GET	/clients/{id}/balance	Consulta o saldo pré-pago
  POST	/clients	Cadastra um cliente; retorna 201 Created e Location
  PUT	/clients/{id}	Atualiza nome, documento e tipo de documento


O contrato de identificação é POST /auth, recebendo documentId e retornando clientId. A lógica foi implementada no service; a exposição dessa rota deve ser verificada junto do controller de autenticação.
Endpoints planejados
Método	Rota	Descrição
POST	/clients/{id}/credits	Adiciona crédito pré-pago e registra a movimentação
GET	/clients/{id}/transactions	Consulta o histórico financeiro
GET	/conversations	Lista conversas do cliente identificado
GET	/conversations/{id}	Consulta uma conversa
GET	/conversations/{id}/messages	Consulta o histórico da conversa
POST	/messages	Solicita o envio de uma mensagem
GET	/messages	Lista mensagens com filtros
GET	/messages/{id}	Consulta uma mensagem
GET	/messages/{id}/status	Consulta o status de uma mensagem
GET	/queue/status	Consulta estatísticas da fila em uma evolução posterior


Recarga e histórico financeiro foram incluídos para atender às regras de negócio, embora suas rotas não tenham sido detalhadas na lista de APIs do desafio.
Exemplo de cadastro
POST /clients
Content-Type: application/json
{
"name": "Cliente Exemplo",
"documentId": "529.982.247-25",
"documentType": "CPF",
"planType": "PREPAID"
}
Resposta esperada: 201 Created, header Location: /clients/1 e corpo:
{
"clientId": 1
}
Na atualização cadastral, envie name, documentId e documentType. A troca de plano não faz parte desse contrato.
Arquitetura e modelagem
O projeto utiliza um monólito organizado por funcionalidade, com controllers para o contrato HTTP, services para coordenar os casos de uso, repositories para persistência e entidades para regras de estado.
Entidade	Responsabilidade
Client	Contratante da plataforma, documento, plano, saldo, limite e situação
Recipient	Contato que recebe mensagens, sem login, plano ou saldo
Conversation	Associação entre um cliente e um destinatário
Message	Conteúdo, canal, prioridade, custo, horário e status do envio
FinancialTransaction	Registro de crédito ou débito, associado à mensagem quando aplicável


Conteúdo e horário da última mensagem e contagem de não lidas serão obtidos por consultas e incluídos no DTO da conversa. Não são campos persistidos em Conversation.
Decisões e premissas
Identificação e operações administrativas
O /auth realiza identificação simplificada por documento. Não emite token nem cria sessão. O CPF/CNPJ não é uma credencial secreta e não comprova a identidade de quem chama a API.
Na Parte 1, as operações de mensagens e conversas utilizarão o clientId informado na requisição. A associação entre cliente e conversa será verificada no service, mas isso não substitui autenticação.
A documentação menciona operações administrativas e marca a listagem de clientes como (admin), sem definir credenciais ou papéis. Nesta etapa, as operações não possuem autorização administrativa. Essa limitação é explícita; nenhuma classe Admin, senha compartilhada ou sistema de papéis foi introduzido.
Financeiro
- A primeira entrega aceita apenas o plano pré-pago. A presença de campos ou constantes de pós-pago no modelo não representa suporte ao seu fluxo.
- Valores monetários usam BigDecimal e colunas NUMERIC(12,2).
- Clientes inativos não podem realizar novas recargas nem envios; saldo e histórico são preservados.
- Crédito por recarga não possui mensagem associada. Débito por envio deve referenciar uma mensagem e ter valor igual ao seu custo.
- O fluxo financeiro deverá alterar saldo e registrar a movimentação na mesma transação de banco.
- O @Version de Client permite detectar conflitos de atualização. Sua integração e o comportamento concorrente ainda precisam ser testados.
  Mensagens e conversas
- Prioridade normal custa R$ 0,25 e urgente custa R$ 0,50. O preço é calculado no servidor e armazenado na mensagem.
- Os canais previstos são SMS e WhatsApp. O envio será simulado, sem integração com provedores externos.
- O limite de conteúdo adotado é de 2.000 caracteres, uma premissa do projeto.
- A conversa será criada junto da primeira mensagem aceita, na mesma transação, evitando persistir uma conversa vazia quando a operação falhar.
- Apesar da menção a “envio e recebimento”, não há contrato detalhado para respostas externas. O escopo adotado contempla envio e consulta de histórico; entrada de respostas do destinatário não será implementada nesta etapa.
- unreadCount será calculado como a quantidade de mensagens em SENT ou DELIVERED: envios sem confirmação de leitura pelo destinatário. Mensagens QUEUED, PROCESSING, READ e FAILED não entram na contagem.
- Entrega e leitura representam atualizações da mensagem enviada, não mensagens de resposta.
  Testes
  Foi preparada uma suíte unitária para ClientService, com JUnit, Mockito e AssertJ. Ela utiliza entidades e validação de documentos reais e simula apenas o repository.
  Os cenários cobrem cadastro, normalização, duplicidade, restrição de plano, atualização, identificação de clientes ativos/inativos e consultas. A execução e a compatibilidade com os DTOs do projeto ainda devem ser confirmadas; não há percentual de cobertura declarado.
  Para executar os testes disponíveis no projeto:
  ./mvnw test
  No Windows PowerShell:
  .\mvnw.cmd test
  Após adicionar a classe de teste, é possível executar apenas a suíte do service:
  .\mvnw.cmd "-Dtest=ClientServiceTest" test
  Testes unitários com repository simulado não verificam migrations, constraints reais, rollback, concorrência no PostgreSQL, rotas HTTP ou o advice. Esses comportamentos precisam de testes de integração e de controller.
  Limitações atuais
- A implementação completa de envio, fila e recarga ainda está em desenvolvimento.
- Não há autenticação forte nem autorização administrativa.
- Validação de CPF/CNPJ verifica estrutura e dígitos verificadores, não existência ou situação cadastral na Receita Federal.
- O pós-pago e a conversão de planos ainda não estão disponíveis.
- O envio real por SMS/WhatsApp e respostas externas não fazem parte do escopo atual.
- A fila prevista é em memória e não oferece durabilidade após reinício. Recuperação de mensagens pendentes será uma evolução separada.
  As funcionalidades e limitações serão atualizadas conforme os fluxos forem implementados e verificados.
  Possivel melhoria:
- consultar transações(adm)