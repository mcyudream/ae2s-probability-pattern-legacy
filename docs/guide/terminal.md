---
navigation:
  title: Probability Pattern Terminal
  icon: statpatterns:probability_pattern_terminal
  parent: index.md
  position: 10
item_ids:
  - statpatterns:probability_pattern_terminal
---

# Probability Pattern Terminal

<ItemLink id="statpatterns:probability_pattern_terminal" /> is a cable-attached terminal for encoding probability patterns. In **Crafting / Stonecutting / Smithing** mode it behaves exactly like the <ItemLink id="ae2:pattern_encoding_terminal" />; in **Processing** mode it encodes a **probability pattern** instead of a regular processing pattern.

<RecipeFor id="statpatterns:probability_pattern_terminal" />

## Slots

| Slot | Purpose |
|------|---------|
| Blank Pattern | A <ItemLink id="ae2:blank_pattern" /> consumed when encoding |
| Encoded Pattern | The resulting pattern |
| Processing Input × 3 | The inputs of a **single attempt** |
| Processing Output | The **expected** output of one attempt |

## Encoding a Probability Pattern

1. Put a <ItemLink id="ae2:blank_pattern" /> in the blank pattern slot
2. Switch to **Processing** mode
3. Fill the inputs with a single attempt's ingredients
4. Fill the output with the expected result of one attempt
5. Set the **success probability**
6. Click **Encode**

### Success Probability

The success probability is the chance that one attempt yields the output. It accepts values from `0.01` (1%) to `0.9999` (99.99%).

### Confidence Level (α95)

The **α95** toggle selects the confidence of the whole batch:

| Toggle | Alpha | Guarantee |
|--------|-------|-----------|
| α95 ON | 0.05 | 95% confidence of reaching the requested amount |
| α95 OFF | 0.01 | 99% confidence |

## Recipe Viewer Support

Recipes can be pulled straight from JEI, EMI or REI:

* The pattern type is auto-selected like vanilla AE2 — a 3×3 crafting recipe becomes a crafting pattern, everything else a processing (probability) pattern
* If the recipe exposes a success rate (a `successProbability`, `probability` or `chance` field/method), it is filled in automatically

## Wireless Variant

With AE2 Wireless Terminal Library (ae2wtlib) installed, a portable version with the usual ae2wtlib upgrades is available:

<RecipeFor id="statpatterns:wireless_probability_pattern_terminal" />
