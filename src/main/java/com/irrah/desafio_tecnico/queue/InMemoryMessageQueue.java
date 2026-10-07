package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.PriorityType;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Component
public class InMemoryMessageQueue {

    private final Queue<Long> normalQueue = new ArrayDeque<>();
    private final Queue<Long> urgentQueue = new ArrayDeque<>();
    private final Set<Long> queuedIds = new HashSet<>();

    private int consecutiveUrgent = 0;
    private final int MAX_CONSECUTIVE_URGENT = 3;

    public synchronized void enqueue(Long messageId, PriorityType priorityType){
        if(messageId == null){
            throw new InvalidInputException("O ID da mensagem é obrigatorio");
        }
        if(priorityType == null){
            throw new InvalidInputException("A prioridade da mensagem é obrigatoria");
        }

        if(!queuedIds.add(messageId)){
            return;
        }

        switch (priorityType){
            case URGENT -> urgentQueue.offer(messageId);
            case NORMAL -> normalQueue.offer(messageId);
        }
    }

    public synchronized Long dequeue(){
        Long messageId;

        if(urgentQueue.isEmpty() && normalQueue.isEmpty()){
            consecutiveUrgent = 0;
            return null;
        }

        if(!urgentQueue.isEmpty() && (this.consecutiveUrgent < this.MAX_CONSECUTIVE_URGENT || normalQueue.isEmpty())) {
            consecutiveUrgent = Math.min(
                    consecutiveUrgent + 1,
                    MAX_CONSECUTIVE_URGENT
            );
            messageId = urgentQueue.poll();
            queuedIds.remove(messageId);
            return messageId;
        }

        this.consecutiveUrgent = 0;
        messageId = normalQueue.poll();
        return messageId;
    }

    public synchronized int size(){
        return normalQueue.size() + urgentQueue.size();
    }
}
