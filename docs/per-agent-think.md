# Per-Agent Think Toggle

## Goal

Allow each agent to resolve its own effective think value per request — boolean-ish (`true`/`false`), concrete level (`high`/`medium`), or omitted. The 🧠 toggle in the action bar sets a session-only preference; per-agent overrides come from AGENT.md frontmatter or provider/model mapping files.

## Business Rules

### R1: Per-agent think resolution ✅
Each agent resolves its own effective think String per request via `ThinkResolver`. The value is computed fresh, never inherited across agents.

- **GIVEN** Peon-Plan has `think: high` in config **WHEN** it makes a request **THEN** the request carries `reasoning.effort=high` (OpenAI) or equivalent for the provider
- **GIVEN** Peon-Dev has think off **WHEN** it makes a request **THEN** no think attribute is sent

### R2: Provider/model mapping in resource files ✅
Auto mode translates generic "on" into provider+model-specific values via `resources/thinking/<PROVIDER>` files. Line format `pattern | on | off`. First match wins; no match → omit.

- **GIVEN** an OpenAI reasoning model matches a pattern **WHEN** auto mode resolves think **THEN** it returns the mapped value (e.g. `high`)
- **GIVEN** an unknown gateway model has no mapping **WHEN** auto mode resolves think **THEN** the attribute is omitted

### R3: think_send is global build-time preference ✅
`sendThinking` / `returnThinking` are langchain4j build-time switches — single global pref. Independent of per-agent think toggle.

- **GIVEN** `think_send` is enabled globally **WHEN** any agent calls a reasoning model **THEN** prior thinking is resent on the next turn

## ADRs
- [ADR-0001](adr/0001-per-agent-think-string.md) — Think resolved to per-agent String
- [ADR-0002](adr/0002-model-mapping-resource-files.md) — Mapping in resource files
- [ADR-0003](adr/0003-send-thinking-independent.md) — think_send independent of toggle

## Non-goals
- Per-agent `think_send` (reserved, blocked by langchain4j build-time limitation)
