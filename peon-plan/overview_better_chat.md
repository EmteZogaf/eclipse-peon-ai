# Modernize Chat UI — Look & Feel Upgrade

## Context

**Goal:** Upgrade the visual polish of the LLM Peon chat interface to feel modern and fresh while maintaining Eclipse RCP integration, keeping the left border bar system, and prioritizing AI messages as the hero content in a large chat area.

**Why:** The current UI is functional but visually dated. As an agent-driven workflow tool, the AI response area should feel premium and spacious. Modern chat UX patterns (rounded bubbles, glassmorphism, better code blocks) should be adopted without losing Eclipse's native feel.

**Scope:** Visual/styling upgrades only. No behavior changes, no new features. Keep all existing functionality intact.

---

## Design Decisions

### 1. AI Messages as Hero Content
- **Decision:** AI messages get the most visual weight — larger padding, subtle background differentiation, thicker left border (5px gradient), better typography
- **Rationale:** Agent-driven workflow means AI responses are the primary content; they deserve breathing room
- **Implementation:** `.message.AI` gets `background: linear-gradient(90deg, #f0fdf4 0%, #ffffff 100%)`, `padding: 12px 16px`, `border-left: 5px solid` with gradient

### 2. User Messages — Compact but Clear
- **Decision:** Keep blue left border (3px), add subtle right-side rounding, slightly more compact padding
- **Rationale:** Users need to distinguish their messages but they're secondary to AI responses
- **Implementation:** `.message.USER` gets `border-radius: 0 8px 8px 0`, `padding: 8px 12px`

### 3. Code Blocks — Developer-First
- **Decision:** Add top bar with language name (right-aligned) and optional filename, better padding, more prominent copy button
- **Rationale:** Code is the primary output; needs excellent readability and copy UX
- **Implementation:** 
  - JS `highlight` callback now wraps `<pre>` with a `.code-header` div containing language badge (right-aligned) and optional filename (left-aligned, italic)
  - Copy button becomes a full-height right edge with hover reveal (CSS `opacity` transition)
  - Language detection: only show language name (filename detection deferred per OQ2)
- **Scope note:** This is a **logic change** to the markdown-it highlight callback, not just CSS — see JS changes in Affected Files table

### 4. Glassmorphism Live Bar
- **Decision:** Status bar uses `backdrop-filter: blur(12px)` with semi-transparent dark background
- **Rationale:** Modern visual effect that doesn't obscure content behind it
- **Implementation:** 
  - **Base rule (works everywhere, including MSHTML):** `background: rgba(15, 20, 32, 0.95)`
  - **Progressive enhancement:** `@supports (backdrop-filter: blur(1px)) { #live-status { background: rgba(15, 20, 32, 0.85); backdrop-filter: blur(12px); } }`
  - **Note:** `@supports` itself isn't supported in MSHTML, so the base rule applies there. Browsers that support `@supports` + `backdrop-filter` (WebView2, modern WebKit) get the blur; others get the solid bg.

### 5. Left Border Bars — Enhanced
- **Decision:** Keep the color-coding system with gradient AI border and **opacity pulse during streaming**
- **Rationale:** User explicitly likes these; they work for quick role identification. Pulse animates only during active generation to signal "working" state without distracting after completion.
- **Implementation:** 
  - Static gradient: `.message.AI::before { background: linear-gradient(180deg, #1a7f37 0%, #4ade80 100%); }`
  - Streaming pulse: `.message.AI.streaming::after { animation: pulse-border 1.6s ease-in-out infinite; }` with `@keyframes pulse-border { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }`
  - **JS hook:** Add `.streaming` class on first chunk, remove on message completion (same scope as copy-button work in Phase 1)
- **Constraint:** Only animates `opacity` (GPU-accelerated, compositor-only) — satisfies the existing performance constraint
- **Scope:** Pulse applies only to `.streaming` state, not all AI messages — completed messages remain calm and readable

### 6. Typography — Better Readability
- **Decision:** Increase base font size to 14px, line-height to 1.6, better heading hierarchy
- **Rationale:** Large chat area deserves comfortable reading; modern fonts need more space
- **Implementation:** `body { font-size: 14px; line-height: 1.6; }`, headings get proper scale (1.5em H1, 1.3em H2, etc.)

