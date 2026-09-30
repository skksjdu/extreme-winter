# 1.2.1 验证报告

2026-09-30，本地单人验证；未上传 GitHub。成品：`outputs/extreme-winter-1.2.1.jar`。
Minecraft 1.21.6 / Fabric Loader 0.19.5 / Fabric API 0.128.2+1.21.6 / Java 21。

- `build` 通过；16 项单元测试（10 项温度、6 项配置）通过，0 失败、错误或跳过。
- 最终正式 JAR 的基础回归 A 通过，2m 34s；光影组合 D 通过，2m 52s。每组运行 7 个游戏测试入口，覆盖初始物资、便携热源、火把/阶段、雪地移动、温度/HUD、旧热源与存档、自然世界及悬停界面。
- D 使用 Sodium 0.7.3 / Iris 1.9.6 / Complementary Reimagined r5.9.3，启动日志和 Iris 公共 API 均确认光影启用。
- 七种便携热源的全新/恢复满物品隐藏时间条；有消耗时显示并随恢复变化，普通石剑的耐久条和悬停保持原样。实际 `ItemStack.getTooltip` 验证中英文耐久、用途、回收和冷却说明；资源重载遮罩退出后捕获真实鼠标悬停界面，已目视检查。
- 普通地面火把和墙上灵魂火把露天到期后移除方块，每个准确产生 1 个原版物品实体；其时间组件恢复满后可再次放置。默认暴露时限 45 游戏秒，地面/背包约 5 游戏秒恢复满。
- 未恢复满的火把真实 `BlockItem.place` 被拒绝，物品数量不减；D 另验证普通火把的原版挖掘掉落保留时间组件，且不依赖方块实体。
- 火把在背包中运行 40 tick，已用时间从 45 降至 27；掉落物运行 120 tick 后恢复满。火把方块计时保存重进后保留，玻璃屋顶下暂停。
- 两个可见营火的取暖强度大于单个；墙体挡住后强度为 0，单个火把比营火弱。默认火把权重 35%，累计强度上限 4；纯模型验证更高强度带来更快恢复。
- 真实服务器将日历从第 2 个完整游戏日调到第 3 个完整游戏日，其他条件相同时失温量为原先的 1.1 倍。阶段随世界时间保存；单元测试覆盖阶段边界、上限、禁用、负时间、有顶及浸水。默认每 3 天增加 10%，最多 10 阶段，即 2 倍失温；热源回温不减慢。
- 营火实测：创造模式且冻结昼夜时，40 tick 内已用时间从 20 到 22；树叶遮蔽后暂停，移除树叶并改生存后继续到 24；保存重进后移除玻璃遮挡，30 继续到 32。
  **未复现用户报告的少数营火不耗损，不能确认特定存档或创造模式是原因。** 屋顶/树冠、游戏暂停和未加载区域本就停止计时；悬停说明明确提示遮挡规则。没有宣称已修复未能复现的根因。
- 之前的回归保持通过：同一客户端连续创建两个存档各领取且只领取 1 个营火，原版出生点、重复进入不补发、安全屋资源缺失；营火回收及食材掉落、炉子时钟/燃料、背包恢复、保存重进均正常。
- 积雪下陷、掉落合并、慢行和跳跃回归通过；步行速度比为 0.85708，地面/雪地跳跃最高点均 1.25220 格。自然世界测试继续确认树叶沿用原版资源。
- 包检查通过：74072 字节，29 个 Java 21 类，四个通用 Mixin；无测试类、内嵌第三方 JAR、可选模组硬依赖、安全屋或原版视觉覆盖。
  SHA-256：`3b87524b8f862c630eed5dbf966635752725871acf00dec3fcc00895f6fc67d9`。

