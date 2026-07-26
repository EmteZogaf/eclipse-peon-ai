# ADR 0014 — Use System.lineSeparator() in Strings Sent to LLM

**Status** · Accepted

**Decision** · Use `System.lineSeparator()` in tool output strings sent to the LLM (directory listings, file content previews, error messages) instead of hardcoding `"\n"`. Keep `\n` only for pure markdown formatting where the LLM interprets it as a logical newline.

**Consequences** · LLM sees line endings matching the host OS, reducing mismatch bugs in tools like `diskReplaceLines`. Test assertions must use `System.lineSeparator()` or normalize before comparing to handle cross-platform CI runs.
