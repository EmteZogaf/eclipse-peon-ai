# ADR 0003 — send-thinking is independent of think support and stays global

**Status** · Accepted

**Context:** Some models emit reasoning even when we send no think/reasoning attribute, and require that reasoning to be returned and resent on the next turn. This show+resend concern is separate from whether an agent's model supports thinking. Langchain4j exposes `returnThinking`/`sendThinking` only on the model builder, not per request.

**Decision:**
- `returnThinking` (parse + show) = `think_supported` **OR** global send-thinking.
- `sendThinking` (resend prior thinking) = global send-thinking.
- The send-thinking preference remains a single global switch (`PREF_SEND_THINKING_ENABLED`).

**TODO:** Remove `think_enabled` backward compatibility (kept in `CustomAgent.THINK_ENABLED` for old AGENT.md files) in a future major version. Auto-migration happens on the first write operation; files are not modified on load.

**Consequences:** A reasoning model may think and have its thinking shown even when no request attribute is sent. Per-agent send-thinking transport is not wired per request.
