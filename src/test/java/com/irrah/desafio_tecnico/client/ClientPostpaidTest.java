package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.billing.exception.ExceedingValueException;
import com.irrah.desafio_tecnico.billing.exception.InsufficientFundsException;
import com.irrah.desafio_tecnico.client.exception.ClientNotActiveException;
import com.irrah.desafio_tecnico.client.exception.InvalidPlanOperationException;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ClientPostpaidTest {

    private static final LocalDate OCTOBER =
            LocalDate.of(2026, 10, 1);

    private Client postpaidClient(String limit) {
        Client client = new Client(
                "Empresa",
                "52998224725",
                DocumentType.CPF,
                PlanType.POSTPAID
        );

        client.adjustCreditLimit(new BigDecimal(limit));
        return client;
    }

    @Test
    void shouldAccumulateConsumptionWithoutChangingLimit() {
        Client client = postpaidClient("100.00");

        client.consumeCredit(new BigDecimal("0.25"), OCTOBER);
        client.consumeCredit(new BigDecimal("0.50"), OCTOBER);

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("0.75");
        assertThat(client.getCreditLimit())
                .isEqualByComparingTo("100.00");
        assertThat(client.getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(client.getConsumptionMonth()).isEqualTo(OCTOBER);
    }

    @Test
    void shouldAllowConsumptionExactlyAtLimit() {
        Client client = postpaidClient("1.00");

        client.consumeCredit(new BigDecimal("1.00"), OCTOBER);

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("1.00");
    }

    @Test
    void shouldRejectConsumptionAboveAvailableLimitWithoutChangingState() {
        Client client = postpaidClient("1.00");
        client.consumeCredit(new BigDecimal("0.75"), OCTOBER);

        assertThatThrownBy(() ->
                client.consumeCredit(new BigDecimal("0.50"), OCTOBER)
        ).isInstanceOf(InsufficientFundsException.class);

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("0.75");
        assertThat(client.getConsumptionMonth()).isEqualTo(OCTOBER);
    }

    @Test
    void shouldStartNewConsumptionPeriodInFollowingMonth() {
        Client client = postpaidClient("10.00");
        client.consumeCredit(new BigDecimal("9.00"), OCTOBER);

        client.consumeCredit(
                new BigDecimal("0.25"),
                LocalDate.of(2026, 11, 15)
        );

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("0.25");
        assertThat(client.getConsumptionMonth())
                .isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void shouldPreservePreviousPeriodWhenNewMonthChargeIsRejected() {
        Client client = postpaidClient("10.00");
        client.consumeCredit(new BigDecimal("9.00"), OCTOBER);

        assertThatThrownBy(() ->
                client.consumeCredit(
                        new BigDecimal("11.00"),
                        OCTOBER.plusMonths(1)
                )
        ).isInstanceOf(InsufficientFundsException.class);

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("9.00");
        assertThat(client.getConsumptionMonth()).isEqualTo(OCTOBER);
    }

    @Test
    void shouldRejectPreviousMonthWithoutChangingConsumption() {
        Client client = postpaidClient("10.00");
        client.consumeCredit(new BigDecimal("1.00"), OCTOBER);

        assertThatThrownBy(() ->
                client.consumeCredit(
                        new BigDecimal("0.25"),
                        OCTOBER.minusMonths(1)
                )
        ).isInstanceOf(InvalidInputException.class);

        assertThat(client.getMonthlyConsumption())
                .isEqualByComparingTo("1.00");
        assertThat(client.getConsumptionMonth()).isEqualTo(OCTOBER);
    }

    @Test
    void shouldRejectInvalidAmountsAndMissingMonth() {
        Client client = postpaidClient("10.00");

        assertThatThrownBy(() -> client.consumeCredit(null, OCTOBER))
                .isInstanceOf(InvalidInputException.class);

        for (String value : new String[]{"0", "-1", "0.001"}) {
            assertThatThrownBy(() ->
                    client.consumeCredit(new BigDecimal(value), OCTOBER)
            ).isInstanceOf(InvalidInputException.class);
        }

        assertThatThrownBy(() ->
                client.consumeCredit(new BigDecimal("0.25"), null)
        ).isInstanceOf(InvalidInputException.class);

        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("0");
        assertThat(client.getConsumptionMonth()).isNull();
    }

    @Test
    void shouldRejectConsumptionForInactiveClient() {
        Client client = postpaidClient("10.00");
        client.deactivate();

        assertThatThrownBy(() ->
                client.consumeCredit(new BigDecimal("0.25"), OCTOBER)
        ).isInstanceOf(ClientNotActiveException.class);

        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRejectConsumptionAndLimitAdjustmentForPrepaidClient() {
        Client client = new Client(
                "Empresa", "52998224725",
                DocumentType.CPF, PlanType.PREPAID
        );

        assertThatThrownBy(() ->
                client.consumeCredit(new BigDecimal("0.25"), OCTOBER)
        ).isInstanceOf(InvalidPlanOperationException.class);

        assertThatThrownBy(() ->
                client.adjustCreditLimit(new BigDecimal("10.00"))
        ).isInstanceOf(InvalidPlanOperationException.class);
    }

    @Test
    void shouldAllowZeroLimitWithoutErasingConsumption() {
        Client client = postpaidClient("10.00");
        client.consumeCredit(new BigDecimal("1.00"), OCTOBER);

        client.adjustCreditLimit(BigDecimal.ZERO);

        assertThat(client.getCreditLimit()).isEqualByComparingTo("0");
        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("1");

        assertThatThrownBy(() ->
                client.consumeCredit(new BigDecimal("0.25"), OCTOBER)
        ).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldRejectInvalidLimitsWithoutChangingExistingLimit() {
        Client client = postpaidClient("10.00");

        assertThatThrownBy(() -> client.adjustCreditLimit(null))
                .isInstanceOf(InvalidInputException.class);

        for (String value : new String[]{"-1", "0.001"}) {
            assertThatThrownBy(() ->
                    client.adjustCreditLimit(new BigDecimal(value))
            ).isInstanceOf(InvalidInputException.class);
        }

        assertThatThrownBy(() ->
                client.adjustCreditLimit(new BigDecimal("10000000000.00"))
        ).isInstanceOf(ExceedingValueException.class);

        assertThat(client.getCreditLimit()).isEqualByComparingTo("10");
    }

    @Test
    void shouldRejectLimitAdjustmentForInactiveClient() {
        Client client = postpaidClient("10.00");
        client.deactivate();

        assertThatThrownBy(() ->
                client.adjustCreditLimit(new BigDecimal("20.00"))
        ).isInstanceOf(ClientNotActiveException.class);

        assertThat(client.getCreditLimit()).isEqualByComparingTo("10");
    }
}
