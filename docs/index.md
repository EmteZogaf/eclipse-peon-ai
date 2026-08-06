# Docs — design & dev spec (the HOW / system reference)

The `docs/` tree is our shared memory: one story per feature (business rules + BDD). Your technical
notes live in [adr/](adr/index.md). Not published — user-facing docs are in `homepage/`.

## Stories

* [Disk File Write Tool](disk-file-write-tool.md) - real filesystem write/edit tools, configurable workingDir, disabled by default.
* [Eclipse Workspace Write Tool](eclipse-workspace-write-file-tool.md) - Eclipse VFS write/edit tools, project-scoped sandbox, always available.
* [Advanced Configuration](advanced-configuration.md) - the two-page preference split and per-agent model resolution via `ChatRequest.modelName()`.
* [Custom Agents](custom-agents-design.md) - user-defined `AGENT.md` agents with tool allowlists, read-only mode and per-agent model.
* [Interaction Design](interaction-design.md) - the chat view layout: history, input block, action bar and status line.
* [Plan & Dev Agent](plan-dev-agent-design.md) - the two-phase plan→dev handoff model and its planned pipeline features.
* [Model Loading](model-loading.md) - model list lifecycle: lazy fetch, persistence across agent switches, fallback on failure.
* [Per-Agent Think Support](per-agent-think.md) - per-agent thinking support and request-value resolution via provider mapping files and AGENT.md frontmatter.
* [Queued User Messages](queued-user-messages.md) - input queue with batching, FIFO consumption, drain-to-memory on abort.
* [Session Token Usage](token-usage.md) - cumulative ↑/↓ token spend in the header, fed from the StreamingBridge choke point.
* [Scaffold Agent](scaffold-agent.md) - built-in agent for creating/editing Peon config artifacts (agents, skills, commands) with config-scoped disk tools.
* [Standing Orders](standing-orders-design.md) - context lines (project, AGENTS.md, active command/skill) that survive mid-loop compaction.
* [AGENTS.md Support](agents-md-support.md) - base AGENTS.md loading: purpose, file name resolution, toggle.
* [Agent-Specific AGENTS-<agent>.md](agent-specific-agentsmd.md) - AGENTS-<agent>.md: agent name resolution, case-insensitive fallback, deduplication.
* [SWT Integrated Input Buttons](swt-integrated-input-buttons.md) - flat icon buttons beside a `StyledText` that read as one white field on macOS + Windows.
* [Ask User Tool](user-question-tool-design.md) - the LLM pausing mid-task to ask a clarifying question inline in the chat.
* [Persistent Agent History](persistent-agent-history.md) - JSONL chat history persistence for Dev, Plan and custom agents.
* [Streaming Response Display](streaming-display.md) - status-bar overlay with bounded live preview, single DOM insert on completion, no incremental chat rendering.
* [Peon-PO (Jon)](po-agent-jon.md) - docs-owning business-owner agent that designs features and orchestrates its own Peon-Plan/Peon-Dev via jonCreateDevPlan/jonAskQuestion/jonAskDev with planComplete/planImplemented completion signals.

## Notes

* [ADRs](adr/index.md) - technical decision records (the agent's long-term memory).
