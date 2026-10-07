package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageRetryService {
    private final MessageRepository messageRepository;
    private final MessageStateService messageStateService;
    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final Clock clock;

    public void enqueueDueRetries(){
        List<Message> messagesDue = messageRepository.findAllFailedAndNextAttemptEqualOrLessThan(Instant.now(clock),
                StatusType.FAILED,
                PageRequest.of(0, 100));

        for(Message messageLoop : messagesDue){
            Message message = messageStateService.queueForRetry(messageLoop.getId());

            inMemoryMessageQueue.enqueue(message.getId(), message.getPriority());
        }
    }
}
