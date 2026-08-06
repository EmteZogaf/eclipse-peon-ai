# Peon-PO (Jon)

## Goal

A docs-owning **business-owner agent** — identity **"Jon"**, shown as **`Peon-PO`** in the agent
dropdown. Jon designs features together with the user directly in `docs/`, then drives their
implementation by orchestrating his **own** Peon-Plan and Peon-Dev instances through `jon*` tools.
He owns the WHAT (the docs); the plan and the code are delegated work he never touches himself.

Jon is a **skeptical, critical guardian of the docs**: he keeps them coherent ("round"), always
representing the **SOLL** and clearly separated from the **IST**, and uses Plan/Dev for the heavy
lifting. A question he cannot answer from the docs he **escalates to the user** rather than guessing
(R13).

This story is the design; nothing is built yet, so every rule is **❌**.

**100 % additive.** Peon-PO only *adds* — a new agent, the `jon*` tools, the slave-side completion
signals (`planComplete` / `planImplemented`) and the write-allowlist decorator. It changes **nothing** in the standalone Peon-Plan / Peon-Dev /
Peon-Scaffold agents or in today's button handoff. Jon lives in the **`core`** module and is therefore
**fully testable in core** with the headless disk tools; the Eclipse plugin only **injects the
Eclipse-workspace tools** (behind the same write wrapper). Jon **never gets a shell** — in any layer.

**Status legend:** **✅ done** (slice ships with a green test) · **🚧 WIP** (in progress / partially
built) · **❌ not started**. Jon applies the same incremental discipline to every feature he takes on:
capture it, mark rules ❌, move them to 🚧 while building, flip to ✅ when green (R5).

## Business Rules

### R1: Registration & Naming ❌
Built-in agent alongside Peon-Dev, Peon-Plan and Peon-Scaffold. **The default entry agent stays
Peon-Dev** — Peon-PO is opt-in.

- Dropdown / selection name: `Peon-PO`
- Identity in the system prompt **and** in the docs: `Jon`
- Lives in the **`core`** module: package `org.sterl.llmpeon.po`, class `AiPoAgent extends
  AbstractAgent` — fully unit-testable in core (headless disk tools, no Eclipse runtime)
- Registered via `addPersistentAgent()` — survives `clearAgents()` on reload (same as Peon-Scaffold)
- Auto-loads **`AGENTS-PO.md`** (if present) as its role context — same layering as Plan/Dev loading
  `AGENTS-PLAN.md` / `AGENTS-DEV.md`; the base `AGENTS.md` lists this layer so it is discoverable

**BDD:**
```
GIVEN AgentService is constructed with the default agents
WHEN getAgents() is called
THEN Peon-PO is in the list alongside Peon-Dev, Peon-Plan and Peon-Scaffold
AND the default active agent is still Peon-Dev
```

### R2: ToolService — reuse existing file tools behind a write-allowlist ❌
Jon has his own `ToolService(false)` (no default-tool leakage, like Peon-Scaffold) holding:

- `jonCreateDevPlan`, `jonAskQuestion` — both drive the **same** persistent Peon-Plan slave (R9),
  distinct tool names for distinct intent: `jonCreateDevPlan` runs the full planning workflow (the plan
  Dev will implement, ends when the Plan slave calls `planComplete()`); `jonAskQuestion` sends a
  **direct question** (`Question: <text>. Just directly respond.`) and returns the answer with **no**
  completion signal.
- `jonAskDev` — drive the Peon-Dev slave for one turn; the reply is the tool result.
- `jonAskScaffold` — ask Peon-Scaffold to create/edit a Skill for Jon.
- `SearchAgentTool` — one-shot, **stateless** discovery (unchanged). Complements `jonAskQuestion`,
  which reuses the Plan slave's **warm** project context instead of starting cold.
- **Injected** file tools — Jon gets **no bespoke docs tools**: the **disk** read/write tools in core
  (and tests), the **Eclipse-workspace** read/write tools in the plugin. Each **write** tool is wrapped
  in the **write-path-allowlist decorator** (R3); reads pass through.

No plan* tools, no compact tool on Jon himself, and **no shell in any layer**; writing is bounded by
the allowlist, not by a custom tool.