日志：`work/test-1.2.1-A-final.log`、`work/test-1.2.1-D.log`；单元报告：`build/reports/tests/test/index.html`；包检查：`outputs/package-1.2.1.json`。
截图：`outputs/heat-bars-1.2.1-vanilla.png`、`outputs/campfire-tooltip-1.2.1-zh.png`、`outputs/campfire-tooltip-1.2.1-en.png`、`outputs/furnace-tooltip-1.2.1-zh.png`、`outputs/torch-tooltip-1.2.1-zh.png`、`outputs/campfire-tooltip-1.2.1-complementary.png`。
Complementary 仍记录既有 `BIOME_SULFUR_CAVES`、`endFlashIntensity/endFlashFactor` 和旧 `stone_slab variant` 警告；测试器的空音频设备选项及离线登录警告保留。语言重载时曾有一次服务器落后 50 tick 的提示，全部断言通过；本次未出现此前 Eclipse 的 OpenGL 错误。

已安装到指定实例：`D:\迅雷下载\PCL 正式版 2.12.7.3\.minecraft\versions\1.21.6-Fabric 0.19.5\mods\extreme-winter-1.2.1.jar`。
安装前确认该游戏进程退出；构建、成品和安装文件哈希一致，实例仅保留 1 个本模组 JAR。其他三个模组、`options.txt`、两份配置及光影 ZIP 哈希不变。
旧版 1.2.0 备份：`work/install-backup-1.2.1-20260930-095323`；安装清单：`outputs/installation-1.2.1.json`。旧配置未重写，缺省的新字段自动采用默认值。
只验收单人；未测试专用服务器/多人、用户各个已有存档、其他 GPU 或末地光影效果。已有世界按原版日历直接进入相应寒冷阶段；睡觉和时间命令会改变阶段。

# 1.2.0 历史验证报告

2026-09-30，本地单人验证；未上传 GitHub。成品：`outputs/extreme-winter-1.2.0.jar`。
Minecraft 1.21.6 / Fabric Loader 0.19.5 / Fabric API 0.128.2+1.21.6 / Java 21。

- `build` 通过，14 项单元测试通过，0 失败、错误或跳过。最终正式 JAR 的基础回归 A 通过，2m 9s。
- 光影组合 D 通过，1m 52s，使用 Sodium 0.7.3 / Iris 1.9.6 / Complementary Reimagined r5.9.3；日志与 Iris 公共 API 均确认光影启用。
- 同一客户端进程连续创建两个新存档，分别领取且只领取 **1 个营火**；重复 JOIN 和存档重进不补发，领取不改变玩家位置或世界出生点。
- 普通地形世界也收到 1 个营火；安全屋模板缺失，携带营火不提供取暖效果。包中没有安全屋生成器、蓝图或结构资源。
- 蹲下空手右键收回营火，实际移除方块、保留已用 100 秒的时钟，烹饪中的 1 个牛肉准确掉落一次。
- 熔炉真实掉落及 `BlockItem.place` 保留已用 200 秒的时钟；耗尽的营火放置后立即熄灭。
- 背包中真实运行 40 tick，营火已用时长从 100 降到 92，恢复 8 秒；五种便携热源从耗尽最多 30 次每秒恢复后充满。
- 原版物品条 API 在耗尽时为 0 格、满时为 13 格绿色；部分时间组件同步到客户端，普通石剑的耐久条保留。截图确认显示正常。
- 背包内部分恢复的营火保存重进后仍有消耗，未被重置为满时长；方块时钟、温度及原有热源/燃料保存回归通过。
- 积雪下陷、掉落合并、慢行和跳跃回归通过；步行速度比仍为 0.85708，地面/雪地跳跃最高点均 1.25220 格。
- 包检查通过：60281 字节，25 个 Java 21 类，四个通用 Mixin；无测试类、内嵌第三方 JAR、可选模组硬依赖或原版视觉覆盖。
  SHA-256：`8d3fb24fbcc8dc593f6f05f748fe3e9298745753d5f5e40040bc5d8827acbbfb`。

