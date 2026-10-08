package com.irrah.desafio_tecnico.queue;

import org.springframework.stereotype.Component;

@Component
public class QueueReadiness {

    private volatile boolean ready = false;

    public boolean isReady(){
        return ready;
    }

    public void markReady(){
        this.ready = true;
    }
}
