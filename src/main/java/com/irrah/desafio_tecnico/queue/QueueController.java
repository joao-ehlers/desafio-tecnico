package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.queue.dto.QueueMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/queue")
public class QueueController {
    private final QueueMetrics queueMetrics;
    private final InMemoryMessageQueue inMemoryMessageQueue;

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