### 7. Compact Input Area
- **Decision:** Reduce input block height by 20-30% through tighter spacing, smaller buttons, better visual hierarchy
- **Rationale:** Maximize chat area; Eclipse style means fixed bottom bar but can be sleeker
- **Implementation:** Reduce margins/padding in UserInputWidget, ActionsBarWidget, StatusLineWidget by 20%; use smaller icon buttons

---

## Architecture Decisions

### Component Boundaries

```
AIChatView (Java layout)
├── HeaderBarWidget (Java) — tokens left, hammer right
├── ChatMarkdownWidget (HTML/CSS/JS) — the chat area
│   ├── chat.html (CSS + JS logic)
│   ├── highlight.js (syntax)
│   ├── markdown-it (rendering)
│   └── diff2html (unified diffs)
└── InputBlock (Java composite, SWT.BORDER)
    ├── UserInputWidget (file chips + StyledText + buttons)
    ├── ActionsBarWidget (mode/model selectors)
    └── StatusLineWidget (project/skills/AGENTS.md/MCP/compact)
```

### Data Flow
- **Java → HTML:** `ChatMarkdownWidget` uses `Browser.execute()` to call JS functions (`appendMessage`, `updateLiveResponse`, `hideLiveStatus`, `appendDiff`)
- **HTML → Java:** `Browser.addLocationListener` handles `open-in-editor:` URLs from diff file clicks
- **CSS:** Pure styling, no data flow

### Integration Points
- **Eclipse native widgets** stay as-is (SWT buttons, StyledText, Labels)
- **HTML/JS side** gets full creative freedom for modern styling
- **Browser widget** handles the rendering boundary

---

## Affected Files

### Java (SWT Widgets — Compact & Polish)

| File | Changes |
|------|---------|
| `/org.sterl.llmpeon/src/org/sterl/llmpeon/parts/AIChatView.java` | Reduce `inputBlockData.verticalIndent` from 5 to 3; adjust layout spacing |
| `/org.sterl.llmpeon/src/org/sterl/llmpeon/parts/widget/HeaderBarWidget.java` | Reduce margins from 4 to 2; use smaller icon (16x16); tighten token readout |
| `/org.sterl.llmpeon/src/org/sterl/llmpeon/parts/widget/UserInputWidget.java` | Reduce StyledText min rows from 2 to 1.5 (via lineHeight); smaller button column (24px vs 28px); tighter file chip padding |
| `/org.sterl.llmpeon/src/org/sterl/llmpeon/parts/widget/ActionsBarWidget.java` | Reduce verticalSpacing from 2 to 1; use 16x16 icons; tighter combo/button padding |
| `/org.sterl.llmpeon/src/org/sterl/llmpeon/parts/widget/StatusLineWidget.java` | Reduce horizontalSpacing from 4 to 2; smaller labels; use icon+label compact format |

### HTML/CSS/JS (Chat Area — Modern Look)

| File | Changes |
|------|---------|
| `/org.sterl.llmpeon/resources/chat/chat.html` | **Major CSS overhaul:** message bubbles (using `::before` for borders to avoid border-radius clipping), code block headers, glassmorphism live bar with `@supports` fallback, typography, left border gradients (static, no animation), streaming pulse animation |
| `/org.sterl.llmpeon/resources/chat/chat.html` (JS) | **Logic change:** `highlight` callback now wraps `<pre>` with `.code-header` div containing language name (right-aligned); copy button uses CSS `opacity` transition for hover reveal; **add `.streaming` class toggling** (add on first chunk, remove on completion); no filename detection (deferred) |
| `/org.sterl.llmpeon/resources/chat/test-chat.html` | **New:** Manual test page that loads chat.html's JS/CSS and provides buttons to trigger test scenarios (appendMessage, updateLiveResponse, appendDiff, clear) for Phase 3 verification |

### Icons (Optional — If Needed)

| File | Changes |
|------|---------|
| `/org.sterl.llmpeon/icons/` | Optionally create 16x16 versions of existing icons if 20x20 looks too large in compact layout |

---

## Rules & Constraints

