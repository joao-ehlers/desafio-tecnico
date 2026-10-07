package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.support.MessageFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageStateServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
    @Mock MessageRepository repository;
    private MessageStateService service;
    @BeforeEach void setUp() {
        service = new MessageStateService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void shouldStartProcessingOnLoadedEntity() {
        Message message = MessageFixtures.queued();
        when(repository.findById(42L)).thenReturn(Optional.of(message));
        assertThat(service.startProcessing(42L)).isSameAs(message);
        assertThat(message.getStatus()).isEqualTo(StatusType.PROCESSING);
        assertThat(message.getAttempts()).isEqualTo(1);
    }

    @Test void shouldScheduleFirstFailureUsingClockAndPolicy() {
        Message message = MessageFixtures.queued();
        message.startProcessing();
        when(repository.findById(42L)).thenReturn(Optional.of(message));
        assertThat(service.markAsFailed(42L)).isEqualTo(ProcessingResult.RETRY_SCHEDULED);
        assertThat(message.getStatus()).isEqualTo(StatusType.FAILED);
        assertThat(message.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test void shouldReturnFinalFailureAtThirdAttempt() {
        Message message = MessageFixtures.queued();
        for (int i = 0; i < 2; i++) {
            message.startProcessing();
            message.markAsFailed(NOW.minusSeconds(5), 3, Duration.ofSeconds(5));
            message.queueForRetry(NOW);
        }
        message.startProcessing();
        when(repository.findById(42L)).thenReturn(Optional.of(message));
        assertThat(service.markAsFailed(42L)).isEqualTo(ProcessingResult.FAILED);
        assertThat(message.getNextAttemptAt()).isNull();
        assertThat(message.getAttempts()).isEqualTo(3);
    }

    @Test void shouldMarkSuccess() {
        Message message = MessageFixtures.queued();
        message.startProcessing();
        when(repository.findById(42L)).thenReturn(Optional.of(message));
        assertThat(service.markAsSent(42L)).isEqualTo(ProcessingResult.SENT);
        assertThat(message.getStatus()).isEqualTo(StatusType.SENT);
    }

    @Test void shouldQueueRetryAtDueTimeWithoutIncrementingAttempts() {
        Message message = MessageFixtures.queued();
        message.startProcessing();
        message.markAsFailed(NOW.minusSeconds(5), 3, Duration.ofSeconds(5));
        when(repository.findById(42L)).thenReturn(Optional.of(message));
        assertThat(service.queueForRetry(42L)).isSameAs(message);
        assertThat(message.getStatus()).isEqualTo(StatusType.QUEUED);
        assertThat(message.getAttempts()).isEqualTo(1);
        assertThat(message.getNextAttemptAt()).isNull();
    }

    @Test void shouldRejectMissingMessage() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.startProcessing(42L))
                .isInstanceOf(MessageNotFoundException.class);
    }
}
