package org.interkambio.SistemaInventarioBackend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> buildResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

    /** Maneja errores de validación de @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        String message = "Errores de validación: " + String.join(", ", errors);

        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, message, request),
                HttpStatus.BAD_REQUEST
        );
    }

    /** Maneja cuando una entidad no existe en la BD */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request
    ) {
        return new ResponseEntity<>(
                buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND
        );
    }

    /** Maneja errores de argumentos inválidos (como stock negativo) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST
        );
    }

    /** Maneja cualquier otro error no controlado */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(
            Exception ex,
            WebRequest request
    ) {
        return new ResponseEntity<>(
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
