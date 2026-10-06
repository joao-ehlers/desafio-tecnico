package com.irrah.desafio_tecnico.queue;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Queue;

@Component
public class InMemoryMessageQueue {

    private final Queue<Long> pendingMessages = new ArrayDeque<>();

    public synchronized void enqueue(Long messageId){
        if(messageId == null){
            throw new IllegalArgumentException("O ID da mensagem é obrigatorio");
        }

        pendingMessages.offer(messageId);
    }

    public synchronized Long dequeue(){
        return pendingMessages.poll();
    }

    public synchronized int size(){
        return pendingMessages.size();
    }
}
