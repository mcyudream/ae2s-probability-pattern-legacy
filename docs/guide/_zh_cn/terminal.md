---
navigation:
  title: 概率样板终端
  icon: statpatterns:probability_pattern_terminal
  parent: index.md
  position: 10
item_ids:
  - statpatterns:probability_pattern_terminal
---

# 概率样板终端

<ItemLink id="statpatterns:probability_pattern_terminal" /> 是用于编码概率样板的线缆终端。在**合成 / 切石 / 锻造**模式下与原版 <ItemLink id="ae2:pattern_encoding_terminal" /> 完全一致；在**处理**模式下，它编码的是**概率样板**，而非普通处理样板。

<RecipeFor id="statpatterns:probability_pattern_terminal" />

## 槽位

| 槽位 | 用途 |
|------|------|
| 空白样板 | 编码时消耗的 <ItemLink id="ae2:blank_pattern" /> |
| 已编码样板 | 编码产出的样板 |
| 处理输入 × 3 | **单次尝试**的输入 |
| 处理输出 | 单次尝试的**期望**产物 |

## 编码概率样板

1. 在空白样板槽放入 <ItemLink id="ae2:blank_pattern" />
2. 切换到**处理**模式
3. 在输入槽填入单次尝试的原料
4. 在输出槽填入一次尝试的期望结果
5. 设置**成功率**
6. 点击**编码**

### 成功率

成功率即单次尝试产出该产物的概率，取值范围为 `0.01`（1%）到 `0.9999`（99.99%）。

### 置信水平（α95）

**α95** 开关决定整批生产的置信度：

| 开关 | Alpha | 保证 |
|------|-------|------|
| α95 开启 | 0.05 | 95% 置信达到请求数量 |
| α95 关闭 | 0.01 | 99% 置信 |

## 配方查看器支持

可以直接从 JEI、EMI 或 REI 拉取配方：

* 样板类型像原版 AE2 一样自动选择——能放入 3×3 的合成配方编码为合成样板，其余编码为处理（概率）样板
* 若配方暴露了成功率（`successProbability`、`probability` 或 `chance` 字段/方法），会自动填入

## 无线版本

安装 AE2 无线终端库（ae2wtlib）后，可使用带有常用 ae2wtlib 升级的便携版本：

<RecipeFor id="statpatterns:wireless_probability_pattern_terminal" />
