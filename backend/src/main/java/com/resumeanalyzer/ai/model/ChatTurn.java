package com.resumeanalyzer.ai.model;

/** A single conversation turn passed to the AI provider for context. */
public record ChatTurn(Role role, String content) {

    public enum Role { SYSTEM, ASSISTANT, USER }

    public static ChatTurn system(String content) {
        return new ChatTurn(Role.SYSTEM, content);
    }

    public static ChatTurn assistant(String content) {
        return new ChatTurn(Role.ASSISTANT, content);
    }

    public static ChatTurn user(String content) {
        return new ChatTurn(Role.USER, content);
    }
}
