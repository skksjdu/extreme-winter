# 配置文件

路径：**游戏实例**的 `config/extreme-winter.json`。开发和每个兼容性测试实例有各自独立的配置。
修改后完全重启游戏；不进行运行中热重载。缺省字段沿用默认值。
1.0 配置首次升级时备份为 `extreme-winter.json.v1.bak`，再迁移仍为旧默认值的参数。
自定义值、未知字段和禁用冻伤的设置保留；`configVersion=2` 后不会重复改写。

| 字段 | 默认值 | 单位 / 含义 |
| --- | ---: | --- |
| `configVersion` | 2 | 配置迁移标记 |
| `minTemperature` | 0 | 严重冻伤温度下限 |
| `maxTemperature` | 100 | 正常温度上限及重生初值 |
| `coldThreshold` | 70 | 低于此值显示寒冷；室内被动恢复上限 |
| `slownessThreshold` | 40 | 低于此值获得缓慢 I |
| `fatigueThreshold` | 20 | 低于此值再获得挖掘疲劳 I |
| `baseLoss` | 0.32 | 每游戏秒，露天基础降温 |
| `weatherPenalty` | 0.18 | 每游戏秒，露天降水额外降温 |
| `nighttimePenalty` | 0.18 | 每游戏秒，露天夜间额外降温 |
| `waterPenalty` | 1.4 | 每游戏秒，浸水额外降温，包括室内水 |
| `recoveryRate` | 1.2 | 每游戏秒，最强热源的基础恢复量，乘距离系数 |
| `shelteredRecoveryRate` | 0.03 | 每游戏秒，无热源的干燥室内被动恢复量 |
| `heatSourceRadius` | 4 | 方块距离，允许 1–6；墙体遮挡有效 |
| `damageThreshold` | 40 | 低于此温度开始冻伤，必须大于最低温；缺省时随缓慢阈值 |
| `damageIntervalSeconds` | 4 | 冻伤间隔，允许 1–3600 游戏秒 |
| `minimumFreezingDamage` | 1 | 刚低于冻伤阈值时每次伤害，1 点 = 半颗心 |
| `freezingDamage` | 6 | 最低温时每次伤害，允许 0–20；必须不小于最小伤害 |
| `snowIntervalTicks` | 20 | 积雪采样间隔，允许 20–72000 tick |
| `freezeIntervalTicks` | 40 | 结冰采样间隔，允许 20–72000 tick |
| `samplesPerPass` | 16 | 每个采样批次的总列数，允许 1–64，不随玩家数相乘 |
| `simulationRadiusChunks` | 4 | 玩家周边采样半径，允许 0–8 区块，仅处理已加载区块 |
| `maxSnowLayers` | 64 | 整列积雪的总层数上限，8 层 = 1 格，允许 1–4096；不削减已有雪 |
| `persistentWeather` | true | 主世界开始时及每 60 游戏秒延长原版降水，不触发雷暴 |
| `coldVanillaBiomes` | true | 通过 Fabric API 降低原版主世界生物群系气温并启用降水 |
| `starterShelter` | true | 只在新世界首次启动时尝试放置避难所 |
| `snowAccumulation` | true | 开启本模组的额外积雪采样 |
| `waterFreezing` | true | 开启本模组的额外水源结冰采样 |
| `outdoorHeatExtinguishing` | true | 主世界露天热源熄灭 / 岩浆源冷却 |
| `campfireExposureSeconds` | 120 | 营火累计露天游戏秒 |
| `soulCampfireExposureSeconds` | 180 | 灵魂营火累计露天游戏秒 |
| `furnaceExposureSeconds` | 240 | 熔炉累计燃烧且露天游戏秒 |
| `blastFurnaceExposureSeconds` | 300 | 高炉累计燃烧且露天游戏秒 |
| `smokerExposureSeconds` | 150 | 烟熏炉累计燃烧且露天游戏秒 |
| `lavaExposureSeconds` | 3600 | 岩浆源累计露天游戏秒，默认 3 游戏日后成为黑曜石 |

1 游戏秒 = 20 tick。所有速率必须是 0–100 的有限数字；温度上下限允许 -10000–10000。
阈值必须满足 `minTemperature < fatigueThreshold < slownessThreshold < coldThreshold <= maxTemperature`。
不满足限制时整份配置回退至默认值并记录错误，原文件保留，便于修复。

减少清雪工作：增大 `snowIntervalTicks`，减小 `samplesPerPass` 或 `maxSnowLayers`。
延长探索时间：减小 `baseLoss`、`weatherPenalty`、`nighttimePenalty`。
关闭额外采样用布尔开关，不要把间隔设为 0。

`snowAccumulation=false` / `waterFreezing=false` 只关闭本模组额外采样；
原版寒冷生物群系在降水时仍会进行原版的积雪与结冰。
`persistentWeather=true` 会在睡觉或天气命令清空降水后，最迟约 60 游戏秒重新维持降水。

数据包可扩展 `extreme_winter:snow_surfaces` 方块标签，使其他模组的普通地表接受雪层。
方块实体、非雪非空气目标、屋顶下、水中及过亮位置仍受保护。模组生物群系的气候不会自动修改，
只有实际寒冷的生物群系接受额外雪/冰采样；体温系统则在整个主世界生效。

积雪使用 `extreme_winter:snow_drift`，每格保持 1–8 层并跨格堆叠。
无支撑时生成原版下落方块实体；薄雪会合并。实体与邻居变化使用计划 tick，不逐 tick 扫描整个雪堆。
普通铲子可快速清理，每层掉落一个雪球。原版纹理引用允许资源包改变雪的外观。

1.1.0 配置无需重写，缺失的新字段自动使用默认值；可手动添加上述字段，时长允许 1–604800 游戏秒。
有顶时暂停计时，区块卸载/离线不推进；计时保存在方块实体附件或世界数据中。熄灭的营火可重新点燃。
炉子被天气熄灭后，需要屋顶才能重新自动点燃，不丢弃库存中的物品或燃料。
默认岩浆发现扫描每秒一块已加载地表区块，不扫描地下、流动岩浆或下界。
内置圆润树叶视觉包可以在游戏资源包菜单中关闭，与这些服务器配置独立。
