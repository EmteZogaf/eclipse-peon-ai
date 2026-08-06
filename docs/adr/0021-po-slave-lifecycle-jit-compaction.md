# ADR-0021: Peon-PO slave lifecycle & just-in-time compaction

**Status** · Proposed

## Context
Jon's slaves (Peon-Plan, Peon-Dev) carry a real conversation across many `jonAsk*` turns, so they must
**keep their context** between calls — unlike the stateless one-shot SearchAgent. Long-running slaves
will eventually overflow their context window, but compacting eagerly (or on every turn) wastes tokens
and can drop the very instruction Jon is about to send.

## Decision
- **Lazy persistent singletons:** the first Plan-side call (`jonCreateDevPlan` / `jonAskQuestion`) or
  `jonAskDev` call creates the instance; it
  is then kept alive and reused for the whole Jon session, holding its context. SearchAgent stays
  one-shot/stateless. These are **dedicated, Jon-owned instances with their own history files**, not
  the user-selectable Peon-Plan/Peon-Dev from `AgentService` (sharing would corrupt the user's memory/
  history and dead-queue the nested `call()` via the `working` guard).
- **Just-in-time compaction:** a slave is compacted **only immediately before Jon sends it the next
  message** (via `slave.compressContext(monitor)`), and only when its context exceeds a **single
  constant threshold (default 60 %,** to be fine-tuned later). The threshold needs its own explicit
  base — `AbstractAgent.tokenContextUsedInPercent()` caps its denominator at `min(autoCompactAfter,
  4000)`, so that value is too fuzzy to key "60 %" on directly.
- **Standing-order survival:** Jon's outgoing message is delivered to the slave as a **standing order**
  (like a `/` command), so it survives the compaction and is inserted **before** the compact result —
  the slave never compacts away the instruction it is about to act on.

## Consequences
- Exactly two long-lived slave contexts per Jon session → the header can show `peon-plan(context-size)`
  and `peon-dev(context-size)` with a status ball each.
- The 60 % threshold is one named constant, expected to need tuning.
- Reuses the existing standing-orders mechanism ([ADR-0010](0010-standing-orders-setactiveagent-hook.md))
  and compaction tooling rather than inventing a new pre-compaction channel.
