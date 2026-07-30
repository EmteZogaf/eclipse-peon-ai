# ADR 0019 — JSONL Agent History Store

Status: Accepted

## Context

Peon keeps active chat memory in process-local `ThreadSafeMemory`. Plan, Dev, and custom agents should survive Eclipse/plugin restarts without introducing a database or replacing the current memory implementation.

LangChain4j provides per-message JSON serializers for `ChatMessage` types.

## Decision

Use a path-scoped `FileAgentHistoryStore(historyFile)` and pass it directly into `ThreadSafeMemory` when history persistence is enabled.

- One file per agent lives under `<configDir>/state/<safe-agent-name>-history.jsonl`.
- One line is one `ChatMessage`, serialized with `ChatMessageSerializer.messageToJson`.
- `ThreadSafeMemory` calls `append(...)` when messages are only added.
- `ThreadSafeMemory` calls `persist(snapshot)` only when memory is replaced, e.g. adjacent user-message merge or `replaceAll`.
- `clear()` deletes the agent file.
- Loading reads non-blank lines in order using `ChatMessageDeserializer.messageFromJson`.
- Corrupt history is logged, deleted, and ignored.
- Save failure is surfaced once; `ThreadSafeMemory` then disables its store for the session so in-memory work can continue.

## Consequences

- No H2/Spring dependency is added.
- No extra persister/supplier/interface indirection is needed.
- Runtime state stays central and separate from custom agent prompt folders.
- Persisted history mirrors current `ThreadSafeMemory`; this is not an immutable audit transcript.
- Token totals are recalculated per session and are not persisted.
