---
navigation:
  title: Probability Pattern
  position: 1
---

# Probability Pattern

**Probability Pattern** is an AE2 extension that adds **statistical processing patterns**. A probability pattern does not assume a recipe always succeeds — instead it encodes a **single attempt** together with its **success probability**, and lets the mod plan enough attempts to reach your requested amount with a chosen confidence.

Main features:

* <ItemLink id="statpatterns:probability_pattern" /> — a pattern that stores one attempt, its success probability and its confidence level
* <ItemLink id="statpatterns:probability_pattern_terminal" /> — encodes probability patterns as easily as normal patterns
* **Confidence-based sizing** — get at least the requested amount with 95% or 99% confidence
* **Chain crafting** — every level of the crafting tree is sized independently
* **Recipe viewer support** — drag recipes from JEI, EMI or REI with automatic success-rate extraction

## Table of Contents

* [Probability Pattern Terminal](terminal.md) — encoding, success probability and confidence settings, recipe viewer support
* [Probability Sizing](probability-sizing.md) — how the required attempt count is computed
