package com.irrah.desafio_tecnico.queue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueInitializerTest {

    @Test
    void shouldReleaseQueueOnlyAfterRecoveryCompletes() {
        QueueReadiness readiness = new QueueReadiness();
        QueueRecoveryService recovery = mock(QueueRecoveryService.class);

        QueueInitializer initializer =
                new QueueInitializer(readiness, recovery);

        assertThat(readiness.isReady()).isFalse();

        doAnswer(invocation -> {
            assertThat(readiness.isReady()).isFalse();
            return null;
        }).when(recovery).recoverQueuedMessages();

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(recovery).recoverQueuedMessages();
        assertThat(readiness.isReady()).isTrue();
    }

    @Test
    void shouldKeepQueueBlockedWhenRecoveryFails() {
        QueueReadiness readiness = new QueueReadiness();
        QueueRecoveryService recovery = mock(QueueRecoveryService.class);

        QueueInitializer initializer =
                new QueueInitializer(readiness, recovery);

        var failure = new IllegalStateException("recovery failed");
        doThrow(failure).when(recovery).recoverQueuedMessages();

        assertThatThrownBy(() ->
                initializer.run(
                        new DefaultApplicationArguments(new String[0])
                )
        ).isSameAs(failure);

        assertThat(readiness.isReady()).isFalse();
    }
}