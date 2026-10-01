# 26.0.7 配置说明

实例 `config/extreme-winter.json`，修改后完全重启。所有速率按 20 tick 为一游戏秒，温暖度不是摄氏温度。

v1/v2 首次升级先校验原文，备份为 `.v1.bak`/`.v2.bak`，再将仍为旧默认的参数迁移。阈值组和伤害组整体迁移，定制组保留，禁止冻伤的 0 值保留。未知字段、coldModdedBiomes 和显式无限雪 0 保留。非法原文不修改并记录可见错误，回退安全默认；再次启动不会重复迁移。备份冲突不会覆盖旧备份。

当前用户确认两格雪的目标配置在 `outputs/extreme-winter-26.0.7-target-config.json`，coldModdedBiomes=true。这里只生成交付文件，尚未应用到真实实例。

| 字段 | 默认值 | 含义 |
| --- | --- | --- |
| `weatherMode` | `scheduled` | scheduled 计划天气 / legacy 旧持续降水 / vanilla 原版控制 |
| `blizzardParticles` | `true` | 仅额外偏斜原版雪花，最多每 5 tick 四个，不改变危险 |
| `blizzardWind` | `true` | 仅原版风声资源，遵循天气音量，屋顶下较弱 |
| `blizzardHaze` | `true` | 暴雪轻白雾屏幕色罩，可独立关闭；不改变世界三维雾或光影管线 |
| `blizzardFrost` | `true` | 低温时淡霜边，可独立关闭，中心视野保持 |
| `structureSupplies` | `true` | 加载战利品时仅向九个指定原版内置表追加补给；重启或重载后生效，不影响已打开箱 |
| `configVersion` | `3` | 迁移标记；v2→v3 先备份原文，保留未知字段和定制值 |
| `minTemperature` | `0.0` | 温暖度下限 |
| `maxTemperature` | `100.0` | 温暖度上限和重生初值 |
| `coldThreshold` | `70.0` | 寒冷显示及室内恢复上限 |
| `slownessThreshold` | `25.0` | 低于此值缓慢 I |
| `fatigueThreshold` | `10.0` | 仅 miningFatigue=true 时低于此值附加挖掘疲劳 |
| `baseLoss` | `0.04` | 每游戏秒露天基础损耗 |
| `weatherPenalty` | `0.015` | 每游戏秒露天降水损耗 |
| `nighttimePenalty` | `0.01` | 每游戏秒露天夜间损耗 |
| `waterPenalty` | `0.2` | 每游戏秒浸水损耗，屋内仍生效 |
| `recoveryRate` | `1.2` | 每秒有效热强度乘此回温值 |
| `shelteredRecoveryRate` | `0.3` | 干燥无热源庇护每秒回温，至 coldThreshold |
| `heatSourceRadius` | `4` | 普通热源半径 1—6；距离/墙体遮挡生效 |
| `damageIntervalSeconds` | `10` | 预警后伤害脉冲间隔秒 |
| `damageThreshold` | `25.0` | 冻伤预警温暖度阈值 |
| `minimumFreezingDamage` | `0.5` | 伤害脉冲最低值，生命点 |
| `freezingDamage` | `1.0` | 最低温暖度时伤害脉冲，生命点 |
| `snowIntervalTicks` | `80` | 额外积雪采样间隔 tick；树冠为四倍 |
| `freezeIntervalTicks` | `200` | 额外水源结冰采样间隔 tick |
| `samplesPerPass` | `16` | 每批候选总数 1—64 |
| `simulationRadiusChunks` | `4` | 玩家附近已加载区块半径 0—8 |
| `maxSnowLayers` | `16` | 天气额外新增整列层数上限；0 无限；不削减旧雪、不限制手动堆放/重力合并 |
| `persistentWeather` | `true` | legacy 模式的持续降水开关；scheduled 由统一控制器接管 |
| `coldVanillaBiomes` | `true` | 原版主世界群系静态降温 |
| `coldModdedBiomes` | `false` | 模组主世界群系静态降温；Terralith 可开启，迁移保留实际开关 |
| `snowAccumulation` | `true` | 仅额外积雪采样开关 |
| `waterFreezing` | `true` | 仅额外源水结冰采样开关 |
| `outdoorHeatExtinguishing` | `true` | 耐候总开关，分类开关同时生效 |
| `campfireExposureSeconds` | `600` | 营火累计露天耐候秒 |
| `soulCampfireExposureSeconds` | `900` | 灵魂营火累计露天耐候秒 |
| `furnaceExposureSeconds` | `240` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `blastFurnaceExposureSeconds` | `300` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `smokerExposureSeconds` | `150` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `lavaExposureSeconds` | `3600` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `heatRecoverySeconds` | `30` | 背包内耐候条恢复秒；不是燃料 |
| `torchExposureSeconds` | `45` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `torchRecoverySeconds` | `5` | 仅对应分类机制开启时使用，单位为游戏秒 |
| `torchHeatStrength` | `0.35` | 火把相对热强度 |
| `maxHeatStrength` | `4.0` | 多源叠加上限 |
| `winterStageDays` | `3` | legacy 兼容字段，独立时钟不再使用 |
| `winterStageLossIncrease` | `0.1` | legacy 兼容字段，独立时钟不再使用 |
| `maxWinterStages` | `10` | legacy 兼容字段，独立时钟不再使用 |
| `minimumColdHealth` | `6.0` | 本模组冻伤生命下限；不补血，不影响其他伤害 |
| `damageWarningSeconds` | `60` | 低于 damageThreshold 累计预警；回到阈值+2 清除 |
| `respawnProtectionSeconds` | `180` | 新生/死亡重生冻伤保护；重登不刷新 |
| `miningFatigue` | `false` | 本模组挖掘疲劳开关 |
| `torchWeathering` | `false` | 火把过冷掉落/冷却；关闭可清理旧物品组件并允许正常放置 |
| `furnaceWeathering` | `false` | 原版炉具受冷停工；关闭自动清除旧 BLOCKED |
| `lavaCooling` | `false` | 额外岩浆源冷却；关闭停止推进，不还原黑曜石 |

