package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.PriorityType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InMemoryMessageQueueTest {
    private final InMemoryMessageQueue queue = new InMemoryMessageQueue();

    @Test void shouldReturnNullWhenEmpty() {
        assertThat(queue.dequeue()).isNull();
        assertThat(queue.size()).isZero();
    }

    @Test void shouldRejectInvalidInputWithoutChangingQueue() {
        assertThatThrownBy(() -> queue.enqueue(null, PriorityType.NORMAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.enqueue(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(queue.size()).isZero();
    }

    @Test void shouldPreserveFifoWithinEachPriorityAndAlternateThreeToOne() {
        queue.enqueue(10L, PriorityType.NORMAL);
        queue.enqueue(11L, PriorityType.NORMAL);
        for (long id = 1; id <= 6; id++) queue.enqueue(id, PriorityType.URGENT);
        assertThat(queue.size()).isEqualTo(8);
        for (long expected : new long[]{1, 2, 3, 10, 4, 5, 6, 11}) {
            assertThat(queue.dequeue()).isEqualTo(expected);
        }
        assertThat(queue.dequeue()).isNull();
        assertThat(queue.size()).isZero();
    }

    @Test void shouldDrainNormalQueueWhenThereAreNoUrgentMessages() {
        for (long id = 1; id <= 5; id++) queue.enqueue(id, PriorityType.NORMAL);
        for (long id = 1; id <= 5; id++) assertThat(queue.dequeue()).isEqualTo(id);
        assertThat(queue.dequeue()).isNull();
    }

    @Test void shouldKeepDrainingUrgentQueueBeyondQuotaWhenNoNormalIsWaiting() {
        for (long id = 1; id <= 8; id++) queue.enqueue(id, PriorityType.URGENT);
        for (long id = 1; id <= 8; id++) assertThat(queue.dequeue()).isEqualTo(id);
        assertThat(queue.dequeue()).isNull();
    }

    @Test void shouldServeNewNormalAfterQuotaHasAlreadyBeenReached() {
        for (long id = 1; id <= 8; id++) queue.enqueue(id, PriorityType.URGENT);
        for (long id = 1; id <= 5; id++) assertThat(queue.dequeue()).isEqualTo(id);
        queue.enqueue(99L, PriorityType.NORMAL);
        assertThat(queue.dequeue()).isEqualTo(99L);
        assertThat(queue.dequeue()).isEqualTo(6L);
    }

    @Test void shouldResetQuotaAfterObservingEmptyQueue() {
        queue.enqueue(1L, PriorityType.URGENT);
        queue.enqueue(2L, PriorityType.URGENT);
        queue.dequeue(); queue.dequeue();
        assertThat(queue.dequeue()).isNull();
        queue.enqueue(99L, PriorityType.NORMAL);
        for (long id = 3; id <= 6; id++) queue.enqueue(id, PriorityType.URGENT);
        for (long expected : new long[]{3, 4, 5, 99, 6}) {
            assertThat(queue.dequeue()).isEqualTo(expected);
        }
    }

    @Test void shouldNotStarveNormalUnderContinuousUrgentArrivals() {
        queue.enqueue(99L, PriorityType.NORMAL);
        for (long id = 1; id <= 3; id++) {
            queue.enqueue(id, PriorityType.URGENT);
            assertThat(queue.dequeue()).isEqualTo(id);
        }
        queue.enqueue(4L, PriorityType.URGENT);
        assertThat(queue.dequeue()).isEqualTo(99L);
        assertThat(queue.dequeue()).isEqualTo(4L);
    }
}
