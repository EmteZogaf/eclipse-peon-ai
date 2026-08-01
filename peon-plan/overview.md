# Streaming Response Display — Plan

## Context

**Goal:** Fix streaming response display to eliminate flicker, repeated markdown parsing, and unbounded content growth during long THINK/AI responses.

**Background:** The current `showRealtimeAiResponse` feature renders markdown incrementally on every streaming chunk (`md.render(fullAccumulatedText)` N times). This causes:
- Constant DOM flicker (updates on every chunk)
- Performance degradation (full MD parse per chunk)
- Unbounded content growth ("black screen" for large THINK blocks)

**Why this change:** The stream should be a lightweight progress indicator, not the canonical message insert. The final `onChatResponse` is the single insert point with proper MD highlighting.

## Design Decisions

- **Overlay preview:** The existing `.live-chunk` in `#live-status` shows the actual streamed text (THINK + ANSWER) instead of the current token count. `.live-state` keeps showing elapsed time + tok/s.
- **Bounded preview:** `.live-chunk` gets `max-height: 300px; overflow-y: auto` — auto-scrolls via `scrollTop = scrollHeight` after each append. Prevents "black screen" on large responses.
- **Line endings:** `\n` and `\r\n` in streamed text are replaced with `<br>` for correct line breaks in the preview (plain text, no MD).
- **Single DOM insert:** THINK and AI messages are inserted once at the end via `onChatResponse`, with full MD highlighting applied. No scroll-to-bottom during streaming — only on final message insert.
- **No suppression:** Remove the conditional logic that skipped `appendMessage` when `showRealtimeAiResponse` was enabled — always append THINK/AI on `onChatResponse`.
- **Cleanup:** Remove the incremental JS update functions (`updateLastThinkingMessage`, `updateLastAnsweringMessage`) and their Java callers — they served the broken incremental chat rendering.

## Architecture Decisions

No new components. Changes are confined to the existing streaming display path:

```
StreamingBridge → AiMonitor.onStreamingChunk → AIChatView → ChatMarkdownWidget.onStreamingChunk
                                                                                      ↓
                                                                             status bar update only (existing)

ToolService.executeLoop → ToSimpleMessage.convert → AiMonitor.onChatResponse
                                                                        ↓
                                                            AIChatView → ChatMarkdownWidget.appendMessage
                                                                        ↓
                                                              single MD render + DOM insert
```

## Affected Files

| File | Change |
|------|--------|
| `org.sterl.llmpeon/parts/widget/ChatMarkdownWidget.java` | Remove `updateThinkingMessageInUIThread` + `updateAnsweringMessageInUIThread`; accumulate streamed text in `onStreamingChunk`; pass accumulated text to `updateLiveResponseInUIThread` as the chunk preview; add auto-scroll JS call |
| `org.sterl.llmpeon/parts/AIChatView.java` | Remove suppression logic in `onChatResponse` — always append THINK and AI messages |
| `org.sterl.llmpeon/resources/chat/chat.html` | Remove `updateLastThinkingMessage` + `updateLastAnsweringMessage`; update `updateLiveResponse` to render chunk text with `\n`→`<br>` + auto-scroll; add `max-height: 300px; overflow-y: auto` to `.live-chunk` CSS |

**Unchanged:**
- `StreamingBridge.java` — streaming contract stays the same
- `ToolService.java` — `onChatResponse` calls stay the same
- `ToSimpleMessage.java` — conversion logic stays the same
- `AIChatView` config handling — `showRealtimeAiResponse` pref stays but now means "show status bar" (semantics unchanged from user perspective)

## Rules & Constraints

- **Thread safety:** All UI updates must run in the UI thread (existing `EclipseUtil.runInUiThread` usage)
- **Browser readiness:** JS execution must respect the `browserReady` / `pendingExecutions` queue (existing pattern)
- **No behavioral change for the user:** The final message appears the same; the only difference is no intermediate flicker

## BDD Use Cases

### R1 — Overlay shows streamed text preview, not chat messages
During streaming, THINK and ANSWER chunks accumulate in the overlay preview (`.live-chunk`), not in the chat history. Line endings are converted to `<br>`.

- **GIVEN** a user sends a message **WHEN** the AI streams THINK/ANSWER chunks **THEN** `.live-chunk` shows the accumulated text with `<br>` line breaks, and the chat history has no AI/THINK message yet
- **Tag:** unit (verify `ChatMarkdownWidget` accumulates text + calls `updateLiveResponseInUIThread` with chunk text, not JS message functions)

### R2 — Overlay preview is bounded and auto-scrolls
The preview div doesn't grow unbounded; it scrolls internally.

- **GIVEN** a large response streams > 300px of text **WHEN** chunks arrive **THEN** `.live-chunk` stays within `max-height: 300px` and auto-scrolls to show the latest text
- **Tag:** CSS/behavior verification (manual + CSS inspection)

### R3 — Final THINK message is appended on completion
When the tool loop completes, the THINK message from the final response is inserted into the chat with MD highlighting.

- **GIVEN** a response with thinking content **WHEN** `onChatResponse` is called with a THINK message **THEN** the message is appended to the chat → unit: verify no suppression logic blocks the append
- **Tag:** unit

### R4 — Final AI message is appended on completion
The canonical AI message is always inserted via `onChatResponse`, regardless of the realtime setting.

- **GIVEN** `showRealtimeAiResponse` is enabled or disabled **WHEN** `onChatResponse` is called with an AI message **THEN** the message is appended to the chat with MD highlighting
- **Tag:** unit

### R5 — No duplicate messages
The chat history contains exactly one THINK and one AI message per tool-loop response.

- **GIVEN** a response with thinking and answer **WHEN** streaming completes and `onChatResponse` fires **THEN** the chat has one THINK + one AI message (not two of each)
- **Tag:** integration

### R6 — Status bar hides on END
The live status bar (including preview) is hidden when streaming ends.

- **GIVEN** the overlay is visible during streaming **WHEN** a streaming chunk with type END arrives **THEN** `hideLiveStatus()` is called and the overlay disappears
- **Tag:** unit (existing behavior, verify preserved)

## Open Questions

- **None resolved inline needed.** The implementer follows the design directly.

## Rules & Constraints

- **Thread safety:** All UI updates must run in the UI thread (existing `EclipseUtil.runInUiThread` usage)
- **Browser readiness:** JS execution must respect the `browserReady` / `pendingExecutions` queue (existing pattern)
- **No escaping:** Streamed text is trusted (from our LLM) — no HTML escaping, only `\n`→`<br>` replacement
- **No scroll during streaming:** Only scroll-to-bottom on final message append, not on every chunk

## Docs Changes

- **Story:** Create `docs/streaming-display.md` — goal + rules + BDD from this plan
- **ADR:** No ADR needed — decision is clear from BDD (defer heavy rendering to final insert, overlay for live preview)
- **Story registry:** Add entry to `docs/index.md`
- **User-facing docs:** No change — UX improvement, no new user-facing capability
