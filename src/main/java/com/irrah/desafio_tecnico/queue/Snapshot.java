package com.irrah.desafio_tecnico.queue;

public record Snapshot(Long sent, Long failed){
    public Long processed(){
        return sent + failed;
    }
}