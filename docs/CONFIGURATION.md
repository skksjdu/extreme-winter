# 配置文件

路径：**游戏实例**的 `config/extreme-winter.json`。开发和每个兼容性测试实例有各自独立的配置。
修改后完全重启游戏；不进行运行中热重载。缺省字段沿用默认值，已有文件不会被自动重写。

| 字段 | 默认值 | 单位 / 含义 |
| --- | ---: | --- |
| `minTemperature` | 0 | 严重冻伤温度下限 |
| `maxTemperature` | 100 | 正常温度上限及重生初值 |
| `coldThreshold` | 70 | 低于此值显示寒冷；室内被动恢复上限 |
| `slownessThreshold` | 40 | 低于此值获得缓慢 I |
| `fatigueThreshold` | 20 | 低于此值再获得挖掘疲劳 I |
| `baseLoss` | 0.06 | 每游戏秒，露天基础降温 |
| `weatherPenalty` | 0.04 | 每游戏秒，露天降水额外降温 |
| `nighttimePenalty` | 0.03 | 每游戏秒，露天夜间额外降温 |
| `waterPenalty` | 0.6 | 每游戏秒，浸水额外降温，包括室内水 |
| `recoveryRate` | 1.2 | 每游戏秒，最强热源的基础恢复量，乘距离系数 |
| `shelteredRecoveryRate` | 0.03 | 每游戏秒，无热源的干燥室内被动恢复量 |
| `heatSourceRadius` | 4 | 方块距离，允许 1–6；墙体遮挡有效 |
| `damageIntervalSeconds` | 5 | 最低温时冻伤间隔，允许 1–3600 游戏秒 |
| `freezingDamage` | 1 | 每次伤害，1 点 = 半颗心，允许 0–20 |
| `snowIntervalTicks` | 20 | 积雪采样间隔，允许 20–72000 tick |
| `freezeIntervalTicks` | 40 | 结冰采样间隔，允许 20–72000 tick |
| `samplesPerPass` | 16 | 每个采样批次的总列数，允许 1–64，不随玩家数相乘 |
| `simulationRadiusChunks` | 4 | 玩家周边采样半径，允许 0–8 区块，仅处理已加载区块 |
| `maxSnowLayers` | 3 | 本模组增加雪层的上限，允许 1–8；不会削减既有更厚雪层 |
| `persistentWeather` | true | 主世界开始时及每 60 游戏秒延长原版降水，不触发雷暴 |
| `coldVanillaBiomes` | true | 通过 Fabric API 降低原版主世界生物群系气温并启用降水 |
| `starterShelter` | true | 只在新世界首次启动时尝试放置避难所 |
| `snowAccumulation` | true | 开启本模组的额外积雪采样 |
| `waterFreezing` | true | 开启本模组的额外水源结冰采样 |

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
方块实体、非空气目标、屋顶下、水中及过亮位置仍受保护。模组生物群系的气候不会自动修改，
只有实际寒冷的生物群系接受额外雪/冰采样；体温系统则在整个主世界生效。
