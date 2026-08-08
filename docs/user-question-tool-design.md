# Ask User Tool — Design

## Business Requirements

- The LLM can pause mid-task and ask the user a clarifying question with optional predefined answer choices.
- The user can always override predefined choices with free text.
- One question per tool call; displayed inline in the chat view (no dialog).
- Cancelling (Stop button or job interruption) returns `"[canceled]"` to the LLM so it can react gracefully.
- **Queue-Safety:** The `AskUserTool` blocks the agent thread via `CountDownLatch`, guaranteeing the next user input is treated as the direct answer — preventing queued messages from interrupting the LLM's line of thought or misaligning with the question.

## Interaction Design

While a question is pending the normal input area is replaced by the question widget:

```
┌─────────────────────────────────────────────┐
│ [Question text label]                       │
│                                             │
│ ○ Predefined answer A                       │
│ ○ Predefined answer B                       │
│ ○ Enter own answer                          │  ← always present
│                                             │
│ [Text input, auto-grow]        [Answer]     │
└─────────────────────────────────────────────┘
```

Selecting a radio pre-fills the text field; the field stays editable so the user can refine or append.
The **Answer** button (and `Ctrl/Cmd+Enter`) always submits whatever is in the text field.
On submit the normal input reappears and the LLM receives the answer string.

## Key Technical Decisions

| Concern | Decision |
|---------|----------|
| **Package — tool** | `org.sterl.llmpeon.parts.tools` — `AskUserTool` |
| **Package — widgets** | `org.sterl.llmpeon.parts.widget` — `UserQuestionWidget`, `TextInputWidget` |
| **Thread sync** | `CountDownLatch(1)` blocks the LangChain4j background thread; the UI `onAnswer` callback releases it |
| **Widget swap** | `GridData.exclude` + `setVisible` on `UserInputWidget` / `UserQuestionWidget` inside `inputBlock` |
| **Cancel path** | `lockWhileWorking(false)` calls `questionWidget.cancel()` → fires `"[cancelled]"` → releases latch |
| **Text reuse** | `TextInputWidget` extracted from `UserInputWidget`; injected `Runnable onReflow` drives height propagation |
| **Tool registration** | `AIChatView.createPartControl` via `aiService.getToolService().addTool(new AskUserTool(...))` |
