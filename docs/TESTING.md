# 1.1.0 验证报告

时间：2026-09-30（Asia/Shanghai）。Minecraft 1.21.6 单人游戏。
**14 项单元测试通过，A–E 五组实际发布 JAR 集成测试全部通过。**

## 运行矩阵

共同环境：Fabric Loader 0.19.5、Fabric API 0.128.2+1.21.6、Temurin Java 21.0.12.1、
Windows / Intel Arc。Loom 的 ClientProductionRunTask 加载实际 remapped 1.1.0 JAR。
测试代码位于独立测试模组，不进入成品。第三方 JAR 和光影包不打包分发。

| 组 | 可选组件 | 结果 | 时长 |
| --- | --- | --- | --- |
| A | 无 | 通过 | 64 秒 |
| B | Sodium 0.7.3 | 通过 | 61 秒 |
| C | Sodium 0.7.3 + Iris 1.9.6，未启用光影 | 通过 | 64 秒 |
| D | C + Complementary Reimagined r5.9.3，实际启用光影 | 通过 | 70 秒 |
| E | C + Lithium 0.17.0 + FerriteCore 8.0.4 + Mod Menu 15.0.2 | 通过 | 66 秒 |

D 的日志确认载入指定光影，测试通过 Iris 公共 API 确认处于启用状态。
可选文件的发布元数据支持 1.21.6，已经校验 SHA-512。
本次运行日志为 work/new-matrix-A.log 至 work/new-matrix-E.log，不是旧版本日志。

## 实际游戏验证

- 新世界生成坡顶小屋，首次进入有屋顶、热源和基础物资；普通地形种子 20260929 生成成功。
- 农作物经过真实 tick 后仍存在，每个种植格的方块光照至少为 9。
- 室外降温、屋顶遮蔽、热源回暖、墙体遮挡、熄灭热源检查通过。
- 35 体温时缓慢扣血，0 体温时扣血明显更多；缓慢、疲劳施加及回暖后过期检查通过。
- 64 层积雪跨格堆积，达到整列上限后停止，保护屋顶、农作物和机器。
- 移除三格雪柱的底格，剩余两格实际下落并重新堆叠。
- 7 层雪落到 5 层薄雪后合并为 8 + 4 层，层数守恒。
- 普通石铲可以采集重力积雪，8 层雪的实际战利品表产生 8 个雪球。
- 露天静止水结冰，有顶水源受保护，未加载的远处区块被跳过。
- 服务端体温同步到火焰 HUD，80 显示八枚完整火焰；截图核对与饱食度对齐。
- 水下截图核对火焰上移，氧气条、饱食度和心形互不遮挡。
- 保存重进保留体温、坐标和避难所状态；箱子不补货，断开连接清空 HUD。
- A–E 每组均执行上述回归，并确认对应可选模组实际加载。

## 单元测试与包检查

8 项体温规则测试：两分钟降到 40、天气/室内/浸水、被动恢复、数据修复、
阶段边界、热源恢复、40 以下递增伤害、自定义温标。
6 项配置测试：缺失生成、部分配置、非法文件保留、阈值验证、旧默认迁移/备份/
保留自定义和未知字段，以及保留禁用冻伤设置。0 失败、0 错误、0 跳过。

tools/audit_package.py 通过：19 个 Java 类，字节码 Java 21，Minecraft 精确为 1.21.6。
结构、中文资源、积雪模型/战利品表和三个 9×9 图标存在。
没有测试代码、嵌入第三方 JAR、自写 Mixin、Sodium/Iris 硬依赖或 OpenGL 调用。

成品 52,564 字节。SHA-256：
cb2be449b10e8cf3c5c0b1c79fd3a2ff2ed1772449785978ecc6938163840a87

## 性能及验证边界

预热后热源检测 100 次的单次平均值：
A 41.195 μs、B 43.299 μs、C 50.429 μs、D 53.387 μs、E 46.236 μs。
这是局部测量，不代表全游戏 FPS/TPS 或多人性能保证。
默认每位玩家每秒检测 257 个热源候选位置，环境每秒全局采样 16 列。
积雪只沿当前列检查层数，重力由邻居更新和计划 tick 触发，不扫描全世界。

开发中修复：原版雪的替换标记和较低碰撞高度会令上层下落后变成物品；
重力雪采用独立注册设置，满层碰撞为一整格，下落和薄雪合并回归通过。
水下 HUD 早期测试让水扩散出清理范围，重进后仍然浸水降温；测试水槽现已封闭并检查
清理后的干燥状态，没有放宽体温存档断言。

光影包仍报告既有 uniform/方块映射警告，实际启用、截图和所有断言正常。
离线身份的 Mojang 皮肤/公钥查询可能超时；Gradle 有未来版本弃用提示，保持固定 9.8.0。

按用户的单人范围，没有专用服务器/多人测试。未穷举种子、内容模组、资源包和光影。
未做长时间平衡、大存档压力或单独的骑乘 HUD 截图测试。
旧存档小屋不被替换；极端新世界找不到安全地点时仍可能跳过小屋。

## 可复查材料

- 单元报告：build/test-results/test；HTML：build/reports/tests/test/index.html。
- 日志：work/new-matrix-A.log 至 work/new-matrix-E.log。
- 测试存档与截图：work/run-production-A 至 work/run-production-E。
- [成品 JAR](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/extreme-winter-1.1.0.jar)，
  [校验文件](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/SHA256SUMS-1.1.0.txt)。
- [原版雪景](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/winter-1.1.0-vanilla.png)、
  [光影雪景](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/winter-1.1.0-shaders.png)、
  [安全屋与火焰 HUD](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/shelter-1.1.0-hud.png)、
  [水下 HUD](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/warmth-1.1.0-underwater.png)。
- [1.1.0 发布页](https://github.com/skksjdu/extreme-winter/releases/tag/v1.1.0)。
- [1.0.0 历史验证报告](https://github.com/skksjdu/extreme-winter/blob/v1.0.0/docs/TESTING.md)。
