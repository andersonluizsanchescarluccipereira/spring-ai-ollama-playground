package br.com.lab.ollama;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    private ChatClient.ChatClientRequestSpec prepare(ChatRequest request) {
        var options = OllamaOptions.builder()
                .model("qwen2.5:1.5b")
                .temperature(request.temperature())
                .topK(request.topK())
                .topP(request.topP())
                .numPredict(request.maxTokens())
                .numCtx(2048)
                .seed(request.seed() == null ? -1 : request.seed())
                .build();

        String system = request.systemPrompt();
        return chatClient.prompt()
                .system(system == null || system.isBlank()
                        ? "Responda em português do Brasil, de forma objetiva." : system)
                .user(request.prompt())
                .options(options);
    }

    public ChatResponseDto chat(ChatRequest request) {
        long start = System.nanoTime();
        var response = prepare(request).call().chatResponse();
        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("O modelo não retornou uma resposta.");
        }
        var usage = response.getMetadata().getUsage();
        return new ChatResponseDto(response.getResult().getOutput().getText(),
                (System.nanoTime() - start) / 1_000_000,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens());
    }

    public Flux<String> stream(ChatRequest request) {
        return prepare(request).stream().content();
    }
}