复查日志：`work/test-1.2.0-A-final.log`、`work/test-1.2.0-D-final.log`；单元报告：`build/reports/tests/test/index.html`；包检查：`python tools/audit_package.py`，结果在 `outputs/package-1.2.0.json`。
截图：`outputs/heat-charge-1.2.0-vanilla.png`、`outputs/heat-charge-1.2.0-complementary.png`、`outputs/spawn-1.2.0-vanilla.png`、`outputs/spawn-1.2.0-complementary.png`；物品时间条、自然出生地及 HUD 已目视检查。
Complementary 保留既有 `BIOME_SULFUR_CAVES`、`endFlashIntensityM/endFlashFactor1`、旧 `stone_slab variant` 警告；主世界测试通过，未出现此前 Eclipse 的 OpenGL 错误。测试器还记录空音频设备选项警告，未修改用户选项。

最终文件已安装至 `D:\迅雷下载\PCL 正式版 2.12.7.3\.minecraft\versions\1.21.6-Fabric 0.19.5\mods\extreme-winter-1.2.0.jar`，哈希与上述成品一致；只替换本模组，核验其他三个模组与三份配置哈希不变。
原始 1.1.2 在 `work/install-backup-1.2.0-20260930-080813`，清单为 `outputs/installation-1.2.0.json`。原配置中的 `starterShelter` 已忽略，缺省恢复时长自动为 30 秒。
本版只验收单人，未做专用服务器或多人连接测试。旧存档已建建筑保留，已有玩家不补发营火；岩浆桶没有物品时间条。

# 1.1.2 历史验证报告

2026-09-30，本地验证；未上传 GitHub。成品：`outputs/extreme-winter-1.1.2.jar`。
Minecraft 1.21.6 / Fabric Loader 0.19.5 / Fabric API 0.128.2+1.21.6 / Java 21。

- `build` 通过；14 项单元测试通过，0 失败、错误或跳过。
- 最终 JAR 基础单人回归 A 通过，1m 35s；覆盖温度/HUD、积雪重力与层数守恒、避难所、热源、保存重进。
- 光影回归 D 通过，1m 41s。组合为 Sodium 0.7.3、Iris 1.9.6、Complementary Reimagined r5.9.3；启动日志与 Iris 公共 API 均确认光影实际启用，室内/室外/HUD 截图已检查。
- 新增真实客户端输入测试：1–8 层雪的玩家碰撞高度为 1/16–1/2 格，薄雪、满层雪、两格堆叠雪均稳定落地，客户端与内置服务器位置一致。
- 相同 30 tick 前进输入，普通地面 6.21617 格，三种雪地均 5.32773 格，速度比 0.85708，即降低约 **14.3%**；多格积雪没有叠加减速。
- 普通地面与雪地跳跃最高点均为 1.25220 格；离开雪地后速度恢复，测试期间未出现窒息或额外冻伤。
- A 组合中，11 种树叶的模型与纹理共 22 项资源在运行时均来自 `vanilla`；D 组合模型仍来自 `vanilla`，纹理由 Sodium 自带的原版贴图副本提供。两组截图均确认原版树叶形状已恢复。
- 包检查通过：63821 字节，24 个 Java 21 类，只有既有炉子 Mixin，没有测试类、内嵌第三方 JAR 或任何原版视觉覆盖。
  SHA-256：`72a3b2b1c15d1001b40f339c82b989281b99fd06d348ebe59cb7446161b33cf5`。

