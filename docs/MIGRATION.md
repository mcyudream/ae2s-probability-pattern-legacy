# 平台迁移说明

目标为 Minecraft 1.12.2、Cleanroom **0.4.0 正式版**、Java 25 和 AE2S。
固定 AE2S 源码 `a47dfe029305eead8750fbaa7c79a538e1a481ba`，对应实际测试安装包
`AppliedEnergistics2-Supergiant-1.1.0+a47dfe029.jar`。

| 上游机制 | 1.12.2 实现 |
| --- | --- |
| NeoForge 注册、菜单及客户端动作 | Forge 注册事件、AE2S 原生编码容器及其带窗口校验的动作通道 |
| 数据组件和 Codec | 原生 `encoded_processing_pattern` 加 `statistical_pattern` NBT；物品、流体及 long 数量使用 AE2S `GenericStack` |
| 有线概率终端 | AE2S `PatternEncodingTerminalPart` 子类，保留原生存储、配方选择及编码操作 |
| ae2wtlib 无线终端、万用终端 | AE2S 原生无线定义注册、充电、绑定、GUI 工厂及万用终端组合 |
| JEI / EMI / REI | 目标平台使用 HEI / JEI，复用 AE2S 原生导入并补充概率提取与界面避让 |
| GuideME | 原始指南保存在 `docs/guide/`，游戏内新增可选 Ponder 演示 |
| 合成、切石、锻造、处理模式 | 使用 1.12.2 实际存在的原生合成和处理模式；1.21.1 的切石/锻造界面不进入安装包 |

## 合成与持久化

AE2S 的图规划对普通配方使用固定线性用料。检测到含概率样板的合成图时，本 Mod 转入 AE2S 自带的
树规划路径，在扣除已有库存之后，根据每个节点的实际缺口创建不可变的批量样板。
普通配方仍保留原生规划行为。

批量样板的定义带有 `requestedOutput`。它的输入是完整尝试次数对应的用料，输出是 CPU 要等待的实际数量；
CPU 存档后经公开 decoder 恢复同一批次，不把一个批次重新解释为单次配方。
不同批量具有不同身份，防止任务合并时混淆数量。供应器查找及原生供应器的样板存在性检查归一到原始样板，
派发与回收记账继续使用批量详情。

目标成功次数按 `ceil(请求产量 / 每次成功产量)` 计算。30 次成功以内采用精确二项左尾；更大订单保留
上游的正态近似。精确部分使用对数累加和二分搜索，避免很低成功率下的线性循环和 `q^n` 下溢。
数量乘加使用精确 long 运算，超出范围时拒绝该计划。

## 可选依赖与服务端

Ponder API 仅存在于 `compat/ponder`，通过客户端、已加载检测和 IMC 注册。
HEI 特有调用只位于可选插件、客户端 Mixin 和测试辅助类。发布包没有捆绑 AE2S、Ponder、HEI 或测试 harness。

正式 Cleanroom 0.4.0 在专服的 Mod 扫描路径会调用客户端 Kirino 入口。本 Mod 内置与工作区其他移植相同的
兼容修正，只在物理服务端且加载器版本**恰为 `0.4.0`** 时启用，移除这一处调用；没有替换加载器。

Mixin 注解按正式 0.4.0 所带 API 编译。注入目标都是 AE2S 自有成员名，安装包的 Minecraft 方法调用由
CleanroomGradle 重映射为 SRG；本项目不需要 Minecraft 字符串目标 refmap。

