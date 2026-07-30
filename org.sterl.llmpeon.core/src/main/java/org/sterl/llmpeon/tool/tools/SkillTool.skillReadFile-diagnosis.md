# SkillTool `skillReadFile` diagnosis

## Problem

`skillReadFile` fails for valid-looking paths returned by `skillRead`, for example:

```text
skillReadFile(name="catalog-documentation-creator", path="references/flows.md")
```

Observed result:

```text
File not found in skill directory: references/flows.md
```

This is noisy for agents because `skillRead` prints an inventory that includes paths such as:

```text
catalog-documentation-creator/references/flows.md
catalog-documentation-creator/SKILL.md
```

The natural next call is to remove the skill prefix and read `references/flows.md`, but the tool cannot resolve it.

## Diagnosis

The issue is in `SkillTool.skillReadFile`:

```java
return skill.get().readRelativeFile(path);
```

`SkillTool` validates only that `name` and `path` are non-blank. It does not normalize or resolve the path shown by the skill inventory, and it delegates all path semantics to `SkillPromptFile.readRelativeFile(path)`.

Likely causes to inspect next:

1. `SkillPromptFile.readRelativeFile` may resolve paths relative to the global skill root instead of the selected skill directory.
2. The paths displayed by `skillRead` are prefixed with the skill directory (`catalog-documentation-creator/references/flows.md`), while `skillReadFile` promises a path relative to the selected skill directory (`references/flows.md`). The two tool outputs/contracts may be inconsistent.
3. Skill names and directory names may differ by case or aliasing; `skillService.get(name)` succeeds, but the resolved disk directory used by `readRelativeFile` may not match the displayed directory.
4. The file list in `skillRead` may come from a broad filesystem listing, while `readRelativeFile` may use a narrower base directory or classpath resource lookup.

## Expected behavior

Both of these should work or the tool output should make the supported form explicit:

```text
skillReadFile(name="catalog-documentation-creator", path="references/flows.md")
skillReadFile(name="catalog-documentation-creator", path="catalog-documentation-creator/references/flows.md")
```

Prefer accepting both forms. Normalize by stripping an optional leading `<skill-directory>/` prefix after resolving the selected skill.

## Suggested fix

In `SkillTool.skillReadFile` or `SkillPromptFile.readRelativeFile`:

1. Resolve the selected skill first.
2. Normalize `path`:
   - reject absolute paths and `..` traversal,
   - strip a leading `<skillName>/` or actual skill directory name prefix if present,
   - normalize separators.
3. Resolve strictly under the selected skill directory.
4. If missing, return a diagnostic with:
   - selected skill name,
   - selected skill disk directory,
   - normalized relative path,
   - a short list of available files under that skill.

## Current workaround

Use `skillRead(name)` for the main skill body and avoid `skillReadFile` unless the exact relative path is verified by implementation. If a reference file is needed and `skillReadFile` fails, read the skill body or inspect the skill directory implementation before retrying.
