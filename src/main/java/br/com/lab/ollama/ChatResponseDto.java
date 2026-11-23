package br.com.lab.ollama;

public record ChatResponseDto(String answer, long elapsedMs,
                              Integer promptTokens, Integer completionTokens,
                              Integer totalTokens) {}
