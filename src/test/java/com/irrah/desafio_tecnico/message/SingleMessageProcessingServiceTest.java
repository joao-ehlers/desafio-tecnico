package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SingleMessageProcessingServiceTest {
    @Mock MessageStateService stateService;
    @Mock MessageSender sender;
    @Mock Message message;
    private SingleMessageProcessingService service;

    @BeforeEach void setUp() {
        service = new SingleMessageProcessingService(sender, stateService);
    }

    @Test void shouldSendBetweenStartAndSuccessTransitions() {
        when(stateService.startProcessing(42L)).thenReturn(message);
        when(message.getId()).thenReturn(42L);
        when(stateService.markAsSent(42L)).thenReturn(ProcessingResult.SENT);
        assertThat(service.process(42L)).isEqualTo(ProcessingResult.SENT);
        var order = inOrder(stateService, sender);
        order.verify(stateService).startProcessing(42L);
        order.verify(sender).sendMessage(message);
        order.verify(stateService).markAsSent(42L);
        verify(stateService, never()).markAsFailed(anyLong());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessingResult.class, names = {"RETRY_SCHEDULED", "FAILED"})
    void shouldReturnFailureDecisionFromStateService(ProcessingResult result) {
        when(stateService.startProcessing(42L)).thenReturn(message);
        when(message.getId()).thenReturn(42L);
        doThrow(new MessageDeliveryException()).when(sender).sendMessage(message);
        when(stateService.markAsFailed(42L)).thenReturn(result);
        assertThat(service.process(42L)).isEqualTo(result);
        verify(stateService).markAsFailed(42L);
        verify(stateService, never()).markAsSent(anyLong());
    }

    @Test void shouldNotSendWhenStartingProcessingFails() {
        when(stateService.startProcessing(42L)).thenThrow(new MessageNotFoundException());
        assertThatThrownBy(() -> service.process(42L)).isInstanceOf(MessageNotFoundException.class);
        verifyNoInteractions(sender);
        verify(stateService, never()).markAsSent(anyLong());
        verify(stateService, never()).markAsFailed(anyLong());
    }

    @Test void shouldPropagateUnexpectedSenderFailureWithoutTreatingItAsDeliveryFailure() {
        when(stateService.startProcessing(42L)).thenReturn(message);
        var failure = new IllegalStateException("unexpected sender defect");
        doThrow(failure).when(sender).sendMessage(message);
        assertThatThrownBy(() -> service.process(42L)).isSameAs(failure);
        verify(stateService, never()).markAsFailed(anyLong());
        verify(stateService, never()).markAsSent(anyLong());
    }

    @Test void shouldNotReportSuccessWhenFinalPersistenceFails() {
        when(stateService.startProcessing(42L)).thenReturn(message);
        when(message.getId()).thenReturn(42L);
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(stateService.markAsSent(42L)).thenThrow(failure);
        assertThatThrownBy(() -> service.process(42L)).isSameAs(failure);
        verify(sender, times(1)).sendMessage(message);
        verify(stateService, never()).markAsFailed(anyLong());
    }
}