**BDD:**
```
GIVEN the user switches to Peon-PO
THEN the ToolService has jonCreateDevPlan, jonAskQuestion, jonAskDev, jonAskScaffold, SearchAgentTool and the standard write tools wrapped in the write-allowlist decorator
AND no plan*, shell or compact tools are available
```

### R3: Docs ownership via a write-path allowlist ❌
Jon writes **only** where a configurable allowlist permits — his docs are his single source of truth,
kept coherent and always expressing the SOLL vs. the IST. He treats plan/task artefacts as delegated
work he does not own. **Reading is not gated** — he must see the IST (code included) to keep the docs
honest.

**Write-allowlist decorator + config** (see [ADR-0022](adr/0022-write-path-allowlist-decorator.md)):
the existing Eclipse-workspace and disk write tools are wrapped in a decorator that matches every
target path against a **comma-separated glob list** from config. The list is a **user-editable config
field** (visible and changeable in the settings UI), **preloaded with `*/docs/*`**. Patterns combine as
OR; a write matching none is rejected. The underlying write tool still auto-creates missing sub-paths,
so `docs/` appears on the first write. Semantics:

- `*/docs/*` (default) — a `docs/` folder at **any depth**. In Eclipse the glob is matched against the
  **project-root-relative** path (`<project-name>/docs/...`, the workspace VFS), not a filesystem root;
  for disk tools against the working-dir / given path.
- `docs/*` — only a `docs/` folder **at the (project) root**; the leading position is the sole
  difference from `*/docs/*`.
- `*.md` — any Markdown file, anywhere.

**BDD:**
```
GIVEN the default allowlist */docs/* and a project without a docs/ directory
WHEN Jon writes his first story
THEN the write is allowed and docs/ is created with the file inside it

GIVEN the allowlist */docs/*
WHEN Jon attempts to write a path outside any docs/ folder
THEN the decorator rejects the write

GIVEN the allowlist docs/* (root-only)
WHEN Jon attempts to write <project>/sub/docs/x.md
THEN the decorator rejects it, because docs/ is not at the project root
```

### R4: Onboarding tutorial ❌
On the first activation in a session (`memory.size == 0`) Jon shows a short tutorial message (like
Peon-Scaffold). Later activations in the same session only refresh standing orders.

**BDD:**
```
GIVEN Peon-PO is activated for the first time in a session (memory.size == 0)
THEN a short tutorial message appears in the chat history

GIVEN Peon-PO already has chat history
WHEN the user switches away and back
THEN no tutorial is shown again
```

### R5: Design → approval gate, incremental status ❌
Jon designs and discusses the feature **in the docs** first. He captures a new feature as a story
(goal + business rules + BDD), marks every not-yet-built rule **❌ (WIP)**, and only **after the user
is satisfied** asks *"Shall I implement this?"* before delegating a build to Plan or Dev. (Asking the
Plan agent a **question** via `jonAskQuestion` is design work, not a build — it needs no gate.) When an
implemented slice is confirmed green, Jon flips its rules **❌ → ✅**.

**BDD:**
```
GIVEN the user and Jon are still designing a feature
WHEN Jon has open design questions
THEN Jon keeps refining the story in docs and does NOT delegate to Plan or Dev

GIVEN the user confirms the design is good
THEN Jon asks whether to implement it before calling jonCreateDevPlan

GIVEN a delegated slice returns green and is accepted
THEN Jon flips the affected rules from ❌ to ✅ in the story
```

### R6: Delegate to Plan — `jonCreateDevPlan` (build) & `jonAskQuestion` (Q&A) ❌
Both tools drive the **same** persistent Peon-Plan slave (R9), lazily created on first use and run for
**one turn** via `slave.call(prompt, monitor)` (modeled on `SearchAgentTool`, but against the
persistent slave — so its memory, auto-compact and standing orders are reused). They differ only in
intent and framing:

