package org.sterl.llmpeon.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.llmpeon.AbstractMemoryFileTest;
import org.sterl.llmpeon.shared.ChatMessageUtil;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

class FileAgentHistoryStoreTest extends AbstractMemoryFileTest {

    private Path configDir;

    @BeforeEach
    void before() throws IOException {
        configDir = fs.getPath("/" + UUID.randomUUID());
        Files.createDirectory(configDir);
    }

    @Test
    void loadReturnsMessagesWrittenAsSingleAgentFile() throws IOException {
        // GIVEN
        var file = configDir.resolve("state/Peon-Dev-history.jsonl");
        var subject = new FileAgentHistoryStore(file);
        var messages = List.<ChatMessage>of(UserMessage.from("hello"), AiMessage.from("world"));

        // WHEN
        subject.persist(messages);
        var loaded = new FileAgentHistoryStore(file).load();

        // THEN
        assertThat(loaded).hasSize(2);
        assertThat(ChatMessageUtil.toString(loaded.get(0))).contains("hello");
        assertThat(((AiMessage) loaded.get(1)).text()).isEqualTo("world");
        assertThat(Files.isRegularFile(file)).isTrue();
    }

    @Test
    void persistReplacesJsonlToMatchCurrentMemory() throws IOException {
        // GIVEN
        var file = configDir.resolve("state/Docs_Assistant-history.jsonl");
        var subject = new FileAgentHistoryStore(file);
        subject.persist(List.of(UserMessage.from("1"), AiMessage.from("2"), UserMessage.from("3"), AiMessage.from("4")));

        // WHEN
        subject.persist(List.of(UserMessage.from("summary"), AiMessage.from("done")));

        // THEN
        assertThat(Files.readAllLines(file)).hasSize(2);
        var loaded = subject.load();
        assertThat(loaded).hasSize(2);
        assertThat(ChatMessageUtil.toString(loaded.get(0))).contains("summary");
        assertThat(((AiMessage) loaded.get(1)).text()).isEqualTo("done");
    }

    @Test
    void userMessageMergePersistsJsonlWithoutStalePreMergeLine() throws IOException {
        // GIVEN
        var store = new FileAgentHistoryStore(configDir.resolve("state/Peon-Dev-history.jsonl"));
        store.persist(List.of(AiMessage.from("A"), UserMessage.from("U1")));
        var memory = new ThreadSafeMemory(store);

        // WHEN
        memory.add(UserMessage.from("U2"));

        // THEN
        assertThat(Files.readAllLines(store.historyFile())).hasSize(2);
        var loaded = store.load();
        assertThat(loaded).hasSize(2);
        assertThat(((AiMessage) loaded.get(0)).text()).isEqualTo("A");
        assertThat(ChatMessageUtil.toString(loaded.get(1))).contains("U1", "U2");
    }

    @Test
    void trailingToolRepairAppendsOnlyNewRepairMessages() throws IOException {
        // GIVEN
        var store = new FileAgentHistoryStore(configDir.resolve("state/Peon-Dev-history.jsonl"));
        store.persist(List.of(
                UserMessage.from("Foo"),
                AiMessage.from(ToolExecutionRequest.builder().id("1").name("foo").build()),
                ToolExecutionResultMessage.from("1", "foo", "bar")));
        var memory = new ThreadSafeMemory(store);

        // WHEN
        memory.add(UserMessage.from("U1"));

        // THEN
        assertThat(Files.readAllLines(store.historyFile())).hasSize(5);
        assertThat(store.load()).hasSize(5);
    }

    @Test
    void roundTripsToolExecutionMessages() throws IOException {
        // GIVEN
        var store = new FileAgentHistoryStore(configDir.resolve("state/Peon-Dev-history.jsonl"));
        var toolRequest = ToolExecutionRequest.builder().id("1").name("lookup").arguments("{}").build();
        var messages = List.<ChatMessage>of(
                UserMessage.from("call tool"),
                AiMessage.from(toolRequest),
                ToolExecutionResultMessage.from("1", "lookup", "result"));

        // WHEN
        store.persist(messages);
        var loaded = store.load();

        // THEN
        assertThat(loaded).hasSize(3);
        var memory = new ThreadSafeMemory();
        memory.replaceAll(loaded);
        assertThat(memory.messageFlow()).isEqualTo("USER->TOOL_REQUEST->TOOL_EXECUTION_RESULT");
    }

    @Test
    void corruptHistoryIsDeletedAndReturnsEmptyHistory() throws IOException {
        // GIVEN
        var store = new FileAgentHistoryStore(configDir.resolve("state/Peon-Dev-history.jsonl"));
        Files.createDirectories(store.historyFile().getParent());
        Files.writeString(store.historyFile(), "not-json");

        // WHEN
        var loaded = store.load();

        // THEN
        assertThat(loaded).isEmpty();
        assertThat(Files.exists(store.historyFile())).isFalse();
    }

    @Test
    void firstAppendFailureDisablesFurtherPersistenceAndThrows() throws IOException {
        // GIVEN
        var store = new FailingStore(configDir.resolve("state/Peon-Dev-history.jsonl"));
        store.persist(List.of(UserMessage.from("old")));
        store.failAppend = true;
        var memory = new ThreadSafeMemory(store);

        // WHEN / THEN
        assertThatThrownBy(() -> memory.add(AiMessage.from("new"))).isInstanceOf(RuntimeException.class);
        memory.add(AiMessage.from("ignored"));
        assertThat(store.load()).hasSize(1);
    }

    private static class FailingStore extends FileAgentHistoryStore {
        boolean failAppend;

        FailingStore(Path historyFile) {
            super(historyFile);
        }

        @Override
        public void append(List<ChatMessage> messages) throws IOException {
            if (failAppend) throw new IOException("append failed");
            super.append(messages);
        }
    }
}
