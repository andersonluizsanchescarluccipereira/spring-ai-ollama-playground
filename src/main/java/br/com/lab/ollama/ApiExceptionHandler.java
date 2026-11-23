package br.com.lab.ollama;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldError();
        return ResponseEntity.badRequest().body(Map.of("error", field == null ? "Dados inválidos" : field.getField() + ": " + field.getDefaultMessage()));
    }

    @ExceptionHandler({org.springframework.web.client.RestClientException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> ollamaFailure(Exception ex) {
        log.warn("Falha na chamada ao modelo", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Falha na geração. Verifique o Ollama e o terminal."));
    }
}
