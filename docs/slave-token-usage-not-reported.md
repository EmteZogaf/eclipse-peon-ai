# Slave Token Usage nicht im Header

## Goal

Die globalen Token-Stats im Header (`↑ sent  ↓ received`) aktualisieren sich nicht, wenn Jon's Sklaven
(Da Thinka / Da Mek) arbeiten. Die Session-Totals sollen **alle** LLM-Calls zählen — auch die der Sklaven.

**Status:** 🚧 in design

## IST vs SOLL

**IST:** Header-Stats zeigen nur Jon's eigene LLM-Calls, nicht die der Sklaven.
**SOLL:** Header-Stats zählen alle LLM-Calls (Jon + Da Thinka + Da Mek + Compaction + Search).
**WEIL:** ADR-0004 definiert StreamingBridge als single choke point; `onTokenUsage` ist cross-agent.

## Hypothesen (zu verifizieren)

Die Monitor-Kette sollte korrekt sein:
1. `AIChatView.submitAiJob` → `active.call(msg, this)` (`this` = AIChatView)
2. `AbstractAgent.doCall` → `ToolLoopRequest.builder().monitor(monitor)` (AIChatView)
3. `SmartToolExecutor.run` → `tool.withToolRequest(req)` (JonDelegateTool.monitor = AIChatView)
4. `JonDelegateTool.dispatch` → `slave.call(prompt, this.monitor)` (AIChatView)
5. Slave's `doCall` → `ToolLoopRequest.builder().monitor(monitor)` (AIChatView)
6. `ToolLoopRequest.call` → `bridge.call(model, request, monitor)` (AIChatView)
7. `StreamingBridge.onCompleteResponse` → `monitor.onTokenUsage(usage)` (AIChatView.onTokenUsage)

Mögliche Fehlerquellen:
- **A)** `ConfiguredChatModel.callBlocking` erzeugt eigenen StreamingBridge ohne Monitor?
- **B)** Slave's ToolLoopRequest nutzt `chatModel.callBlocking` statt `bridge.call`?
- **C)** TokenUsage vom Provider ist null/fehlend für Slave-Calls?
- **D)** `EclipseUtil.runInUiThread` in AIChatView.onTokenUsage blockiert/wird nicht erreicht?

## BDD

```
GIVEN Jon (Peon-PO) ist aktiv und der Header zeigt ↑ 100k  ↓ 50k
WHEN Jon dispatcht Da Thinka via planWithPlanAgent
AND Da Thinka completes a turn with real token usage (10 input, 5 output)
THEN the header shows ↑ 110k  ↓ 55k (cumulative, cross-agent)

GIVEN Jon dispatcht Da Mek via buildWithAgent
WHEN Da Mek completes a turn with real token usage
THEN the header accumulates Da Mek's usage too
```

## ADRs

(Keine yet — erst nach Verifizierung.)
