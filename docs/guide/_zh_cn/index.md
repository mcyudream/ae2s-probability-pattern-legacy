---
navigation:
  title: 概率样板
  position: 1
---

# 概率样板

**概率样板**是 AE2 的一个扩展，为 AE2 引入**统计型处理样板**。概率样板不假设配方必然成功——它编码**单次尝试**及其**成功率**，由模组规划足够的尝试次数，使你能以选定的置信度获得请求的产物。

主要特性：

* <ItemLink id="statpatterns:probability_pattern" /> — 记录单次尝试、成功率与置信度的样板
* <ItemLink id="statpatterns:probability_pattern_terminal" /> — 让编码概率样板与普通样板一样简单
* **基于置信度的批量规划** — 以 95% 或 99% 的置信度获得至少请求数量的产物
* **链式合成** — 合成树每一层独立计算规模
* **配方查看器支持** — 从 JEI、EMI 或 REI 拖拽配方，并自动提取成功率

## 目录

* [概率样板终端](terminal.md) — 编码、成功率与置信度设置、配方查看器支持
* [概率规模计算](probability-sizing.md) — 所需尝试次数如何计算
