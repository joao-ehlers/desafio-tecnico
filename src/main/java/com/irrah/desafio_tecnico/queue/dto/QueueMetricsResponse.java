package com.irrah.desafio_tecnico.queue.dto;

import lombok.Builder;

@Builder
public record QueueMetricsResponse(
        int size,
        Long processed,
        Long sent,
        Long failed
){
}
