package com.irrah.desafio_tecnico.queue;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class QueueMetrics {

    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public synchronized void registerSuccess(){
        sent.incrementAndGet();
    }

    public synchronized void registerFailure(){
        failed.incrementAndGet();
    }

    public synchronized Snapshot snapshot(){
        return new Snapshot(sent.get(), failed.get());
    }


}
