package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.CreditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ClientServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bcb_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add(
                "spring.flyway.locations",
                () -> "classpath:db/migration"
        );
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    transactions,
                    messages,
                    conversations,
                    recipients,
                    clients
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void shouldPersistCreditAndFinancialTransaction() {
        Long clientId = createClient();

        var response = clientService.addCredit(
                clientId,
                new CreditRequest(new BigDecimal("50.00"))
        );

        BigDecimal persistedBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM clients WHERE id = ?",
                BigDecimal.class,
                clientId
        );

        assertThat(persistedBalance)
                .isEqualByComparingTo("50.00");

        assertThat(response.balance())
                .isEqualByComparingTo("50.00");

        Long transactionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE client_id = ?",
                Long.class,
                clientId
        );

        assertThat(transactionCount).isEqualTo(1L);

        var transaction = jdbcTemplate.queryForMap("""
                SELECT id, transaction_type, amount, message_id
                FROM transactions
                WHERE client_id = ?
                """, clientId);

        assertThat(transaction.get("id"))
                .isEqualTo(response.transactionId());

        assertThat(transaction.get("transaction_type"))
                .isEqualTo("CREDIT");

        assertThat((BigDecimal) transaction.get("amount"))
                .isEqualByComparingTo("50.00");

        assertThat(transaction.get("message_id")).isNull();
    }

    @Test
    void shouldRollbackBalanceWhenFinancialTransactionFails() {
        Long clientId = createClient();


        jdbcTemplate.execute("""
                ALTER TABLE transactions
                ADD CONSTRAINT ck_test_reject_amount
                CHECK (amount <> 50.00)
                """);

        try {
            assertThatThrownBy(() -> clientService.addCredit(
                    clientId,
                    new CreditRequest(new BigDecimal("50.00"))
            )).isInstanceOf(DataIntegrityViolationException.class);

            BigDecimal persistedBalance = jdbcTemplate.queryForObject(
                    "SELECT balance FROM clients WHERE id = ?",
                    BigDecimal.class,
                    clientId
            );

            Long transactionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM transactions WHERE client_id = ?",
                    Long.class,
                    clientId
            );

            assertThat(persistedBalance)
                    .isEqualByComparingTo("0.00");

            assertThat(transactionCount).isZero();
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE transactions
                    DROP CONSTRAINT ck_test_reject_amount
                    """);
        }
    }

    private Long createClient() {
        Client client = new Client(
                "Cliente Integração",
                "52998224725",
                DocumentType.CPF,
                PlanType.PREPAID
        );

        return clientRepository.saveAndFlush(client).getId();
    }
}