package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.support.MessageFixtures;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class MessageRetryRulesTest {
    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
    private static final Duration DELAY = Duration.ofSeconds(5);

    @Test void shouldIncrementAttemptsOnlyWhenStartingProcessing() {
        Message message = MessageFixtures.queued();
        assertThat(message.getAttempts()).isZero();
        message.startProcessing();
        assertThat(message.getAttempts()).isEqualTo(1);
        message.markAsFailed(NOW, 3, DELAY);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getNextAttemptAt()).isEqualTo(NOW.plus(DELAY));
        message.queueForRetry(NOW.plus(DELAY));
        assertThat(message.getStatus()).isEqualTo(StatusType.QUEUED);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getNextAttemptAt()).isNull();
        message.startProcessing();
        assertThat(message.getAttempts()).isEqualTo(2);
    }

    @Test void shouldRejectEarlyRetryButAllowExactDueInstant() {
        Message message = MessageFixtures.queued();
        message.startProcessing(); message.markAsFailed(NOW, 3, DELAY);
        assertThatThrownBy(() -> message.queueForRetry(NOW.plusSeconds(4)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(message.getStatus()).isEqualTo(StatusType.FAILED);
        assertThat(message.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        message.queueForRetry(NOW.plusSeconds(5));
        assertThat(message.getStatus()).isEqualTo(StatusType.QUEUED);
    }

    @Test void shouldStopAfterExactlyThreeAttempts() {
        Message message = MessageFixtures.queued();
        Instant now = NOW;
        for (int attempt = 1; attempt <= 3; attempt++) {
            message.startProcessing();
            message.markAsFailed(now, 3, DELAY);
            assertThat(message.getAttempts()).isEqualTo(attempt);
            if (attempt < 3) {
                assertThat(message.getNextAttemptAt()).isEqualTo(now.plus(DELAY));
                now = now.plus(DELAY);
                message.queueForRetry(now);
            }
        }
        assertThat(message.getStatus()).isEqualTo(StatusType.FAILED);
        assertThat(message.getNextAttemptAt()).isNull();
        assertThatThrownBy(() -> message.queueForRetry(NOW.plusSeconds(100)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void shouldAllowSuccessAfterRetryWithoutSchedulingAnotherAttempt() {
        Message message = MessageFixtures.queued();
        message.startProcessing(); message.markAsFailed(NOW, 3, DELAY);
        message.queueForRetry(NOW.plus(DELAY));
        message.startProcessing(); message.markAsSent();
        assertThat(message.getStatus()).isEqualTo(StatusType.SENT);
        assertThat(message.getAttempts()).isEqualTo(2);
        assertThat(message.getNextAttemptAt()).isNull();
        message.markAsDelivered(); message.markAsRead();
        assertThat(message.getStatus()).isEqualTo(StatusType.READ);
        assertThatThrownBy(() -> message.queueForRetry(NOW.plusSeconds(30)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void shouldRejectInvalidTransitionsWithoutChangingAttempts() {
        Message message = MessageFixtures.queued();
        assertThatThrownBy(message::markAsSent).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(message::markAsDelivered).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(message::markAsRead).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> message.markAsFailed(NOW, 3, DELAY))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> message.queueForRetry(NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThat(message.getAttempts()).isZero();
        message.startProcessing();
        assertThatThrownBy(message::startProcessing).isInstanceOf(IllegalStateException.class);
        assertThat(message.getAttempts()).isEqualTo(1);
    }

    @Test void shouldRejectInvalidRetryPolicyBeforeChangingState() {
        Message message = MessageFixtures.queued();
        message.startProcessing();
        assertThatThrownBy(() -> message.markAsFailed(null, 3, DELAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message.markAsFailed(NOW, 0, DELAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message.markAsFailed(NOW, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message.markAsFailed(NOW, 3, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> message.markAsFailed(NOW, 3, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(message.getStatus()).isEqualTo(StatusType.PROCESSING);
        assertThat(message.getNextAttemptAt()).isNull();
    }
}
