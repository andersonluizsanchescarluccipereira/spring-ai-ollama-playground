package br.com.lab.ollama;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service = service; }

    @PostMapping
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest request) {
        return service.chat(request);
    }

    // JSON em cada evento SSE preserva espaços e quebras de linha.
    public record StreamChunk(String text) {}

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamChunk>> stream(@Valid @RequestBody ChatRequest request) {
        return service.stream(request)
                .map(text -> ServerSentEvent.builder(new StreamChunk(text)).event("chunk").build())
                .concatWithValues(ServerSentEvent.builder(new StreamChunk("")).event("done").build())
                .onErrorResume(ex -> Flux.just(ServerSentEvent
                        .builder(new StreamChunk("Falha na geração. Verifique o Ollama e o terminal."))
                        .event("error").build()));
    }
}