- **`jonCreateDevPlan`** — the full planning workflow. The Plan agent interviews **Jon** in place of
  the user: each Plan question comes back as the tool result, Jon answers with the next
  `jonCreateDevPlan` call (a question Jon can't decide → escalate, R13). When the plan is ready the Plan
  agent calls **`planComplete()`** (R8) — Jon's done-marker, carrying the plan link — and Jon **reviews**
  it, then either releases it or sends change requests back via `jonCreateDevPlan`.
- **`jonAskQuestion`** — a direct question to the Plan agent, wrapped as
  `Question: <text>. Just directly respond.` The agent answers **directly** — no interview, no
  completion signal, no handover — and the answer is the tool result. Same warm context as the planning
  turns, so Jon can sanity-check SOLL-vs-IST without kicking off a build.

**BDD:**
```
GIVEN Jon calls jonCreateDevPlan for an approved feature
WHEN the Plan agent asks a clarifying question
THEN the question is returned to Jon as the tool result and Jon answers with the next jonCreateDevPlan call

GIVEN the Plan agent calls planComplete()
THEN Jon gets the done-marker with the plan link and reviews the plan
AND Jon either releases it or returns change requests via jonCreateDevPlan

GIVEN Jon calls jonAskQuestion with "Question: <text>. Just directly respond."
THEN the Plan agent answers directly, does NOT call planComplete(), and the answer is the tool result
```

### R7: Delegate to Dev via `jonAskDev` ❌
After releasing the plan Jon calls `jonAskDev`. The Dev agent receives the **plan file path via a
standing order** (reusing today's `onHandoff` `_handoffLine` mechanism) plus the instruction to call
**`planImplemented()`** when finished (R8). On `planImplemented()` Jon has his done-marker (plan flipped
to *done*) and reviews the result, then either accepts it (flip ❌ → ✅, R5) or returns change requests
via `jonAskDev`.

**Post-dev review of larger plans is delegated, not done by Jon himself:** after a completed dev cycle
Jon charges the **Plan agent** with a review + **gap analysis** (built code vs. plan & docs) — via
`jonAskQuestion` for a pure gap analysis, or `jonCreateDevPlan` if the plan itself must change. For
small changes Jon reviews inline; the dedicated Reviewer agent stays a future extension.

**BDD:**
```
GIVEN Jon calls jonAskDev with a released plan
THEN the Dev agent's standing order carries the plan path and the instruction to call planImplemented() when done

GIVEN the Dev agent calls planImplemented()
THEN the plan is flipped to done, Jon gets the done-marker and reviews the result
AND Jon accepts (flipping ❌ → ✅) or returns change requests via jonAskDev
```

### R8: Atomic completion signals — `planComplete` / `planImplemented` (core) ❌
The tool-call loop ends by **natural stop** — a slave's turn is over when it emits plain text with no
tool call (`ToolService.executeLoop`, no terminal tool exists). So control returns to Jon on **every**
slave turn — it *is* a tool call. The signal solves a different problem: *did the slave just ask a
clarifying question / give an interim answer, or is the whole job done?*

Two **atomic completion signals**, authored **fresh in core**. The Eclipse `PlanTool` (IFile-bound) is
**not** moved or touched; it stays as standalone legacy.
- **`planComplete()`** — the **Plan** slave: the plan is ready; a **pure** signal (no file I/O) that
  sets the done-latch carrying the **link to the plan**. The plan file stays — Dev needs it.
- **`planImplemented()`** — the **Dev** slave: implementation is done. **Atomic:** in one step it
  archives/renames the plan to a *done* name **and** sets the done-latch (link → the archived plan). The
  archive runs through an injected **`PlanArchiver` port** (core interface): a **disk** impl in
  core/tests, an **IFile** impl in the Eclipse plugin. Making the rename **deterministic code** — not a
  second, LLM-discretionary tool call — is what guarantees the active-plan slot is freed for the next
  session; it stays fully core-testable via the disk impl.

They are **markers, not loop-enders**. Jon asks the slave *"are you done?"* and passes the **tool name +
"call it when you are finished"** in the dispatch prompt / standing order.

**Consume-once latch.** A signal only *sets* a small piece of state on the slave — an
`Optional<CompletionInfo>` (done + plan link). After `slave.call(...)` returns, the `jonAsk*` tool
**reads the latch**: if present, it surfaces "done + plan link" to Jon (the tool result **plus** an
OK/done chat message into Jon's memory) and **clears it back to `Optional.empty()`**, so a stale "done"
is never re-consumed on the next turn. If the latch is empty, Jon treats the reply as a clarifying
question / interim status (→ answer or escalate, R13).

**Per-agent tool filtering (static per instance, KV-cache safe):**

| Agent | `planComplete` | `planImplemented` |
| --- | --- | --- |
| Plan slave | ✅ | — |
| Dev slave | — | ✅ |
| Jon | — | — |
| standalone Peon-Plan / Peon-Dev | — | — |

The Dev slave never sees `planComplete`, the Plan slave never sees `planImplemented`, and **Jon has no
plan tools at all**. The signals live **only** on Jon's dedicated slave instances — they never leak into
the user-selectable standalone Plan/Dev. Filters stay **static per agent instance** (a per-turn tool-set
change would kill the KV-cache); `ToolService.addTool` also **throws on a duplicate name**.

**BDD:**
```
GIVEN Jon dispatches the Plan slave with "call planComplete() when the plan is ready"
WHEN the Plan slave calls planComplete()
THEN Jon gets the plan link plus an OK/done chat message marking the plan as done
AND the Dev slave was never offered planComplete

GIVEN Jon dispatches the Dev slave with "call planImplemented() when done"
WHEN the Dev slave calls planImplemented()
THEN planImplemented atomically archives the plan to a done name via the PlanArchiver port and the latch signals Jon completion
AND the Plan slave was never offered planImplemented

GIVEN a slave replies without calling its completion signal
THEN the latch is empty and Jon treats the reply as a clarifying question or interim status, not as done

GIVEN a completion latch was surfaced to Jon
WHEN Jon calls the same slave again
THEN the latch reads Optional.empty() and the stale "done" is not re-consumed
```

### R9: Slave lifecycle — lazy persistent singletons ❌
Per Jon session there is **exactly one** Peon-Plan and **one** Peon-Dev instance. Each is created
**lazily** — the first Plan-side call (`jonCreateDevPlan` / `jonAskQuestion`) creates the Plan slave,
the first `jonAskDev` the Dev slave — then **kept alive**, holding its context across calls.
Peon-Search stays a stateless **one-shot** agent.

These are **dedicated, Jon-owned instances** — **not** the user-selectable Peon-Plan/Peon-Dev from
`AgentService`, and with **their own history files**. Sharing them would make `jonAsk*` mutate the very
memory/history the user sees, and the per-agent `working` guard would silently **queue** the nested
`call()` (returning `null`) — so the slaves must be distinct instances.

**BDD:**
```
GIVEN no plan slave exists yet
WHEN Jon makes his first jonCreateDevPlan or jonAskQuestion call
THEN a single Peon-Plan instance is created and reused for every later Plan-side call in the session

GIVEN Jon uses SearchAgentTool
THEN it runs one-shot and holds no context between calls
```

### R10: Just-in-time compaction of the slaves ❌
A slave is compacted **only just before Jon sends it the next message**, and only when its context
exceeds a threshold — a single constant, **default 60 %** (to be fine-tuned later). Implemented by
calling `slave.compressContext(monitor)` before dispatch. Jon's message is delivered as a **standing
order** (like a `/` command) so it **survives the compaction** and is placed **before** the compact
result (`ToolLoopRequest.clearMemory()` re-injects standing orders after clearing) — the slaves never
eat their own context away.

The threshold needs its **own explicit constant/basis**: `AbstractAgent.tokenContextUsedInPercent()`
caps its denominator at `min(autoCompactAfter, 4000)`, so "60 %" must be defined against a clear base,
not that fuzzy value.

**BDD:**
```
GIVEN a slave's context is below the threshold
WHEN Jon sends it a message
THEN no compaction happens

GIVEN a slave's context exceeds the threshold (default 60%)
WHEN Jon sends it a message
THEN the slave is compacted first, and Jon's message (as a standing order) survives and precedes the compact result
```

### R11: Synchronous work, Stop only ❌
**Only while a `jon*` call is actively executing** the chat input is disabled — the sole control is
**Stop**. Stop terminates the running (sub-)agent via today's abort path
([ADR-0018](adr/0018-abort-path-parity.md)) and the user can type again. No queued messages, no
interleaved chat in the MVP. (Jon ending his turn to escalate is **not** "working" — the input
re-enables, see R13.)

**Abort mid-tool caveat:** ADR-0018's "no tool result on abort" holds at model-turn granularity, not
for a sub-agent nested inside a tool. So `jonAsk*` must itself check `monitor.isCanceled()` **after**
the slave returns and drop the tool result on Stop, instead of feeding a half-finished slave reply back
into Jon's memory.

**BDD:**
```
GIVEN Jon is running a jonCreateDevPlan / jonAskQuestion / jonAskDev loop
THEN the chat input is disabled and only Stop is available

GIVEN the user presses Stop mid-loop
THEN the running sub-agent is aborted, no tool result is produced, and the input is re-enabled
```

### R12: Header status ❌
While Peon-PO is active, the header shows — next to the usual sent/received token counts —
`peon-dev(context-size)` and `peon-plan(context-size)`, each with a status ball (active / waiting /
idle). Exact ball colours are deferred.

**BDD:**
```
GIVEN Peon-PO is active with a live plan and dev slave
THEN the header shows peon-plan(context-size) and peon-dev(context-size) with their status balls
```

### R13: Escalation to the user (anti-deadlock) ❌
Jon answers the slaves' questions himself as long as he can decide from the docs. A question he
**cannot** answer (a genuine user decision) he does **not** guess — he **ends his own turn** with the
question to the user. No `jon*` loop is then running, so the chat input re-enables (R11) and the user
answers. Because the slaves are persistent singletons (R9), Jon **resumes** the paused slave with the
answer via the next `jonCreateDevPlan` / `jonAskDev` call.

This is the rule that prevents the hang: Jon **never blocks inside the tool-call loop waiting for the
user** — escalation is always "end turn, resume later", which the persistent slaves make possible.

**BDD:**
```
GIVEN a slave asks Jon a question that requires a user decision
WHEN Jon cannot answer it from the docs
THEN Jon ends his turn with the question, the chat input re-enables, and no slave is aborted

GIVEN the user answered an escalated question
WHEN Jon continues
THEN he resumes the same persistent slave via jonCreateDevPlan / jonAskDev carrying the answer
```

## Future Extensions (not MVP)

- **Reviewer** — a dedicated agent Jon dispatches to review a plan or the changed code against the
  docs when he is unhappy with Plan/Dev output. In the MVP Jon does this review himself (R6/R7).
- **Async chat / queued messages** — let the user keep talking to Jon while the slaves work
  (collect input, ack, resolve on the next tool result) instead of the synchronous Stop-only model
  (R11).
- **Generalised handover artefact** — fold the two completion signals (R8) and today's UI-button
  `handoverTo()` / `onHandoff` into one model-callable mechanism where any agent writes a
  `<agent-name>-handover.md` and signals `handoverDone`. Deferred as scope creep: it would rework the
  existing standalone Plan→Dev handoff (breaking the "100 % additive" stance) and conflates "report done
  to my orchestrator" with "hand control to another agent". The MVP signals are shaped to carry a
  link/artefact (the Dev flow renames the plan to a done artefact), so a later merge stays cheap.

## ADRs

- [ADR-0020](adr/0020-po-agent-orchestration.md) — Jon orchestrates Plan/Dev as sub-agents via `jon*`
  tools with `planComplete` / `planImplemented` completion signals (vs. the one-shot button handoff).
- [ADR-0021](adr/0021-po-slave-lifecycle-jit-compaction.md) — slave lifecycle (lazy persistent
  singletons) & just-in-time compaction with standing-order survival.
- [ADR-0022](adr/0022-write-path-allowlist-decorator.md) — no bespoke Jon tools; the existing write
  tools are wrapped in a write-path-allowlist decorator driven by a comma-separated glob config
  (default `*/docs/*`).

Related: [Plan & Dev Agent](plan-dev-agent-design.md) — the standalone plan→dev handoff Jon builds on;
[Scaffold Agent](scaffold-agent.md) — the built-in-agent-with-own-ToolService pattern Jon reuses.
