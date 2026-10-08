package com.irrah.desafio_tecnico.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class QueueInitializer implements ApplicationRunner {
    private final QueueReadiness readiness;
    private final QueueRecoveryService recoveryService;

    @Override
    public void run(ApplicationArguments arguments){
        recoveryService.recoverQueuedMessages();

        readiness.markReady();
    }
}
