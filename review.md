# PR #112 Review — Live streaming + JSON serialization

## What's good

- **Jackson serialization** instead of string interpolation — fixes XSS/injection bugs with quotes in streamed text. Worth merging regardless.
- **`tokens == 1` first-token check** — the live status now appears immediately instead of waiting for 20 tokens. Nice UX fix.
- **Division-by-zero guard** on `tokPerSec` — correct.
- **Toggle via `showRealtimeAiResponse`** — lets users opt out if needed.
- **Suppress duplicate THINK/AI messages** in `AIChatView` — prevents double-rendering when both the partial stream and the final `appendMessage` fire.

## Performance issue — O(n²) markdown re-rendering

`updateLastAnsweringMessage` (and `updateLastThinkingMessage`) do:

```js
const updated = existing + delta;
lastMessage.dataset.raw = updated;
contentDiv.innerHTML = md.render(updated);  // ← re-renders the ENTIRE accumulated text
```

Every partial chunk triggers `md.render()` on the **full accumulated string** and replaces `innerHTML`. A 600-token response = ~30 re-renders of increasingly large DOM. THINK blocks can be even longer. This will cause visible jank on slower machines or with WebKit2 (which is already slower than CEF).

## Recommended fix — render only the delta

```js
function updateLastAnsweringMessage(messagePart) {
    const container = document.getElementById("chat");
    const lastMessage = container.lastElementChild;
    const delta = messagePart ?? "";

    if (lastMessage && lastMessage.classList.contains('AI')) {
        const contentDiv = lastMessage.querySelector('.message-content');
        if (contentDiv) {
            contentDiv.insertAdjacentHTML('beforeend', md.render(delta));
        }
    } else {
        const div = document.createElement("div");
        div.className = "message AI";
        div.innerHTML = '<div class="message-content">' + md.render(delta) + '</div>';
        container.appendChild(div);
    }
    window.scrollTo(0, document.body.scrollHeight);
}
```

Same for `updateLastThinkingMessage`. This turns O(n²) into O(n) and avoids the DOM thrash from `innerHTML` replacement.

## Optional — debounce for extra smoothness

If jank persists, debounce the JS side so rapid chunks batch into one DOM update:

```js
let thinkingDebounce = null;
function updateLastThinkingMessage(msg) {
    clearTimeout(thinkingDebounce);
    thinkingDebounce = setTimeout(() => { /* actual render */ }, 100);
}
```

## Review issues (top 3 — 100% sure)

### 1. Same O(n²) bug in THINK blocks

`updateLastThinkingMessage` does the same `innerHTML = md.render(updated)` on the full accumulated string. A long thinking block = many re-renders of increasing size. Fix with delta-render like ANSWER.

### 2. `toolMessage` uses `innerHTML` — injection risk

The Jackson fix solved XSS for streamed text, but `toolMessage` still uses `innerHTML` with unescaped content:

```js
const div = document.createElement("div");
div.className = "message TOOL";
div.innerHTML = message;  // ← unescaped
```

Use `textContent` instead.

### 3. `scrollTo` fires on every streaming chunk

`window.scrollTo(0, document.body.scrollHeight)` is called in `updateLastAnsweringMessage`, `updateLastThinkingMessage`, and `updateLiveResponse`. That's ~30+ DOM scrolls per response. Live status is already at the bottom — only scroll when a new message is appended.

## Additional issues found

### 4. Final message double-renders

`appendMessage` calls `updateLastThinkingMessage(message.message)` / `updateLastAnsweringMessage(message.message)` with the full accumulated text *after* streaming already built the message. The full text gets rendered twice. Skip the call in `appendMessage` for THINK/ANSWER roles.

### 5. `updateLiveResponse` null check is a no-op

`thinkChunk != "null"` compares a string to the literal `"null"`, not to JavaScript `null`. Should be `thinkChunk && thinkChunk !== "null" && thinkChunk !== null`.

### 6. Missing CSS for `.thinking-content`

The THINK message uses `.thinking-content` but there's no style rule for it. Should use `.message-content` like the AI path or add a CSS rule.

### 7. Markdown highlighter uses string concatenation

The `highlight` callback in `markdownit` builds HTML via string concat (`'...' + hljs.highlight(...).value + '...'`). Untrusted code blocks could be exploited. Use `md.utils.escapeHtml` or a safer rendering approach.

### 8. `toolMessage` line truncation is fragile

`last.innerHTML.split('<br>')` breaks if the message contains HTML entities or multi-line content. Use `textContent` and `split('\n')` instead.
