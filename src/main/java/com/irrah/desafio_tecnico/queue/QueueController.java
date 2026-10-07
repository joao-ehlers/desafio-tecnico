package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.queue.dto.QueueMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fila", description = "Métricas e estado da fila de processamento")
@RequiredArgsConstructor
@RestController
@RequestMapping("/queue")
public class QueueController {
    private final QueueMetrics queueMetrics;
    private final InMemoryMessageQueue inMemoryMessageQueue;

    @Operation(
            summary = "Status da fila",
            description = "size não traz mensagens em processamento e em espera de retry. failed conta apenas falhas definitivas. Os contadores reiniciam com a aplicação."
    )
    @GetMapping("/status")
    public ResponseEntity<QueueMetricsResponse> getQueueStatus() {
        var snapshot = queueMetrics.snapshot();

        return ResponseEntity.ok(
                QueueMetricsResponse.builder()
                        .size(inMemoryMessageQueue.size())
                        .processed(snapshot.processed())
                        .sent(snapshot.sent())
                        .failed(snapshot.failed())
                        .build()
        );
    }
}
