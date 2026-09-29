# Extreme Winter · 极寒生存 1.1.0

面向 **Minecraft Java 1.21.6 / Fabric / 单人世界**的小型冬季生存模组。
核心体验是离开避难所收集资源、逐渐失温，再回到屋顶下或热源旁恢复。

## 安装与开始

1. 使用 **Minecraft 1.21.6**，安装 Fabric Loader **0.19.5**，游戏运行时使用 **Java 21**。
2. 将 **Fabric API 0.128.2+1.21.6** 与 `extreme-winter-1.1.0.jar` 放进该游戏实例的 `mods` 文件夹。同一实例只保留一个本模组 JAR。
3. 创建一个新的普通生存世界。初次进入会出现在出生点附近的小屋内。
4. 使用床设置重生点；检查箱子、农田与工具，留意饱食度上方的十枚火焰。火焰越少，身体越冷。

成品在 `outputs/extreme-winter-1.1.0.jar`。不要安装 `-sources.jar` 或测试 JAR。
GitHub 下载：[1.1.0 发布页](https://github.com/skksjdu/extreme-winter/releases/tag/v1.1.0)；[原始 1.0.0](https://github.com/skksjdu/extreme-winter/releases/tag/v1.0.0) 保留。
预览：[小屋外观](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/winter-1.1.0-shaders.png)、
[室内与火焰 HUD](https://github.com/skksjdu/extreme-winter/releases/download/v1.1.0/shelter-1.1.0-hud.png)。
无需安装 Sodium、Iris 或光影包；它们是可选项。其他模组也要选择明确支持 1.21.6 的版本。

## 已实现玩法

- 原版主世界生物群系变冷，使用原版降雪和天气。下界、末地与模组生物群系的气候保持原样。
- 只在玩家附近已加载区块中随机采样，增加重力积雪、冻结露天静止水源。
- 雪可跨方块堆积，默认每列最多 **64 层（8 格高）**，可配置到 4096 层，受世界高度限制。
  铲掉底层后，上层像沙子一样掉落；薄雪落地会合并层数。沿用原版雪模型和纹理，支持资源包。
  用普通铲子清理，按层数获得雪球；不会替换作物或机器，也不会冻结含水方块。
- 单人世界的内置服务器计算并保存体温；客户端仅接收自己的体温信息并绘制 HUD。
- 露天、降水、夜间和浸水分别增加热量损失。玻璃屋顶也有效；树冠和洞穴按简单遮蔽模型算作有顶。
- 点燃的营火、灵魂营火、熔炉、高炉、烟熏炉，以及岩浆可以供暖。熄灭的热源无效。
- 热源默认半径 **4 格**，距离越远恢复越慢，使用碰撞射线检查墙体遮挡。多个热源取最强值，不叠加。
- 创造/旁观模式保持温暖；下界和末地不继续降温并逐渐恢复。退出游戏不会重置体温，死亡重生会重置。

| 体温 | 影响 |
| --- | --- |
| 70–100 | 正常 |
| 40–不足 70 | 寒冷，火焰减少 |
| 20–不足 40 | 缓慢 I，每 4 秒受到冻伤 |
| 高于 0–不足 20 | 缓慢 I、挖掘疲劳 I，冻伤加重 |
| 0 | 上述效果，每 4 秒受到 6 点冻伤（3 颗心） |

默认下雪白天，从 100 到 40 约 **2 分钟**；下雪夜间约 **88 秒**。
**低于 40 开始扣血**：每次伤害从接近 1 点逐渐增长，20 时为 3.5 点，0 时为 6 点。
浸水会明显缩短安全探索时间。屋顶下不受天气和夜间惩罚，低于 70 时缓慢恢复到 70；靠近热源可恢复到 100。
这些时间按游戏 20 TPS 计算，暂停时不会扣除体温。

## 初始避难所

9 × 11 格、10 格高的云杉木小屋：石砖基座、原木框架、坡屋顶、门廊、暖色灯笼与石砌烟囱。
包含床、工作台、熔炉、一次性物资箱、营火、照明和有顶农田。
箱子提供少量食物、种子、树苗、木/石工具、8 支火把和 4 块煤，没有铁装或高级装备。
农田有普通水源，照明支持夜间生长；树苗供后续种树。铁、红石、钻石等仍需出门探索。

房屋由可替换的原版 NBT 结构模板生成。世界保存生成结果，重进不会重复造屋或刷新箱子。
入口会补充最多 8 级台阶，适应附近的地面落差。
建筑氛围参考 [SheraNom 的 Simple Spruce Starter House](https://www.planetminecraft.com/project/simple-spruce-starter-house-easy/)；
本项目自行设计蓝图，没有导入他人的建筑文件。新版房屋仅用于新世界，不覆盖旧版房屋。

只在世界第一次开始计时时尝试生成，**已有存档不会被自动插入小屋**。
生成器只搜索出生点附近已加载的安全位置，必要时采用小型架高平台，不挖掉山体或树木。
极端地形找不到安全位置时会跳过并写入日志，避免强行覆盖地形。不会在每次读档时反复尝试。

## 配置

首次启动生成游戏实例中的 `config/extreme-winter.json`，修改后重启游戏。
字段、单位与限制见 [配置说明](docs/CONFIGURATION.md)。不需要配置 GUI 或 Mod Menu。
损坏或不合法的配置会在日志中报告并使用默认值，原文件会保留。
首次从 1.0 升级时，先备份为 `extreme-winter.json.v1.bak`，再升级仍为旧默认值的降温、冻伤、积雪参数。
自定义值和未知字段保留，已升级的配置不会反复改写。

与其他气候/天气模组共同使用时，可关闭 `coldVanillaBiomes` 或 `persistentWeather`。
关闭气候修改后，温暖生物群系仍可能下雨；本模组不会替换渲染器来强制它们下雪。
已经放置的雪和冰不会因关闭采样而消失；原版雪被采样增长时会转为本模组的重力积雪。
重力积雪属于本模组方块，使用含该方块的存档时应保留模组。

## 验证与限制

测试记录、具体模组版本和截图索引见 [验证报告](docs/TESTING.md)。
运行矩阵使用实际打包 JAR，测试代码是独立开发测试模组，不进入成品。

本版本按用户确认的**单人范围**验收，未进行专用服务器/多人连接测试。
没有自写 Mixin、OpenGL 调用、自定义渲染管线或对 Sodium/Iris 内部类的修改。
光影包控制自身的天气、雾和光照；不会为了统一画面与它们争夺渲染控制权。
不保证所有光影、整合包、保护领地插件或所有地图种子兼容。

## 构建与维护

构建工具固定为 Gradle 9.8.0 / Loom 1.18.2。Loom 需要 **Java 25+** 运行 Gradle，
但模组编译、单元测试与 Minecraft 使用单独的 **Java 21** 工具链；成品不要求 Java 25。
本机沿用已有 Java 26 启动 Gradle，项目的 `work/tools` 内有已校验的 Java 21，不修改系统 Java 设置。

```powershell
# 本项目当前机器，可直接执行：
.\scripts\Build.ps1 build

# 开发模式单人回归：
.\scripts\Build.ps1 runClientGameTest

# 正式 JAR 回归（A=基础，B=+Sodium，C=+Iris，D=+光影，E=更多性能模组）：
.\scripts\Build.ps1 runProductionGameTest -PcompatProfile=A

# 编辑房屋 JSON 后重建 NBT（Python 3，无第三方依赖）：
python tools/generate_shelter.py
```

其他电脑需安装构建用 Java 25+ 和 JDK 21；若 Gradle 找不到 JDK 21，传入
`-Porg.gradle.java.installations.paths=C:\你的路径\jdk-21`。无需全局安装 Gradle。
`gradlew.bat build` 输出到 `build/libs`；`scripts/Build.ps1` 将下载缓存限制在项目 `work` 目录。

正式测试 B–E 需要 `work/compat` 中的可选模组，D 还需在其测试实例中启用光影。
这些第三方文件仅用于本地测试，不与模组打包分发。

源码按 `temperature`、`environment`、`shelter`、`network`、`config` 分离，
客户端代码在 `src/client`；房屋蓝图在 `structures/starter_shelter.json`。
接口依据和实现取舍见 [实现记录](docs/IMPLEMENTATION.md)。
