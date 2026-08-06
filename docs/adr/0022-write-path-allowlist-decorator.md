# ADR-0022: Scope an agent's writes via a write-path-allowlist decorator (comma-separated glob config)

**Status** · Proposed

## Context
Peon-PO (Jon) must be kept from writing anywhere but the docs, but he should **reuse the existing
Eclipse-workspace and disk write tools** rather than get bespoke, Jon-only file tools. "Where may this
agent write" is a cross-cutting concern — a decorator + config keeps it out of the tools themselves and
lets other (custom) agents reuse it. It also fits the existing sandbox stance
([ADR-0015](0015-eclipse-sandbox-boundary.md)): the write tools already carry a boundary; this narrows
it per agent.

## Decision
Wrap the existing write tools (Eclipse-workspace write, disk write) in a **path-allowlist decorator**.
The allowlist is a **comma-separated glob** config field — **user-editable in the settings UI**,
**preloaded with `*/docs/*`**. On each write the decorator matches the target against the patterns (OR)
and rejects a non-match. Reads are **not** gated by this decorator. Glob semantics:

- `*/docs/*` (default) — a `docs/` folder at any depth. In Eclipse matched against the
  **project-root-relative** path (`<project-name>/docs/...`, workspace VFS); for disk tools against the
  working-dir / given path.
- `docs/*` — only at the (project) root; the leading segment is the sole difference from `*/docs/*`.
- `*.md` — any Markdown file, anywhere.

## Consequences
- No Jon-specific file tools; the docs scope is pure configuration → reusable for other agents.
- Eclipse vs. disk differ only in the **base** the glob is matched against (project root vs. working
  dir) — the decorator normalises to that base before matching.
- The default `*/docs/*` gives Jon docs-anywhere; tightening to root-only (`docs/*`) or widening
  (`*.md`) is a one-field config change the user can make.
- The underlying write tool keeps auto-creating missing sub-paths, so an allowed `docs/` path
  materialises on the first write — no explicit "create the docs root" step is needed.
