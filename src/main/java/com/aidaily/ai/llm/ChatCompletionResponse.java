package com.aidaily.ai.llm;

import java.util.List;

public class ChatCompletionResponse {

    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public static class Choice {
        private ChatCompletionRequest.ChatMessage message;

        public ChatCompletionRequest.ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatCompletionRequest.ChatMessage message) {
            this.message = message;
        }
    }
}
