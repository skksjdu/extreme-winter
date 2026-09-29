# 1.0.0 验证报告

时间：2026-09-30（Asia/Shanghai）。目标：Minecraft 1.21.6 单人游戏。
**10 项单元测试通过，A–E 五组正式 JAR 单人集成测试全部通过。**

2026-09-30 将原 0.1.0 正式编号为 **1.0.0**，未改玩法；再次构建成功，10 项单测通过，包结构检查通过。
五组游戏矩阵对应原 0.1.0 的相同玩法代码，未因版本号变更重复运行。
发布构建日志：`work/baseline-1.0.0-build.log`；发布校验：`outputs/SHA256SUMS-1.0.0.txt`。

## 交付复核（2026-09-30）

续接任务时核对了上述五组成功日志、JUnit XML 和原版/光影截图；未重复运行游戏矩阵。
本轮执行 `scripts/Build.ps1 build --offline --no-daemon --stacktrace`，45 秒成功，
编译和测试任务使用已有有效缓存（`UP-TO-DATE`），没有新增玩法代码。
随后再次通过 `tools/audit_package.py`，将同一份 40,332 字节成品和三张原始截图复制到 `outputs`，
逐文件校验复制前后 SHA-256 一致，并生成 `outputs/SHA256SUMS.txt`。

原 0.1.0 成品 SHA-256：`63bbf1751db4f467b2f2ab97502875f156ad8b8c4fd9e6bfab3cde5c4243918e`。
正式 1.0.0 成品 SHA-256：`4a66ef56cc257a95b72c345fecb73ac2ac665ef23ce683023d7c1e827c54cb01`。
构建日志：`work/resume-build-isolated.log`。

## 运行矩阵

共同环境：Fabric Loader 0.19.5、Fabric API 0.128.2+1.21.6、Temurin Java 21.0.12.1、
Windows、Intel Arc 图形设备。使用 Loom 的 `ClientProductionRunTask` 加载实际 remapped 模组 JAR，
独立测试模组通过 Fabric Client Game Test API 操作单人世界。无服务器 EULA 修改或专用服务器启动。

| 组 | 可选组件 | 结果 | 本轮运行时长 |
| --- | --- | --- | --- |
| A | 无 | 通过 | 56 秒 |
| B | Sodium 0.7.3 | 通过 | 51 秒 |
| C | Sodium 0.7.3 + Iris 1.9.6，未启用光影 | 通过 | 53 秒 |
| D | C + Complementary Reimagined r5.9.3，实际启用光影 | 通过 | 102 秒 |
| E | C + Lithium 0.17.0 + FerriteCore 8.0.4 + Mod Menu 15.0.2 | 通过 | 57 秒 |

Sodium/Iris 下载文件名含 `mc1.21.8`，发布元数据声明同时支持 1.21.6，
Iris 包内依赖明确列出 1.21.6/1.21.7/1.21.8；本次实际运行的游戏版本为 **1.21.6**，没有迁移版本。
下载文件经过 Modrinth 发布记录的 SHA-512 校验。第三方 JAR/光影包仅用于本地测试，不打包分发。

