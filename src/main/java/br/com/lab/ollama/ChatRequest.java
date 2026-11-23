package br.com.lab.ollama;

import jakarta.validation.constraints.*;

public record ChatRequest(
    @NotBlank @Size(max = 3000) String prompt,
    @NotNull @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
    @NotNull @Min(1) @Max(100) Integer topK,
    @NotNull @DecimalMin("0.01") @DecimalMax("1.0") Double topP,
    @NotNull @Min(1) @Max(512) Integer maxTokens,
    @Min(-1) Integer seed,
    @Size(max = 1000) String systemPrompt
) {}
