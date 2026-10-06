package com.irrah.desafio_tecnico.queue;

import org.springframework.stereotype.Component;

@Component
public class QueueMetrics {

    private Long sent;

    private Long failed;

    public synchronized void registerSuccess(){
        sent++;
    }

    public synchronized void registerFailure(){
        failed++;
    }

    public synchronized Snapshot snapshot(){
        return new Snapshot(sent, failed);
    }


}
