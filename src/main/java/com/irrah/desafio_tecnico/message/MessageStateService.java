package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Service
public class MessageStateService {

    private final MessageRepository messageRepository;
    private final Clock clock;

    @Transactional
    public Message startProcessing(Long messageId){
        Message message = findMessage(messageId);
        message.startProcessing();
        return message;
    }

    @Transactional
    public ProcessingResult markAsSent(Long messageId){
        Message message = findMessage(messageId);
        message.markAsSent();
        return ProcessingResult.SENT;
    }

    @Transactional
    public ProcessingResult markAsFailed(Long messageId){
        Message message = findMessage(messageId);
        message.markAsFailed(
                Instant.now(clock),
                3,
                Duration.ofSeconds(5)
        );
        return (message.getNextAttemptAt() == null)
                ? ProcessingResult.FAILED : ProcessingResult.RETRY_SCHEDULED;
    }

    @Transactional
    public Message queueForRetry(Long messageId){
        Message message = findMessage(messageId);
        message.queueForRetry(Instant.now(clock));

        return message;
    }

    private Message findMessage(Long id){
        return messageRepository.findById(id).orElseThrow(MessageNotFoundException::new);
    }
}