官方 Modrinth API 明确标记这三个文件支持 1.21.6；缓存下载逐个按官方 SHA-512 校验。
发布页：[Complementary r5.9.3](https://modrinth.com/shader/complementary-reimagined/version/Bqen1mJX)、
[Iris 1.9.6](https://modrinth.com/mod/iris/version/Rhzf61g1)、
[Sodium 0.7.3](https://modrinth.com/mod/sodium/version/7pwil2dy)。
Iris/Sodium 文件名中的 1.21.8 不代表排除 1.21.6。

复查材料：`work/build-1.1.2.log`、`work/test-1.1.2-A.log`、`work/test-1.1.2-D.log`、
`work/install-1.1.2-modrinth.json`、`build/reports/tests/test/index.html`；包检查为 `python tools/audit_package.py`。
1.1.2 只重新验证 A/D 单人组合；下方 1.1.1 的完整 A–E 矩阵为历史记录，不能代替新版本实测。

光影日志保留 `BIOME_SULFUR_CAVES`、`endFlashIntensityM/endFlashFactor1` 和旧 `stone_slab variant` 映射警告，主世界画面与所有回归通过。
本次 D 未发现此前 Eclipse 的 `GL_INVALID_ENUM/non-integer format 28`；没有修改任何光影源文件来隐藏警告。
未验证末地相关光影效果、其他 GPU 或用户存档的实际启动。
截图交付：`outputs/visuals-1.1.2-vanilla.png`、`outputs/visuals-1.1.2-complementary.png`、`outputs/shelter-1.1.2-complementary.png`。

已安装至用户指定的 `D:\迅雷下载\PCL 正式版 2.12.7.3\.minecraft\versions\1.21.6-Fabric 0.19.5`：
仅替换本模组，新增 Iris/Sodium/光影包与启用配置，`options.txt` 只移除旧树叶包 ID，逐个核验安装文件哈希。
旧模组和原始 `options.txt` 位于 `work/install-backup-1.1.2-20260930-072007`，安装清单为 `outputs/installation-1.1.2.json`。

# 1.1.1 历史验证报告

2026-09-30，本地验收；未上传 GitHub。安装包：`outputs/extreme-winter-1.1.1.jar`。
Minecraft Java 1.21.6 / Fabric Loader 0.19.5 / Fabric API 0.128.2+1.21.6 / Java 21。
构建沿用 Gradle 9.8.0 / Loom 1.18.2 / 本机 Java 26，系统配置未更改。

## 最终 JAR 运行矩阵

所有运行使用最终 remapped JAR，包含积雪外观回退后的资源。单人内置服务器，Windows / Intel Arc。

| 配置 | 组合 | 结果 | Gradle 运行时长 |
| --- | --- | --- | --- |
| A | Fabric API | 通过 | 1m 9s |
| B | A + Sodium 0.7.3 | 通过 | 1m 8s |
| C | B + Iris 1.9.6，未启用光影 | 通过 | 1m 9s |
| D | C + Eclipse Shader Unstable，实际启用 | 通过 | 1m 15s |
| E | C + Lithium 0.17.0 / FerriteCore 8.0.4 / Mod Menu 15.0.2 | 通过 | 1m 8s |

每组执行体温/HUD、积雪重力、山内基地、露天热源、保存重进和普通地形截图检查。
14 项单元测试通过（8 项体温、6 项配置），0 失败/错误/跳过；新增时长非法值纳入配置测试。

## 新版行为与画面

- 85 体温截图与图标逐像素核对：第一颗为空、第二颗左半为空，右半有火；整排继续从左向右减少。
- 超平坦世界在覆雪岩丘内部生成房间；普通地形种子 20260929 选中天然山坡，位置 36,70,-12，旋转 CLOCKWISE_180。
- 室内约 7×9 格、4 格高，出生有顶且营火供暖；屋顶之外仍有岩体。床、工作台、炉子和吊灯存在。
- 两盏吊灯经过真实 tick 仍有木梁支撑，出生位置方块光照至少 9。
- 结构中没有农田、作物、水源；箱子仅 3 面包和 2 苹果，其余格为空。
- 首次到达、旋转和生成记录可保存；重进不重复传送，不补货。
- 短时测试配置验证营火、灵魂营火、熔炉、高炉、烟熏炉的各自时限，以及玻璃屋顶保护。
- 营火部分计时经过保存重开仍保留，熄灭后可重新点燃获得新周期。
- 炉子熄灭后实际不供暖，不自动续燃，库存输入和燃料保留；加屋顶后可恢复。
- 岩浆源保存重开后继续计时并成为黑曜石，有顶岩浆暂停；正常默认值 3600 游戏秒，即 3 游戏日。
- 最终视觉包只包含 11 种树叶的 22 个模型/贴图文件。雪和木头没有覆盖文件；雪层/重力雪仍引用原版模型。
- 回退积雪时，所有 22 个树叶资源与回退前 SHA-256 完全一致。近距离截图确认雪为原版方形、树叶圆润、云杉木保持原样。

## Eclipse Unstable 实测与限制

使用[官方 Unstable 仓库](https://github.com/Merlin1809/Eclipse-Shader/tree/Unstable)的完整 ZIP，未修改光影源文件。
下载快照提交：[3c18afcfd89c3bbc4ddf1cf3ea9867731ecd4cc7](https://github.com/Merlin1809/Eclipse-Shader/commit/3c18afcfd89c3bbc4ddf1cf3ea9867731ecd4cc7)。
光影 ZIP SHA-256：`ff12a1b35d5f811c0dce6142527f500fc94a0f32c95ced37bd6186a5eb1bb934`。

Sodium 0.7.3 / Iris 1.9.6 的发布 JAR 支持此游戏版本；测试日志和 Iris 公共 API 均确认 Eclipse 处于启用状态。
室内、室外、体温条和圆润树叶截图已检查，没有白屏、缺失材质或测试断言失败。

当前 Intel Arc（驱动 32.0.101.8132）日志反复报告 `GL_INVALID_ENUM` / `non-integer format 28`。
独立基线仅加载 Fabric API、Sodium、Iris 和测试 JAR，明确排除 Extreme Winter 及其视觉包，仍复现相同错误，画面可显示且基线通过。
**因此可确认该日志问题不依赖本模组；不能据此确认更细的光影/驱动根因。**
未实测本机 NVIDIA GPU、其他驱动或 Eclipse 未来 Unstable 提交，不将本次结果视为所有硬件无错误的保证。

## 包检查与性能

成品 85722 字节，24 个 Java 类，字节码 Java 21；只含一个服务端炉子 tick Mixin。
没有测试类、嵌入第三方 JAR、Sodium/Iris 硬依赖、客户端渲染 Mixin、雪/木头视觉覆盖。
SHA-256：`5804f979de500e4e277345d3c2dc7ba106b65c9a6b2c1eddbb6eadf738c02f99`。

热源扫描预热后的单次均值（100 次）：A 51.236 μs，B 47.818 μs，C 49.887 μs，D 46.463 μs，E 44.037 μs。不是整机 FPS 或 TPS 测量。
露天热源每秒只访问玩家附近已加载区块的方块实体；岩浆发现每秒扫描一个已加载地表区块，默认一轮最多约 81 秒。
离线、暂停和区块卸载不补算暴露时长；岩浆长时默认值用同一逻辑的短时配置验证，未实际等待一小时。

## 复查材料

- 最终构建：`work/build-1.1.1-final.log`；矩阵：`work/test-1.1.1-A.log` 至 `work/test-1.1.1-E.log`。
- 无本模组光影基线：`work/test-1.1.1-eclipse-baseline.log`；启动脚本：`work/shader-baseline.gradle`。
- 单元报告：`build/reports/tests/test/index.html`；包检查：`python tools/audit_package.py`。
- 最终截图：`outputs/shelter-1.1.1-eclipse.png`、`outputs/winter-1.1.1-eclipse.png`、`outputs/visuals-1.1.1-eclipse.png`、`outputs/warmth-1.1.1-left-to-right.png`。

仅单人验收，未做专用服务器/多人、所有种子、长时间存档压力和全资源包组合测试。
旧存档基地保留；新基地需要新世界。极端地形无安全已加载位置时仍可能跳过生成。