阈值满足 min < fatigue < slowness < cold <= max；所有值必须有限。损耗 0—100，伤害 0—20，minimumFreezingDamage <= freezingDamage。关闭伤害请同时设两个伤害字段为 0。

额外雪冰只处理已经加载的随机候选列，不替换作物、机器或含水方块；原版雪冰和世界生成仍可能发生。雪保留旧方块 ID，已超上限的雪停止天气新增，铲底后下落、合并仍守恒。

关闭火把/炉具机制后旧物品的无效耐候条和冷却文案隐藏；旧炉具在正常 serverTick 自动解锁，不丢输入和未使用燃料。营火耐候条和未来炉具燃料条是不同规则。关闭岩浆额外冷却不扫描全地图或还原已冷却方块。

寒潮独立运行时钟保存在主世界 extreme_winter_world。新旧世界首次缺少此数据从 0 初始化。旧 temperature、snow_drift、heat_exposure_seconds、starter_heat_received 等标识保持。创造/旁观玩家温暖度安全，原版其他危险正常。睡觉及时间命令不改变进度。


天气固定规则：首次 85 分钟预警 / 90 分钟暴雪 / 93 分钟退潮 / 95 分钟缓和；以后按保存的种子在暴雪结束后 45—75 分钟再来一次，持续 5—7 分钟，预警 5 分钟/退潮 2 分钟。正常缓和与降雪窗口 10—20 分钟。准备期额外雪采样 25%、额外冻结 0；普通降雪 100%、暴雪 200%；缓和期额外新增 0。不扩大采样半径/候选数，不改静态群系温度。原版雨雪有渐入渐出，缓和不会清理旧雪。

`persistentWeather=false` 的旧配置在缺少 weatherMode 时读取/迁移为 vanilla；显式 weatherMode 优先。配置 v3 缺少新字段时在内存采用缺省，不反复改写。服务端定时发送独立 WinterStatusPayload，winterStage 与 temperature 危险 stage 分开；客户端不能提交状态。

保暖和补给不新增可变默认速率：内衬/食物只缩放空气项，浸水独立；热水袋与炖菜使用全存档运行时钟，放箱继续计时，暂停/退出停钟。热水袋14400tick/充热200tick/补热.03，炖菜6000tick空气倍率.8。structureSupplies 缺省 true，不改变关键物品合成路径；关闭后重启或重载生效，不重刷已开箱。
# 26.0.6 家园规则

取暖炉与冬季农业使用当前新手固定规则，不增加复杂配置：炉具半径6、源强1.25，原木/木板/木棍/煤与木炭/煤块分别1600/400/200/9600/86400 tick。温室需屋顶、光≥9、农业热量≥.25；深冬/长冬露天生长机会为.5/.25。骨粉保持原版，`data/extreme_winter/tags/block/winter_crops.json` 定义参与作物，资源包/资料包可按原版扩展对应资源。玩家热源总上限仍由既有 maxHeatStrength 控制。
