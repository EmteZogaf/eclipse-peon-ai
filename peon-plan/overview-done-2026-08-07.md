# Homepage + Docs Update — Three Increments

## Context

- `docs/po-agent-jon.md` still describes the slaves as "lazy" created, but [ADR-0025](docs/adr/0025-po-status-widget-named-agents.md) decided on **eager** creation.
- The homepage lacks a proper agent overview page and a Jon & Team feature page.
- `agent-mode.md` needs to be renamed and reframed — the autonomous orchestration is now Jon's domain; the handoff button is a general mechanism.

## Increment 1: Docs-Abgleich (lazy → eager) ✅

**File:** `docs/po-agent-jon.md`

**Changes:**
1. **I2.2 (line ~130):** `created **lazily** on first` → `created **eagerly** on Jon activation` (ADR-0025: "Die Agenten werden **eager** erzeugt (leer/0k bis zur ersten Delegation)")
2. **R9 title (line ~513):** `Slave lifecycle — lazy persistent singletons` → `Slave lifecycle — eager persistent singletons`
3. **R9 body (line ~514):** `Each is created **lazily** — the first Plan-side call … creates the Plan slave` → `Each is created **eagerly** on Jon activation (empty/0k until first delegation) and **kept alive** …`
4. **ADR reference (line ~735):** `lazy persistent` → `eager persistent`

**Verification:** Grep for "lazy" in the file — zero hits after change. ✅

**BDD:**
```
GIVEN docs/po-agent-jon.md after Increment 1
WHEN searching for "lazy"
THEN no occurrences remain ✅
AND I2.2 and R9 both describe eager creation ✅
```

---

## Increment 2: Homepage `src/setup/agents.md` (new) ✅

**File:** `homepage/src/setup/agents.md` — new file ✅

**Content:** Overview page listing all built-in agents, Jon first. ✅

**Navigation update:** `homepage/.vitepress/config.ts` sidebar ✅
- Replace `{ text: 'Agent Mode', link: '/setup/agent-mode' }` with `{ text: 'Agents', link: '/setup/agents' }` ✅
- Added `{ text: 'Jon & Team', link: '/setup/peon-po' }` after 'Agents' ✅
- Added `{ text: 'Agent Handoff', link: '/setup/agent-handoff' }` after 'Jon & Team' ✅

**BDD:**
```
GIVEN the homepage is built after Increment 2
WHEN navigating to /setup/agents
THEN the page lists Peon-PO, Peon-Dev, Peon-Plan, Peon-Scaffold with descriptions ✅
AND the sidebar shows "Agents" instead of "Agent Mode" ✅
```

---

## Increment 3: `peon-po.md` (new) + `agent-handoff.md` (renamed) ✅

### 3a: New `homepage/src/setup/peon-po.md` ✅

Jon & Team feature page — user perspective, not technical. ✅

### 3b: Rename `agent-mode.md` → `agent-handoff.md` ✅

**File:** `homepage/src/setup/agent-handoff.md` (rename from `agent-mode.md`) ✅

**Changes to content:** ✅
- Frontmatter: `title: Agent Handoff`, `description: Hand off work between agents via the Handoff button` ✅
- Title: `# Agent Handoff` ✅
- Remove "Agent Mode" references; reframe as a general mechanism ✅
- First paragraph: `Agent handoff lets any agent pass work to another. Click the **Handoff → [Agent]** button to transfer control.` ✅
- Section "The handoff button" → keep, but expand ✅
- Remove "Pin", "Limitations" sections ✅
- Keep "Plan tools" section ✅

### 3c: Navigation updates ✅

**`homepage/.vitepress/config.ts` sidebar:** ✅
- Replace `{ text: 'Agent Mode', link: '/setup/agent-mode' }` → done in Increment 2 ✅
- Add `{ text: 'Agent Handoff', link: '/setup/agent-handoff' }` after 'Agents' ✅
- Add `{ text: 'Jon & Team', link: '/setup/peon-po' }` after 'Agents' ✅

Final sidebar order (Introduction group): ✅
```
{ text: 'Overview',        link: '/' },
{ text: 'Agents',          link: '/setup/agents' },
{ text: 'Jon & Team',      link: '/setup/peon-po' },
{ text: 'Agent Handoff',   link: '/setup/agent-handoff' },
{ text: 'Agents & Skills', link: '/setup/agents-and-skills' },
{ text: 'Custom Agents',   link: '/setup/custom-agents' },
{ text: 'Scaffold Agent',  link: '/setup/scaffold-agent' },
{ text: 'Commands',        link: '/setup/commands' },
{ text: 'Memory',          link: '/peon-memory' }
```

**BDD:**
```
GIVEN the homepage is built after Increment 3
WHEN navigating to /setup/peon-po
THEN the Jon & Team page explains the autonomous orchestration from a user perspective ✅

WHEN navigating to /setup/agent-handoff
THEN the page describes the handoff mechanism without "Agent Mode" branding ✅

WHEN checking the sidebar
THEN "Agent Mode" is gone, and "Agents", "Jon & Team", "Agent Handoff" are present ✅

WHEN checking /setup/agent-mode
THEN it returns 404 (file renamed) ✅
```

---

## Gap Analysis (self-review)

### Increment 1
- ✅ Two "lazy" occurrences found: I2.2 (line 130) and R9 (line 513/514)
- ✅ ADR-0025 confirms eager creation decision
- ✅ Change is minimal, isolated, no side effects

### Increment 2
- ✅ New `agents.md` created with table of all 4 built-in agents
- ✅ Jon listed first (matches dropdown order)
- ✅ Links to `peon-po` (Inc 3), `scaffold-agent` (existing)
- ✅ Sidebar updated — "Agent Mode" replaced with "Agents"
- ⚠️ `custom-agents.md` still says "two built-in agents (Peon-Plan, Peon-Dev)" — **outdated**. Recommend updating to "four built-in agents (Peon-PO, Peon-Dev, Peon-Plan, Peon-Scaffold)" — **not in scope** but flagged.

### Increment 3
- ✅ `peon-po.md` covers Jon from user perspective: describe → design → plan → build → review
- ✅ Mentions Da Thinka / Da Mek as team members
- ✅ Status indicators section reflects ADR-0025
- ✅ `agent-mode.md` → `agent-handoff.md` rename + reframe
- ✅ Handoff section generalized, "Agent Mode" branding removed
- ✅ Sidebar updated with all new entries
- ⚠️ `agents-and-skills.md` references only AGENTS.md/Skills — no agent overview (that's now `agents.md`). **No conflict**, they serve different purposes.

### Open questions
- **`custom-agents.md` outdated "two built-in agents"** — update in a separate pass or now? (Recommendation: separate pass, keep increments small.)
- **`agents-and-skills.md` title/description** — "AGENTS.md & Skills" is about the file convention, not the agents themselves. No clash with the new `agents.md`, but the names are close. (Recommendation: leave as-is; they cover different topics.)
- **`scaffold-agent.md` mentions "two built-in agents"** implicitly (no explicit count). Verify? (Checked: no count mentioned. OK.)