### Visual Constraints
- **Left border bars stay:** USER=blue (#0969da), AI=green (#1a7f37), TOOL/THINK=gray (#ccc), PROBLEM=red (#d32f2f)
- **Eclipse native integration:** SWT widgets keep their native backgrounds; no forced color overrides
- **No behavior changes:** All existing functionality (streaming, diffs, code copy, file links) must work identically
- **Responsive within bounds:** The HTML area fills whatever space SWT gives it; CSS must handle resize gracefully

### Technical Constraints
- **Browser widget compatibility:** CSS/JS must work in SWT Browser (WebKit2 on Linux, MSHTML on Windows, WebKit on macOS) — avoid cutting-edge features without fallbacks
- **Performance:** No layout thrashing; CSS transitions should be GPU-accelerated (`transform`, `opacity`)
- **Accessibility:** Maintain sufficient color contrast ratios (WCAG AA minimum); decorative borders (3-5px) are exempt from text-contrast rules
- **No new dependencies:** Only use existing highlight.js, markdown-it, diff2html

### Testing Approach
- **Manual test page:** `test-chat.html` in `/resources/chat/` provides buttons to trigger JS functions (`appendMessage`, `updateLiveResponse`, `appendDiff`, `clearMessages`) with pre-populated examples
- **Purpose:** Phase 3 verification without needing full Eclipse runtime; catches JS/CSS issues before platform testing
- **Limitation:** Does not replace Eclipse SWT Browser testing (Phase 4) — browser engine differences (WebKit vs MSHTML) must still be verified in-Eclipse
- **Dev-only Playwright (optional, not blocking):** A Playwright script can drive `test-chat.html` via `file://` and assert DOM/class-state programmatically (e.g., `.code-header` presence, `.streaming` class toggling, computed styles) — deferred as nice-to-have, not blocking Phase 3. Playwright would be a devDependency only, never bundled into the RCP artifact.

### Browser Compatibility Notes (Pre-existing & New)
- **Pre-existing constraint:** The current `chat.html` already uses `:has()` selector (`.live-state:has(+ .live-chunk)::after`). This selector only reached stable support in Firefox 121 (Dec 2023) and has version-dependent WebKitGTK support. If it's already broken on target Linux distros, Phase 3 testing should catch this rather than building new features on shaky ground.
- **MSHTML/legacy Trident handling:** 
  - `@supports` at-rule itself isn't supported in MSHTML, so the progressive enhancement pattern (base rule = solid bg, `@supports` adds blur) works correctly: MSHTML ignores `@supports` and uses the base solid background.
  - Border gradients via `::before` pseudo-element: MSHTML supports `position: absolute` and gradients via `filter`, but rendering may differ — test on legacy Windows if applicable.
  - **Open Question (OQ5):** Confirm whether WebView2 is the minimum supported renderer. If yes, MSHTML caveats are moot and documentation can be simplified.
  - **Decision:** Implement progressive enhancement pattern; Phase 3 testing verifies behavior on target platforms.

### Naming Conventions
- CSS classes: keep existing naming (`message`, `message.USER`, `message.AI`, etc.)
- New CSS classes: follow BEM-like pattern (`.message`, `.message__header`, `.code-block`, `.code-block__header`)
- Java: no changes to existing method signatures or class names

---

## BDD Use Cases

### UC1 — AI Messages Get Premium Visual Treatment
- **GIVEN** the chat displays an AI response **WHEN** the message is rendered **THEN** it has a subtle green-to-white gradient background, 12px padding, 5px left border with green gradient (via `::before`), and 14px font size
- **GIVEN** the chat displays an AI response **WHEN** it is actively streaming (has `.streaming` class) **THEN** the left border pulse-animates (opacity 0.5→1→0.5 over 1.6s) to signal "working" state
- **GIVEN** the chat displays an AI response **WHEN** streaming completes (`.streaming` class removed) **THEN** the pulse animation stops and the border remains static
- **GIVEN** the chat displays multiple AI messages **WHEN** scrolling **THEN** each AI message maintains consistent spacing (8px between messages) and visual hierarchy
- **Tag:** unit (verify CSS selectors, computed styles, and JS class toggling)

### UC2 — User Messages Remain Distinct but Compact
- **GIVEN** the chat displays a user message **WHEN** rendered **THEN** it has a 3px blue left border (using `::before` pseudo-element to avoid border-radius clipping), 8px padding, and subtle right-side rounding (8px border-radius on right corners)
- **GIVEN** user and AI messages alternate **WHEN** viewing chat **THEN** clear visual distinction exists without competing for attention
- **Tag:** unit (verify CSS selectors and pseudo-element positioning)

### UC3 — Code Blocks Have Top Bar with Language
- **GIVEN** a code block is rendered **WHEN** highlighted **THEN** a `.code-header` div appears above the `<pre>` containing the language name (right-aligned, small font, muted color)
- **GIVEN** a code block has a filename context **WHEN** rendered **THEN** the filename appears in the header (left-aligned, italic, muted)
- **GIVEN** user hovers over a code block **WHEN** the copy button is revealed **THEN** it appears as a full-height right edge with a hover state
- **Tag:** integration (verify JS `highlight` function and CSS)

### UC4 — Live Status Bar Uses Glassmorphism
- **GIVEN** a streaming response is in progress **WHEN** the live bar appears **THEN** it uses `backdrop-filter: blur(12px)` with semi-transparent dark background
- **GIVEN** content scrolls behind the live bar **WHEN** the bar is visible **THEN** the background content is blurred through the bar
- **Tag:** unit (verify CSS and Browser widget compatibility)

### UC5 — Left Border Bars Show Gradient
- **GIVEN** an AI message is displayed **WHEN** rendered **THEN** the left border uses a vertical gradient from #1a7f37 to #4ade80
- **GIVEN** a user message is displayed **WHEN** rendered **THEN** the left border remains solid #0969da (no gradient)
- **Tag:** unit (verify CSS border styling)

### UC6 — Compact Input Area
- **GIVEN** the input block is displayed **WHEN** the view is created **THEN** the total height is 20-30% smaller than before (measured via SWT bounds)
- **GIVEN** the input block contains file chips **WHEN** files are attached **THEN** chips use smaller padding (4px 8px instead of 6px 10px)
- **GIVEN** the input block contains action buttons **WHEN** displayed **THEN** buttons use 16x16 icons with 24px height
- **Tag:** unit (verify Java widget dimensions)

### UC7 — Typography Improvements
- **GIVEN** markdown content is rendered **WHEN** displayed **THEN** base font size is 14px with 1.6 line-height
- **GIVEN** headings are present **WHEN** rendered **THEN** H1=1.75em, H2=1.5em, H3=1.25em with proper spacing
- **GIVEN** lists are present **WHEN** rendered **THEN** they have 16px left padding and 8px between items
- **Tag:** unit (verify CSS typography)

### UC8 — No Regression in Existing Functionality
- **GIVEN** a streaming response occurs **WHEN** chunks arrive **THEN** the live status updates correctly (state, tok/s, preview text)
- **GIVEN** a diff is displayed **WHEN** rendered **THEN** file links still open in Eclipse editor via `open-in-editor:` URL
- **GIVEN** a code block is present **WHEN** user clicks copy button **THEN** text is copied to clipboard (with fallback for file://)
- **GIVEN** the chat is cleared **WHEN** clear button is pressed **THEN** all messages are removed and live status is hidden
- **Tag:** integration (verify end-to-end behavior)

---

## Open Questions

### OQ1 — Code Block Filename Detection
**Question:** Should we auto-detect filename from code block metadata (e.g., ` ```java filename="Foo.java" `), or only show language?
- **Recommended:** Only language for now; filename detection adds complexity without clear value
- **Impact:** JS `highlight` function signature unchanged

### OQ2 — Icon Size Reduction
**Question:** For the compact input area, should we reduce all icons to 16x16, or keep 20x20 but with tighter spacing?
- **Recommended:** Use 16x16 icons with 24px button height — matches UC6 test scenario and provides meaningful space reduction
- **Impact:** Java widget layout parameters; icon assets (no new assets needed, existing icons scale down)

### OQ3 — Header Bar Agent Name
**Question:** Should the header bar show the active agent name (e.g., "Dev Agent") next to the token readout, or stay minimal?
- **Recommended:** Keep minimal — agent name is visible in the mode selector combo below
- **Impact:** No change to HeaderBarWidget

### OQ4 — Dark Theme Support
**Question:** Should we add a dark theme toggle, or stick with light-only for now?
- **Recommended:** Light-only for Phase 1; dark theme is a separate feature that requires SWT color coordination
- **Impact:** CSS `@media (prefers-color-scheme: dark)` not added yet

### OQ5 — MSHTML Target Compatibility
**Question:** Is MSHTML (legacy IE engine) still in the target Eclipse matrix, or is WebView2 the minimum supported renderer?
- **Recommended:** Confirm with user — if WebView2 is required, the MSHTML caveats in "Browser Compatibility Notes" are moot and can be simplified
- **Impact:** Determines whether the `@supports` progressive enhancement pattern is sufficient or if additional legacy-IE handling is needed

### OQ6 — Accessibility: TOOL/THINK Text Color
**Question:** Current TOOL/THINK text uses `#ccc` (1.61:1 contrast) which fails WCAG AA for text. Should we darken to `#999` (~2.8:1) now, or document as intentional de-emphasis?
- **Recommended:** Keep `#ccc` for now — the current styling is intentionally de-emphasized (11px italic, muted), and the role is also shown by position/border. Document the exemption in Phase 5.
- **Impact:** No CSS change; Phase 5 documentation only

---

## Implementation Order

1. **Phase 1: CSS Overhaul + JS Hooks** — Update chat.html with:
   - New message styling (using `::before` for borders)
   - Code block headers (JS logic change in highlight callback)
   - Glassmorphism live bar with progressive enhancement (`@supports`)
   - Typography upgrades
   - **Streaming pulse:** Add `.streaming` class toggling in JS (add on first chunk, remove on completion) + CSS `@keyframes pulse-border` (opacity-only animation)
   - Copy button hover reveal (CSS `opacity` transition)
2. **Phase 2: Java Compact** — Reduce input block dimensions (16x16 icons, 24px buttons), tighten spacing in all widgets
3. **Phase 3: Manual Test Page** — Create `test-chat.html` in `/resources/chat/` with:
   - Buttons to trigger test scenarios: `appendMessage({role: "USER", message: "..."})`, `appendMessage({role: "AI", message: "..."})`, `updateLiveResponse(...)`, `appendDiff(...)`, `clearMessages()`
   - Pre-populated examples for each message type (markdown, code blocks, diffs)
   - Purpose: Phase 3 verification without needing full Eclipse runtime
4. **Phase 4: Browser Compatibility Testing** — Test across Eclipse platforms:
   - Windows: Verify WebView2 vs MSHTML fallback behavior (OQ5 confirmation needed)
   - macOS: Verify WebKit support for `backdrop-filter` and `:has()`
   - Linux: Test pre-existing `:has()` usage on target WebKitGTK version; document any regressions
   - Verify progressive enhancement: base styles work everywhere, `@supports` enhancements apply where supported
   - Verify streaming pulse animation works in all platforms
5. **Phase 5: Documentation** — Update `docs/interaction-design.md` with new visual specs, browser compatibility notes, and accessibility exemptions (decorative borders)

---

## Success Criteria

- [ ] AI messages have premium visual treatment (gradient background, thicker border via `::before`, better padding)
- [ ] AI messages pulse (opacity animation) during streaming, static after completion
- [ ] User messages remain distinct but compact (border via `::before` to avoid border-radius clipping)
- [ ] Code blocks have language indicator in top bar (JS logic change in highlight callback)
- [ ] Live status bar uses glassmorphism with progressive enhancement (base solid bg + `@supports` blur override)
- [ ] Input block is 20-30% more compact (16x16 icons, 24px button height)
- [ ] Typography is more readable (14px base, 1.6 line-height)
- [ ] All existing functionality works identically
- [ ] No regression in streaming, diffs, code copy, file links
- [ ] Works across Windows/macOS/Linux Eclipse platforms (Phase 3 testing catches pre-existing `:has()` issues on legacy WebKitGTK)
- [ ] Progressive enhancement pattern works correctly: base styles apply everywhere, `@supports` enhancements apply where supported (OQ5 confirmation recommended)
