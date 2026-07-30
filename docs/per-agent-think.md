# Per-Agent Think Support

## Goal

Each agent declares whether its model supports thinking/reasoning and resolves its own effective request value from `think_supported`, `think_on_string`, and `think_off_string`. The resolved string is provider-agnostic; `AiProvider` maps it to provider-specific request parameters.

## Business Rules

### R1: Per-agent support resolves to a request value ✅
Each agent resolves its own effective think string per request via `ThinkResolver`. The value is computed fresh and never inherited across agents.

- **GIVEN** Peon-Dev has `thinkSupported=false` and empty on/off strings **WHEN** it makes an Ollama request **THEN** it sends `think:false`
- **GIVEN** Peon-Dev has `thinkSupported=false` and an empty off-string **WHEN** it makes an OpenAI request **THEN** no reasoning attribute is sent
- **GIVEN** Peon-Dev has `thinkSupported=true` and `thinkOnString=medium` **WHEN** it makes an OpenAI request **THEN** it sends `reasoning.effort=medium`

### R2: Dev, Plan, Search, and Compact stay independent ✅
Built-in agents resolve independently from the same `LlmConfig`.

- **GIVEN** Dev has `thinkSupported=false` **AND** Plan has `planThinkSupported=true` with `planThinkOnString=high` **WHEN** both configs are inspected **THEN** Dev resolves to `""` and Plan resolves to `"high"`
- **GIVEN** Compact or Search uses Ollama **WHEN** request parameters are built **THEN** `think` is unset (`null`), not `false`

### R3: Provider/model mapping in resource files ✅
Auto mode translates generic `true` into provider+model-specific values via `resources/thinking/<PROVIDER>` files. Line format `pattern | on | off`. First match wins; no match → omit for providers without explicit off behavior.

- **GIVEN** an OpenAI reasoning model matches a pattern **WHEN** auto mode resolves think **THEN** it returns the mapped value (e.g. `high`)
- **GIVEN** an unknown gateway model has no mapping **WHEN** auto mode resolves think **THEN** the reasoning attribute is omitted

### R4: Custom agents use canonical frontmatter ✅
Custom agents use `think_supported`, `think_on_string`, and `think_off_string`. Legacy `think_enabled` and `think` remain read-compatible and migrate on write.

- **GIVEN** `AGENT.md` has `think_supported: true` and `think_on_string: high` **WHEN** the custom agent config is read **THEN** `isThinkSupported()` is true and the resolved think value is `high`
- **GIVEN** `AGENT.md` has legacy `think_enabled: true` **WHEN** a write operation occurs **THEN** the file contains `think_supported: true` and no `think_enabled`

### R5: send-thinking transport is global ✅
`sendThinking` / `returnThinking` are langchain4j build-time switches — one global preference. They are independent of per-agent think support.

- **GIVEN** `thinkSupported=false` and global send-thinking is enabled **WHEN** config is inspected **THEN** prior thinking is still resent while request-level reasoning follows provider mapping

## Provider Semantics

- `thinkSupported=true` with empty on/off strings resolves to generic `"true"` and can use provider/model mapping.
- `thinkSupported=false` with empty on/off strings resolves to `""`.
- Ollama maps resolved off/empty to `think:false`; only unset `null` omits `think`.
- OpenAI/Anthropic omit reasoning for off/empty unless a provider-specific explicit off is configured.
- LM Studio sends explicit non-empty off values such as `false` as custom `reasoning=off`.

## ADRs
- [ADR-0001](adr/0001-per-agent-think-string.md) — Think resolved to per-agent String
- [ADR-0002](adr/0002-model-mapping-resource-files.md) — Mapping in resource files
- [ADR-0003](adr/0003-send-thinking-independent.md) — send-thinking independent of support

## Non-goals
- Per-agent send-thinking transport (blocked by langchain4j build-time limitation)
- Separate persisted runtime on/off state beyond the current support boolean
