package org.sterl.llmpeon.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sterl.llmpeon.shared.ChatMessageUtil;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

class ThreadSafeMemoryTest {

    @Test
    void testExtractLastOf() {
        // GIVEN
        var subject = new ThreadSafeMemory();
        
        subject.add(UserMessage.from("Foo"));
        subject.add(AiMessage.from("bar"));
        subject.add(UserMessage.from("Build Plan"));
        subject.add(AiMessage.from("Your plan"));
        
        // WHEN
        var plan = subject.getLastOf(AiMessage.class);
        
        // THEN
        assertThat(plan.text()).isEqualTo("Your plan");
    }
    
    @Test
    void testJoinUserMessage() {
        // GIVEN
        var subject = new ThreadSafeMemory();
        
        subject.add(AiMessage.from("1"));
        subject.add(UserMessage.from("Foo"));
        subject.add(AiMessage.from("2"));
        subject.add(UserMessage.from("Bar"));
        subject.add(AiMessage.from("3"));
        
        // WHEN
        subject.add(UserMessage.from("U1"));
        subject.add(UserMessage.from("U2"));
        
        // THEN
        assertThat(subject.size()).isEqualTo(6);
        var messages = subject.getCopy();
        // AND
        assertThat(ChatMessageUtil.toString(messages.getLast())).contains("U1", "U2");
        // AND
        assertThat(subject.messageFlow()).isEqualTo("AI->USER->AI->USER->AI->USER");
    }
    
    
    @Test
    void test_trailing_tool_result() {
        // GIVEN
        var subject = new ThreadSafeMemory();
        
        subject.add(UserMessage.from("Foo"));
        subject.add(AiMessage.from(ToolExecutionRequest.builder().id("1").name("foo").build()));
        subject.add(ToolExecutionResultMessage.from("1", "foo", "bar"));
        
        // WHEN
        subject.add(UserMessage.from("U1"));
        
        // THEN
        assertThat(subject.size()).isEqualTo(5);
        var messages = subject.getCopy();
        // AND
        assertThat(ChatMessageUtil.toString(messages.getLast())).contains("U1");
        // AND
        assertThat(subject.messageFlow()).isEqualTo("USER->TOOL_REQUEST->TOOL_EXECUTION_RESULT->AI->USER");
    }

    @Test
    void storeReceivesAppendPersistAndClearOperations() {
        // GIVEN
        var store = new RecordingStore();
        var subject = new ThreadSafeMemory(store);

        // WHEN
        subject.add(AiMessage.from("simple"));

        // THEN
        assertThat(store.operations).containsExactly("append:AI");

        // WHEN
        subject.add(UserMessage.from("U1"));
        subject.add(UserMessage.from("U2"));

        // THEN
        assertThat(store.operations).containsExactly("append:AI", "append:USER", "persist:2");

        // WHEN
        var toolResult = ToolExecutionResultMessage.from("1", "tool", "result");
        subject.addResult(ChatResponse.builder().aiMessage(AiMessage.from("answer")).build(), List.of(toolResult));

        // THEN
        assertThat(store.operations).containsExactly(
                "append:AI", "append:USER", "persist:2", "appendList:2");

        // WHEN
        subject.clear();

        // THEN
        assertThat(store.operations).endsWith("clear");
    }

    @Test
    void replaceAllRestoresExactMessageListWithoutMerge() {
        // GIVEN
        var subject = new ThreadSafeMemory();
        var messages = List.<ChatMessage>of(UserMessage.from("U1"), UserMessage.from("U2"));

        // WHEN
        subject.replaceAll(messages);

        // THEN
        assertThat(subject.getCopy()).hasSize(2);
        assertThat(ChatMessageUtil.toString(subject.get(0))).contains("U1");
        assertThat(ChatMessageUtil.toString(subject.get(1))).contains("U2");
        assertThat(subject.messageFlow()).isEqualTo("USER->USER");
    }

    private static class RecordingStore extends FileAgentHistoryStore {
        final List<String> operations = new ArrayList<>();

        RecordingStore() {
            super(Path.of("/unused"));
        }

        @Override
        public void append(ChatMessage message) {
            operations.add("append:" + message.type().name());
        }

        @Override
        public void append(List<ChatMessage> messages) {
            operations.add("appendList:" + messages.size());
        }

        @Override
        public void persist(List<ChatMessage> snapshot) {
            operations.add("persist:" + snapshot.size());
        }

        @Override
        public void clear() {
            operations.add("clear");
        }
    }
}
