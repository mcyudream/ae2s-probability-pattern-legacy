# AE2S Probability Pattern Legacy

将 [TaoLe-si / Probability-Pattern](https://github.com/TaoLe-si/Probability-Pattern)
`1.0.1` 移植到 **Minecraft 1.12.2 / Cleanroom 0.4.0 正式版 / Java 25**。
AE2 实现为[应用能源 2 超巨星（AE2S / Supergiant）](https://www.mcmod.cn/class/28382.html)。

Mod ID：`ae2s-probability-pattern-legacy`。
[源码仓库](https://github.com/mcyudream/ae2s-probability-pattern-legacy) ·
[问题反馈](https://github.com/mcyudream/ae2s-probability-pattern-legacy/issues) ·
[正式版下载](https://github.com/mcyudream/ae2s-probability-pattern-legacy/releases/latest) ·
[CI](https://github.com/mcyudream/ae2s-probability-pattern-legacy/actions/workflows/ci.yml)

概率处理样板会根据成功率、置信度和本次订单的实际需求量计算尝试次数。支持多层概率合成、
已有中间库存扣除、有线编码终端、无线编码终端和 AE2S 万用终端。

## 安装

从[正式版](https://github.com/mcyudream/ae2s-probability-pattern-legacy/releases/tag/v1.0.2)下载
`ae2s-probability-pattern-legacy-1.0.2.jar`，客户端和服务器均需安装。
必装 AE2S；本次实际验收版本为 `1.1.0+a47dfe029`，安装包 SHA 和固定源码见
[依赖锁](dependency-lock.json)。AE2UEL、原版 AE2 1.12.2 不在本次支持范围内。

可选安装 HEI / JEI 进行配方导入。可选安装 **Ponder Legacy 1.4.0** 及其前置
**CraftTweaker 4.1.20.698** 查看中英演示；缺少 Ponder 时全部核心功能仍可使用。
无线功能直接使用 AE2S API。

## 使用

1. 将概率样板编码终端装在线缆上，保持 ME 网络供电并提供频道。
2. 切换到**处理模式**，填写机器**单次尝试**的输入，以及成功时首个目标产出的数量。
3. 在左侧填写真实成功率，例如 `0.8` 或 `80%`；点击置信度按钮切换 **95% / 99%**。
4. 放入 **AE2S 空白样板**并点击编码。将生成的概率样板放入面向机器的样板供应器。
5. 把实际机器产物送回同一 ME 网络，然后正常请求合成。每层先扣除现有中间库存，再独立计算概率用料。

一个概率样板只承诺首个产出；其他副产物可以正常回网存储。成功时每次产出多个物品的配方也可编码。
置信度是每一层的统计指标；随机少产仍可能使 CPU 等待产物，此时可补足产物或取消订单后重试。
大订单沿用上游的正态近似策略，置信度不是整条合成链的联合保证。

HEI / JEI 导入会保留输入量和首个产出；对于暴露 `chance`、`probability` 等常用字段或 getter 的
配方包装器，自动填写成功率。其余配方可以手动填写。

无线概率终端需充电，并在 **AE2S 无线访问点**中绑定网络。它可通过 AE2S 的通用合并机制加入
万用终端，保留编码格、样板、成功率和置信度。升级卡规则由 AE2S 管理。

安装 Ponder 后，在这三种物品的提示框中按住 Ponder 键即可观看演示。

## 构建和验证

```bash
./tools/dev.sh build reobfRuntimeHarnessJar
```

使用 Java 25。脚本优先读取 `PROBABILITY_JAVA_HOME`，其次使用工作区已有 JDK。
编译 SDK 为 Cleanroom `0.7.14-alpha`；最低运行版本和实际打包验收均为 **0.4.0 正式版**。
AE2S 与 Ponder 的编译依赖会校验哈希，运行库不打入本 Mod。

| 文件 | 用途 |
| --- | --- |
| `ae2s-probability-pattern-legacy-1.0.2.jar` | 安装到 `mods/` 的 SRG 成品 |
| `ae2s-probability-pattern-legacy-1.0.2-mcp.jar` | 开发依赖 |
| `ae2s-probability-pattern-legacy-1.0.2-sources.jar` | 源码 |
| `build/verification/statpatterns-verification.jar` | 独立验收工具，只用于测试 |

自动验收入口：`python3 tools/verify.py`。需已准备 Minecraft 1.12.2 运行资源和 Java 25，客户端需可用的
`DISPLAY`；可用 Xvfb。使用 `--with-hei` 加测 HEI 配方导入。

完整结果与截图见[验收记录](verification/README.md)，平台差异见[迁移说明](docs/MIGRATION.md)。

CI 使用 Temurin 25.0.5+8 构建、运行单元测试，并在 Xvfb 下重跑四组正式 Cleanroom 0.4.0
验收，包含 HEI 和中英文 Ponder。推送与包内版本一致的 `v*` 标签后，只有全部检查通过、
且安装包 SHA-256 与已验收记录一致时才创建正式 GitHub Release。发行说明见
[1.0.2](docs/releases/1.0.2.md)；发布门禁和产物收集入口分别为
`python3 tools/verify_package.py --require-verified-hash --tag v1.0.2` 和
`python3 tools/prepare_release.py --tag v1.0.2`。

## 来源和许可证

上游固定提交：`40e0c918b2ea8c9e595fafece951db50f5120262`。
原作者 TaoLe-si，移植维护 yudream。代码和上游资源沿用 **LGPL-3.0-or-later**，见
[LICENSE](LICENSE)、[NOTICE](NOTICE.md) 和 [upstream-lock.json](upstream-lock.json)。