来源：[Sodium](https://modrinth.com/mod/sodium/version/7pwil2dy)、
[Iris](https://modrinth.com/mod/iris/version/Rhzf61g1)、
[Lithium](https://modrinth.com/mod/lithium/version/XWGBHYcB)、
[FerriteCore](https://modrinth.com/mod/ferrite-core/version/LdlksamY)、
[Mod Menu](https://modrinth.com/mod/modmenu/version/ku5NivOP)、
[Complementary Reimagined](https://modrinth.com/shader/complementary-reimagined)。

## 每组实际执行的检查

受控超平坦世界与普通地形世界各一个，普通地形种子为 `20260929`。
受控世界会保存、关闭、重新打开，检查同一个存档。

- 自动生成小屋；首次进入定位在室内，有屋顶和有效热源。
- NBT 模板中的水、农作物和箱子食物存在；取走食物后重复调用、存档重进都不会补货。
- 重新进入保留玩家离开小屋后的坐标和体温，不被强制传送回初始位置。
- 露天体温缓慢下降；玻璃屋顶阻止露天降温；营火恢复体温。
- 实际墙体阻挡热源；熄灭营火不供暖。
- 低温施加缓慢和挖掘疲劳；最低温周期性扣血；回暖后本模组的短时效果自然过期。
- 体温与 HUD 同步；重进重新同步；断开世界连接后 HUD 数据清空。
- 原版雪层成功放置、增长到配置上限后停止；不会替换作物、阻塞机器顶面或在玻璃屋顶下新增雪层。
- 露天静止水源变成冰；有玻璃屋顶的水源不冻结；未加载的远处区块被跳过。
- 普通地形新世界成功生成小屋；农作物经过实际 tick 后仍存在，每个种植格的方块光照至少为 9。
- A–E 按预期加载对应可选模组。D 通过 Iris **公共 API** 确认光影处于启用状态，日志确认载入指定光影包。
- 检查正常世界内外部截图：HUD 可读，屋内照明、天气和地形绘制正常。D 的画面允许光影包控制雪、雾和光照。

## 单元与包结构检查

6 项体温规则测试：探索时间、露天/室内/浸水区别、被动恢复上限、异常存档数值修复、
阶段边界、热源恢复量。4 项配置测试：默认生成、部分字段配置、非法值回退且文件保留、阈值约束。
全部通过，0 失败，0 跳过。

`tools/audit_package.py` 验证成品：17 个 Java 类，字节码目标 Java 21，Minecraft 依赖精确为 1.21.6；
JSON 资源可解析，结构模板和中文翻译存在；没有测试类、嵌入依赖、自写 Mixin 或可选渲染模组硬依赖。
公共代码通过独立 source set 编译，不依赖客户端渲染类。

## 性能证据及边界

普通地形小屋内，每组调用热源检测 100 次，平均每次：
A 46.839 μs、B 46.078 μs、C 59.217 μs、D 63.910 μs、E 48.695 μs。
这是本机已预热场景的局部测量，**不是全游戏 FPS/TPS 保证或多人负载测试**。

默认每位玩家每秒检测 257 个热源候选位置；不在每 tick 扫描。
环境默认全局每秒采样 16 个表面列，结冰每两秒复用一次采样，不遍历整张地图。
配置硬限制将热源半径控制在 6、每批采样控制在 64。

## 已处理失败与观察到的日志

- 本轮受限构建进程读取 Loom 的 Minecraft 缓存 JAR 时出现 `AccessDeniedException`。
  正常本地权限下使用 `--no-daemon` 单次进程后构建成功；未修改文件权限、系统 Java 设置或项目依赖。
- 早期开发模式 D 启动时缺少 Iris 内嵌的 `jcpp`：本地文件依赖被 Loom 开发映射处理后移除了内部 JAR。
  改用正式 JAR 运行模式后，原包依赖正常加载，D 全部检查通过。失败日志保存在
  `work/dev-iris-nested-dependency-failure.log`，没有通过修改 Iris 或禁用光影掩盖错误。
- 离线开发身份查询 Mojang 皮肤/公钥接口发生超时。单人测试正常完成；没有登录账号或多人验证结论。
- 光影包 r5.9.3 对本游戏/Iris 组合报告部分未知 uniform（`BIOME_SULFUR_CAVES`、`endFlashIntensity`）
  和方块映射警告。光影实际启用，截图与测试均通过；没有修改第三方光影文件或声称日志完全无警告。
- Gradle 提示部分 API 在未来 Gradle 10 中弃用；本项目固定使用已经通过的 Gradle 9.8.0。

## 未验证范围

按用户的单人范围，未测试专用服务器或多人连接；未穷举所有种子、第三方内容模组、资源包和光影包。
死亡复位、跨维度行为基于 Fabric 持久化附件与实现规则，本轮未单独执行死亡重生/传送门回归。
极端地形没有安全位置时会跳过初始小屋，详见安装说明。未进行长时间游玩平衡或超大存档压力测试。

## 可复查材料

- 逐组日志：`work/matrix-A.log` 至 `work/matrix-E.log`。
- 原始截图与测试存档：`work/run-production-A` 至 `work/run-production-E`。
- JUnit XML：`build/test-results/test`；HTML 报告：`build/reports/tests/test/index.html`。
- 依赖下载记录：`work/compat/versions.json`。
- 成品与校验：`outputs/extreme-winter-1.0.0.jar`、`outputs/SHA256SUMS.txt`。
- 预览：[原版雪景](../outputs/winter-vanilla.png)、[光影雪景](../outputs/winter-shaders.png)、
  [光影下的室内与 HUD](../outputs/shelter-hud-shaders.png)。
