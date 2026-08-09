# ADR-0026: QuestionOrchestrator + ShellApprovalService extrahieren

## Status
✅ Implemented (2026-08-09)

## Context
`AIChatView` hatte ~900 Zeilen mit ~70 Zeilen verteilter Question-/Shell-Approval-Logik:
- `showQuestion()`/`hideQuestion()` manipultierten 4+ Widgets direkt (exclude/visible/layout)
- `applyShellCommandConfirmation()` las Preferences, erstellte CountDownLatch, rief `showQuestion()` auf
- Beide Pfade teilten sich Widget-Kenntnisse → Doppelte Abhängigkeiten, schwer zu testen

## Decision
Zwei stateless Orchestratoren extrahiert:
- **`QuestionOrchestrator`** (`org.sterl.llmpeon.parts.question`) — zentraler Widget-Swap (`showQuestion`/`hideQuestion`/`cancelSilently`)
- **`ShellApprovalService`** (`org.sterl.llmpeon.parts.shell`) — stateless Shell-Confirmation-Logik, zero SWT-Dependency

## Consequences
- `AIChatView` thinner (~70 Zeilen weniger), delegiert an Orchestratoren
- Shell-Approval testbar ohne SWT/Mockito-UI-Mocks
- Dependency-Kette sauber: `AIChatView → QuestionOrchestrator → widgets`; `ShellApprovalService → QuestionOrchestrator (via QuestionPresenter)`

## See Also
- [Ask User Tool Design](../user-question-tool-design.md)
