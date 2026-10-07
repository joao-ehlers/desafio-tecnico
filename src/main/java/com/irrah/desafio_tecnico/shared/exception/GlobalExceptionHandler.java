package com.irrah.desafio_tecnico.shared.exception;

import com.irrah.desafio_tecnico.billing.exception.ExceedingValueException;
import com.irrah.desafio_tecnico.billing.exception.InsufficientFundsException;
import com.irrah.desafio_tecnico.billing.exception.WrongConsumingMonthException;
import com.irrah.desafio_tecnico.client.exception.*;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.conversation.exception.RecipientNotFoundException;
import com.irrah.desafio_tecnico.message.exception.InvalidMessageStateException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail handleClientNotFound(
            ClientNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Cliente não encontrado",
                "Não existe cliente para o identificador informado."
        );
    }

    @ExceptionHandler(InactiveClientException.class)
    public ProblemDetail handleInactiveClient(
            InactiveClientException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Cliente inativo",
                "O cliente está inativo e não pode executar esta operação."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Um ou mais campos da requisição são inválidos."
        );

        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(
            HttpMessageNotReadableException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido",
                "Verifique a sintaxe do JSON, os tipos dos campos "
                        + "e os valores dos enums."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Parâmetro inválido",
                "O parâmetro '" + exception.getName()
                        + "' possui um formato inválido."
        );
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ProblemDetail handleDuplicateDocument(
            DuplicateDocumentException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Documento já cadastrado",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail handleConversationNotFound(
            ConversationNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Conversa não encontrada",
                "A conversa não existe ou não pertence ao cliente informado."
        );
    }

    @ExceptionHandler(RecipientNotFoundException.class)
    public ProblemDetail handleRecipientNotFound(
            RecipientNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Destinatário não encontrado",
                "Não existe destinatário para o identificador informado."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(
            DataIntegrityViolationException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "A operação viola uma restrição de integridade dos dados."
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(
            OptimisticLockingFailureException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflito de atualização",
                "O registro foi alterado por outra operação. "
                        + "Consulte os dados novamente antes de tentar."
        );
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInput(
            InvalidInputException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Dados inválidos");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({
            InsufficientFundsException.class,
            InvalidPlanOperationException.class,
            InvalidMessageStateException.class,
            ExceedingValueException.class,
            WrongConsumingMonthException.class,
            ClientNotActiveException.class
    })
    public ResponseEntity<ProblemDetail> handleBusinessConflict(
            RuntimeException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Operação não permitida no estado atual");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problem.setTitle(title);
        return problem;
    }

    public record FieldValidationError(
            String field,
            String message
    ) {}
}