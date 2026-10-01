# 26.0.7 表现与正式回归

E01已实现并通过实际渲染：5个原创16×16物品像素图及5个炉具/气象仪贴图，沿用小型原版模型和资源包替换方式。BlizzardOverlay用原版GUI渲染轻白雾色罩与淡霜边，在客户端tick平缓接近目标，中心区域不加霜；不修改世界三维雾参数或Iris管线。blizzardHaze/blizzardFrost独立开关，原有粒子/风声开关保持。效果放在HOTBAR之前，HUD文本保持前景；F1/创造/旁观不显示。屋顶下白雾变弱，风声仍使用WEATHER，方块音效仍由原版BLOCK声道处理。

天气倒计时在两次服务器包之间按本地运行tick逐秒递减，暂停停钟，收到包校准。天气固定于左上角，小窗口及原版下马提示均不遮挡；温暖计仍位于饱食/骑乘条上方，三行坐骑生命条时额外上移以避开原版下马提示。visual-3已实际确认骑乘、水下及隐藏HUD截图。

扩展回归源码已编译：WinterBenchmarkTest的测试包Mixin只计真实服务端模组回调、炉具、农业门控和成长任务，嵌套只计一次；辅助类位于test.metrics，不能放在Mixin保留包test.profile。计时类/钩子不入发行JAR。基准固定种子/视距/场景，并以1200实际tick观测准备开始、真实60分钟与270分钟边界；边界前30秒由夹具跳转，随后自然运行，不宣称连续等待4.5小时。实际场景4个区块逐一检查FULL，并仍等待所有渲染/光照队列完成；默认Fabric测试API要求整圈方形视距区块，与原版圆形传输区域不一致，不能拿永远等不到的视距角块当渲染失败。

WinterLegacySaveTest已由保留的26.0.2正式JAR创建原版100天旧存档，再以26.0.7打开副本；源夹具保留。WinterUpgradeTest在JOIN捕获旧温暖度42，避免随后准备期70下限掩盖旧附件读入；旧背包、箱子、物资收据和独立手册收据实际升级通过。

WinterSurvivalRegressionTest已实际验证饥饿、摔落、僵尸AI和溺水四次死亡重生，个人生产收据/原版进度保持；原版粉雪冻结伤害及五秒去重、点燃完整下界门往返、末地进入/原版通关界面/正常关闭返回主世界和共享冬季时钟通过。首轮测试停在首次末地返回的原版通关界面，读当前EndPortalBlock字节码确认seenCredits路径后补上真实WinScreen.onClose交互，未改游戏机制或绕开断言。

benchmark-3完成三段各1200实际tick，60/270分钟真实参数自然跨边界；其AFK30FPS样本弃用于视觉比较。benchmark-4只复测四个视觉窗口，MINIMIZED解除AFK限帧，平均FPS下降7.75%，有效场景模组CPU p95最高.42ms。固定小场景及分段观测不表示大型基地、连续4.5小时或长时间内存保证；详细原始/聚合记录在outputs/beginner-26.0.7-performance.md/json。五组正式JAR各19完整入口，以及两组Terralith/Eclipse实际光影矩阵均已通过。封存证据在outputs/package-26.0.7.json及最终验证记录。

---

# 26.0.6 生产与家园实现

HeatingStoveBlockEntity 使用原版已加载方块实体 tick 和单槽 WorldlyContainer。剩余燃烧 tick 拆为两个 unsigned short 同步，支持 72 分钟煤块而不溢出。拆除通过原版容器副作用返还未燃燃料，HeatDropMixin 将剩余进度写入独立 persistent/network 组件；重新放置恢复。owner UUID 仅作成长归属，不限制其他人使用。离线、暂停、卸载没有追赶计算。

HeatSourceIndex 使用弱 ServerLevel 键，按已加载 chunk 保存热源位置集合，load/unload 与热源状态变化维护索引。位置查询共同执行距离衰减、已加载射线路径及碰撞遮挡；热量/屋顶结果缓存至多 40 tick、4096 项，燃烧或拆源即时失效。不会为查询加载区块。玩家弱火把/岩浆查询保持有界低频扫描，农业只接受营火、燃烧炉具与取暖炉。

WinterFarming 只在七类作物的原版 randomTick 入口按阶段概率门控：深冬 .5、长冬 .25；符合屋顶、光照和有效热量要求时概率 1。骨粉、树苗与其他维度不接入。WinterTasks 由实际放置、成品取走、回温、连续充热、暴雪经历结束和实际作物掉落实体驱动七个原版进度。成熟作物掉落只在原版破坏/浆果使用上下文内记录，不将计算掉落表或种子视为收获。个人收据持久化并 copyOnDeath，32 份温室收获、自己炉具已燃烧、暴雪经验和长冬组成家园条件。

WinterCycleTest 逐项消费初始夹具材料和实际收集的树木/作物：原版树苗生成、砍树、熔炉烧木炭、供暖、随机增长、收获重种、烤马铃薯、真实进食、脚本逐 tick 外出/返回与原版补给箱转移，最后保存重进。随机增长尝试和补给箱固定表种子是明确的夹具加速，不代表实等作物数小时或在自然村庄徒步发现。手册用原版阅读界面逐页渲染中英十二页，并以实际字体换行高度检查是否截断。

---

# 26.0.5 保暖与远征实现

WinterGear 注册 persistent/network-synced insulation、bottle_filled、bottle_charge_ticks、bottle_last_charge_tick、bottle_expiry_tick；食物效果为独立玩家 persistent 到期附件。保持旧组件/收据。内衬特殊配方复制完整原护甲，只置一个标记；RepairInsulationMixin/AnvilInsulationMixin 在成功原版修理结果追加标记，锻造升级由原版完整组件复制。

空气损耗公式：[(露天基础+天气+夜间)×天气倍率×(1−.15皮革权重−.35内衬权重)×食物倍率 + 浸水]×寒潮倍率；食物倍率为 .8 或1，热水袋为额外 .03/秒，不重复缩放浸水或源恢复。权重和数值范围在纯模型测试覆盖。

HotWaterBottleItem 只对手持装水袋计连续200个存档运行tick；近距离可见营火，用持久绝对到期tick决定14400tick剩余热量，箱子/掉落无需后台逐件扫描。重登仅清部分充热，不改满热期限。WarmingStewItem 复用原版 Consumable/UseRemainder，额外热量和到期刷新在成功食用后执行。

SurvivalManualItem 复用 WrittenBookContent/原版阅读界面，服务器先广播更新再发打开包；独立收据在旧物资后追加手册，保留原有快捷栏。仪器读取当前实际规则。WinterSupplies 只追加九个 VANILLA 来源池，外部覆盖或配置禁用不触碰；开箱原版即清除 lootTable receipt，因此不重复填充。

---

# 26.0.4 天气实现

WinterWeatherModel 是纯确定性规则；WinterWeatherController 是唯一天气写入入口，legacy/vanilla/scheduled 互斥。WinterEnvironment 的旧 onLoad/60 秒强制降水路径已移除，只按控制器给出的阶段和天气倍率计算预算间隔。

独立 WinterWorldState 保存 weatherRandomSeed、eventNumber、nextBlizzardTick、blizzardDurationTicks、windowNumber/windowKind/windowEndTick、weatherKind/weatherStartTick/weatherEndTick 及已有 elapsedTicks。未来排期由保存 seed+event 决定；首次 90 分钟/3 分钟，完成退潮后确保一个缓和窗口。阶段前后持久化、世界重进与跨维度共享状态已测试。

空气损耗公式：[(露天基础+降水+夜间)×暴雪露天倍率 + 浸水]×冬季倍率。恢复不乘天气/气候损耗倍率。C 批装备/食物只需继续扩展 airMultiplier，避免重复放大浸水项。

WinterStatusPayload 独立同步气候阶段和天气计划；切换立即发、每 200 tick 校准。客户端 SnowstormEffects 只使用原版雪花和风声资源，不接管 Iris/Sodium 渲染器，不在温暖模组群系显示额外雪花。表现开关不影响服务端规则。

GameTest 可用 `-PwinterTests=WinterWeatherTest` 明确选择少量入口；不传此参数就生成完整入口表，不修改正式 JAR。失败日志保留，包工具要求输入日志 BUILD SUCCESSFUL 才允许封存。

---

# 26.0.3 新手基础实现

新增 WinterWorldState（独立 SavedData ID / schemaVersion / elapsedTicks / winterStage），共享主世界时钟。END_SERVER_TICK 仅有存活非旁观玩家时加一；玩家保护与原版冻结去重按 tick 持久化。temperature 附件 ID 保持，死亡不复制温暖度及保护计时，重登不重置。

Hypothermia 采用 25/27 迟滞，低温 60 秒预警、10 秒伤害脉冲，raw damage 限制在 max(0, health−6)。ServerLivingEntityEvents.AFTER_DAMAGE 集中捕获原版 freeze，五秒内跳过模组脉冲，自身脉冲不会重复触发。短状态自然过期，不清除其他状态。

配置源版本先校验再迁移，备份原文，保留未知字段；阈值/伤害组一致性检查。热源分类开关与总开关共同判断。旧 BLOCKED 在炉具 tick 解锁，旧失效火把组件在库存清理，岩浆关闭时不发现/推进。

TemperaturePayload.stage 保留危险级别，新增趋势、原因、预警/保护秒；服务端一秒计算并同步，客户端只展示。独立冬季阶段不混用危险级别。旧天气保留到 B 批统一接管。

---

# 26.0.2 / Minecraft 26.1.2

Current target: Minecraft **26.1.2**, Fabric Loader **0.19.5**, Fabric API **0.155.3+26.1.2**.
Gradle 9.8.0 and Loom 1.18.2 are retained. Use the unobfuscated
`net.fabricmc.fabric-loom` plugin, normal `implementation` dependencies and `jar`.
The locally installed JDK 26 compiles with `--release 25`; game metadata requires Java 25+.
Yarn and remapping tasks are removed. Client and common source sets remain separate.

26.0.2 adds `coldModdedBiomes=false`, an independent opt-in for non-Minecraft
Overworld biome namespaces. The existing `coldVanillaBiomes` switch keeps its
meaning and default. Both selections use Fabric's `foundInOverworld` boundary,
so Nether and End climates remain intact. No world data or biome IDs are replaced.
The user's Terralith Yellowstone biome previously retained temperature 0.24775,
making persistent precipitation rain and rejecting snow/ice sampling; enabling
the new option applies the same -0.5 climate as the vanilla winter biomes.

26.0.1 ports the tagged 26.0.0 MVP to the exact Minecraft 26.1.2 target. Common
and client gameplay source, identifiers, configuration defaults and save formats
are unchanged. The newer game and Fabric API compile with the existing code.
Package auditing now compares all versioned dependencies with `gradle.properties`;
compatibility preparation defaults to that same game target. Original 26.0.0
outputs and the `mvp-26.0.0` tag remain intact.
Comparing the final JAR with the MVP confirms all 31 gameplay classes and every
asset are byte-for-byte identical; only `fabric.mod.json` and the manifest's
Minecraft version differ. The comparison is recorded in
`work/port-26.0.1-content-comparison.json`.

26.1 API adaptations include `Identifier`, `ServerLevelEvents`, `END_LEVEL_TICK`,
`PayloadTypeRegistry.clientboundPlay`, `GuiGraphicsExtractor`, record-based `ChunkPos`,
`WeatherData`, the Overworld clock, namespaced `SavedDataType`, and the drop-method
`ItemInstance` descriptor. Saved data retains the default-namespace legacy names
`minecraft:extreme_winter_lava_cooling` and `minecraft:extreme_winter_torch_cooling`.
Existing attachments/component identifiers and configuration fields are retained.

Shelter checks require center coverage and >=7 covered columns in a 3x3 area.
At most 10 heightmap lookups per check (center plus nine columns), using loaded
chunks only. Temperature uses the air above the player's feet to exclude snow
underfoot; heat wear checks above the source block. Glass and broad foliage are
recognized. No wall/room-volume simulation. Solid roofs still block snow and ice;
foliage permits the sparse snowfall described below. Ice keeps the direct-sky constraint.

The 26.0.0 MVP adds `CanopySnow`: an extra downward lookup in the same loaded
sampled column finds the forest floor even when snow covers the leaves. Only the
`minecraft:leaves` tag, air and snow above those leaves are permeable; solid roofs
remain blocking. Extra canopy growth runs every fourth snow sampling pass. Default
`maxSnowLayers=0` imposes no thickness cap; explicit positive legacy/user limits
remain supported. Uncapped growth skips the old downward layer-count walk and
ends at occupied space or world height. Vertical foliage scans stay within world
bounds and one already-loaded column, without an artificial scan-height limit.

`CanopySnowflakes` uses Fabric `ClientTickEvents.END_LEVEL_TICK` and native
`SNOWFLAKE` particles, at most four nearby candidates per five ticks. Snowflakes
only spawn in rainy, cold Overworld foliage columns with air at the spawn point.
The native particle API respects particle settings and optional renderers; there
are no new shaders, textures, renderer hooks or mixins. Disabling additional snow
also disables these particles. [MVP baseline and extension rules](MVP.md).

HUD uses Fabric's public API and vanilla GUI sprites. Terrain and falling snow
use vanilla models/renderers. Four common mixins remain scoped to exact vanilla
heat items and gameplay; no client render mixins or direct OpenGL calls. Optional
mods and shaders are test-only, checked against live release metadata and SHA512
under `work/compat/26.1.2`. The port tests retain the baseline optional component
versions that explicitly support 26.1.2. Test instances have separate 26.1.2 paths
and do not reuse 26.1 or 1.21.6 saves.

Primary references:
- [Fabric 26.1.2 porting documentation](https://docs.fabricmc.net/26.1.2/develop/porting/)
- [Fabric 26.1 changes](https://fabricmc.net/2026/03/14/261.html)
- [Official mapping migration](https://wiki.fabricmc.net/tutorial:migratemappings)
- [Fabric API version metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)
- [Iris 1.10.9](https://modrinth.com/mod/iris/version/MwcLS51S)
- [Sodium 0.8.9](https://modrinth.com/mod/sodium/version/uGvVQBnw)

Runtime results and limitations: [TESTING.md](TESTING.md).

---

# 1.21.6 historical implementation record

# Extreme Winter — implementation notes

Target: Minecraft Java Edition **1.21.6** only. User clarified that this is a
singleplayer mod; release acceptance uses an integrated server, not dedicated
server/multiplayer testing.

## Verified dependencies (2026-09-29)

| Component | Pin | Evidence |
| --- | --- | --- |
| Minecraft | 1.21.6 | User requirement |
| Game / bytecode | Java 21 | Fabric setup documentation; local Temurin 21 |
| Fabric Loader | 0.19.5 | `meta.fabricmc.net/v2/versions/loader/1.21.6`, stable |
| Fabric API | 0.128.2+1.21.6 | Official Maven metadata, latest exact 1.21.6 suffix |
| Yarn | 1.21.6+build.1 | Official Meta API for 1.21.6 (Yarn stable flag is false) |
| Loom | 1.18.2 | Official Maven stable release; remap plugin supports obfuscated MC |
| Gradle | 9.8.0 | Official current stable endpoint; Loom requires >=9.7 |

Loom 1.18.2 runs on Java 25+; Gradle uses the already installed Java 26 on this
machine. Minecraft, compilation, and tests use a separate Java 21 toolchain.
Neither system PATH nor system JAVA_HOME is changed. Wrapper and distribution
SHA-256 values are pinned and verified.

Sources:
- https://fabricmc.net/2025/06/15/1216.html
- https://docs.fabricmc.net/develop/loom/
- https://meta.fabricmc.net/v2/versions/loader/1.21.6
- https://meta.fabricmc.net/v2/versions/yarn/1.21.6
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.18.2/fabric-loom-1.18.2.module
- https://services.gradle.org/versions/current

## Architectural decisions

- Keep common gameplay and client code in separate Loom source sets.
- Use Fabric persistent data attachments for per-player temperature; no player
  NBT mixin. Death resets temperature; logging out does not.
- Use server tick events with bounded scheduled work. No client gameplay authority.
- Use normal status effects with short lifetimes. Never remove another mod's
  effects when temperature recovers.
- Use vanilla blocks, placement checks and weather; 1.2.0 leaves spawn placement entirely to vanilla.
- Use `HudElementRegistry`, the API introduced for 1.21.6. No renderer replacement.
- Use Fabric's public biome modification API to set vanilla Overworld biomes to
  a cold, precipitation-enabled climate. This changes data, not biome classes or
  renderer code, and lets vanilla and shader packs render normal snow. Modded
  biomes retain their original climate by default; `coldModdedBiomes=true` opts
  their Overworld climates into the same change. `coldVanillaBiomes=false` disables the vanilla
  change for climate/world-generation modpacks; warm biomes may then show rain.
  Vanilla light-based snow/ice melting is retained.

## Development log

Each phase must compile before the next phase starts. Runtime assertions and
screenshots are produced by a separate Fabric client test mod, excluded from the
release JAR. Test outcomes are recorded in `TESTING.md` after execution.

| Phase | Implementation and relevant API | Validation performed |
| --- | --- | --- |
| 1 | Split Loom main/client source sets; exact 1.21.6 dependencies | Empty project built, real client started |
| 2 | `AttachmentRegistry` + persistent `Codec.DOUBLE`; default on death | Built; real singleplayer save/rejoin preserved value |
| 3 | `ServerTickEvents.END_SERVER_TICK`, heightmap cover, weather/time/water inputs | Built; exploration timing, cover, bounds and stage unit tests |
| 4 | Finite sphere of precomputed offsets, loaded-chunk checks, vanilla collision raycast | Built; real lit/unlit campfire, obstruction and recovery assertions |
| 5 | Vanilla status effects and `DamageSources.freeze()` | Built; actual effect application/expiry and periodic health loss |
| 6 | Typed S2C `CustomPayload`, `ServerPlayNetworking`, `HudElementRegistry` | Built; owner HUD sync, reconnect, disconnect, rendered screenshots |
| 7 | Fabric biome data modification, world tick events, vanilla snow layer placement | Built; layer cap, crop/block-entity/roof protection and unloaded-column assertions |
| 8 | Loaded surface sampling, source-water checks, vanilla ice | Built; outdoor freezing and glass-roof protection assertions |
| 9 | Vanilla `StructureTemplateManager`, NBT blueprint, `PersistentStateType` + codec | Built; new-world generation, first arrival, heat/farm/chest, persistent marker and no loot refill |
| 10 | Gson already supplied by Minecraft; readable JSON, validated defaults | Built; four config tests including preserving malformed user files |
| 11 | Independent test mod, then Loom `ClientProductionRunTask` | Actual remapped release JAR tested in A–E singleplayer matrix |

Configuration defaults were introduced alongside the temperature model so systems
share one set of values; JSON loading/validation was added in phase 10.

## Work bounds and compatibility tradeoffs

- Temperature work is staggered by entity ID, once per 20 ticks per active player.
  A radius-4 heat sphere contains 257 offsets; maximum allowed radius 6 contains
  925. Offsets are constructed once. Only actual heat-source candidates need rays.
- Environmental sampling has one shared per-world budget, not a full scan per
  player. Defaults are 16 columns each 20 ticks; freezing shares the sampled
  columns every 40 ticks. Sampling never loads a missing chunk.
- Shelter generation and its templates were removed in 1.2.0. A persistent, copy-on-death
  player receipt grants one campfire on the first join of a new save, independently for
  each save. Legacy arrival receipts and nonzero play-time prevent upgrades from adding
  a second starter kit. No code changes world spawn or teleports arriving players.
- `POST_PROCESSING` is Fabric's documented phase for changing biome properties.
  Climate/weather settings are explicit switches because another climate mod may
  wish to own those same properties. No rendering classes are replaced.
- Heat line-of-sight uses collision shapes, so windows and walls obstruct it, while
  gaps or an open doorway can admit heat. Visible source strengths add up to a configured
  cap (4 by default); torches contribute 35% before distance attenuation. There is no
  room-volume simulation.
- Snow and ice obey ordinary block light and vanilla melting. Natural vanilla
  weather ticks remain active in addition to this mod's small sampling budget.
- Effects refresh for 40 ticks and expire naturally, preserving stronger/longer
  vanilla or mod effects. Client networking has no C2S temperature channel.

## Test-launch correction

The initial development-mode D run failed before loading a world because Loom's
local mod remapping strips nested JARs from Iris, and the file dependency had no
Maven metadata to restore its `jcpp` library. The test stack reported
`NoClassDefFoundError: org/anarres/cpp/PreprocessorListener`.

The acceptance matrix therefore uses **production-mode** launches with the intact
published optional mod JARs and the actual `remapJar` output. This also tests the
deliverable's Minecraft name remapping. The test mod is remapped into
`build/testmods`, and it is never included in `build/libs` or the distributed mod.
No Iris internals or mod gameplay were changed to work around the test launcher.

## 1.1.0 changes

- Warmth is ten original 9 x 9 flame sprites above hunger, including half icons. The public
  Fabric HUD API remains unchanged; normal GUI sprites are rendered through Minecraft.
  The row moves above the oxygen bar while underwater and above multiple mount-health rows.
- Default snowy daylight loss is 0.5 units/second; nighttime is 0.68. Freezing starts below
  40, every four seconds, interpolating 1 to 6 health points toward minimum temperature.
- `SnowDriftBlock` extends vanilla `SnowBlock` and implements vanilla `Falling`. Models
  reference vanilla snow textures. Neighbor changes schedule gravity; vanilla falling
  entities handle movement/networking/rendering. No Mixin or custom entity renderer.
  A full drift has full collision height, which prevents the next falling block from
  incorrectly trying to replace its support. Thin deposits combine and preserve layers.
  Total column depth defaults to 64 layers; sampling walks only that column.
- The 9 x 11 footprint now has a 10-block-high gabled spruce roof, stripped-log framing,
  stone-brick base, covered porch, lanterns and chimney. Furniture/loot/farm anchors stay
  compatible with the existing first-arrival logic. Old world shelters are never replaced.
  Visual inspiration: [SheraNom's Simple Spruce Starter House](https://www.planetminecraft.com/project/simple-spruce-starter-house-easy/).
  The blueprint was authored locally; no downloaded schematic is included.
- Legacy JSON defaults migrate once, retaining customized values and unknown fields;
  the original is backed up before writing. Broken files remain untouched. Rates, damage
  and accumulation remain configurable.

## 1.1.1 changes

- The half flame now empties on its left, matching the row's left-to-right depletion.
- Mountain shelter interior: 7 x 9 usable floor, four-block clearance, no farm, and only
  three bread/two apples in the chest. Persistent rotation has a NONE default for old saves.
- Campfires/furnaces persist elapsed outdoor seconds on their own block entities using
  Fabric attachments. Every second only nearby loaded chunks' block entities are visited.
  Exposure pauses under cover; relighting a campfire resets its clock.
- A single common-side `AbstractFurnaceBlockEntity.tick` HEAD injection prevents automatic
  fuel ignition while weather-blocked. Extinguishing reads back the complete existing NBT,
  zeroing only active burn time; input, remaining fuel, output, recipe XP and components are
  retained. Adding a roof releases the block. There are no client/renderer injections.
- Source lava is discovered from one already-loaded surface chunk per second. A world
  PersistentState stores source positions and exposure seconds; unloaded/roofed sources
  pause, absent sources are discarded. Default 3600 seconds produces obsidian, including
  progress across save/rejoin. Flowing lava and non-Overworld dimensions are excluded.
- The default-enabled built-in resource pack overrides only 11 leaf models and textures.
  It uses vanilla JSON cuboids/biome tinting and has no Sodium/Iris dependency. Snow models
  and textures were reverted to vanilla per user review; wood assets remain untouched.
- Eclipse Unstable is tested from the official branch ZIP, without modifying its sources.
  The separate ShaderBaselineTest excludes Extreme Winter to investigate driver messages.

## 1.1.2 changes

- Remove the built-in foliage pack and its generator/registration. All eleven leaf species
  resolve to vanilla models/textures again; snow still references vanilla assets.
- Use cached player-specific collision shapes at half the visible snow height (1/16 to
  1/2 block). `dynamicBounds()` is required so entity-dependent shapes are not cached as
  a single solid shape. Other entities, including falling snow, keep full-height support;
  stacked snow continues to fall and merge without losing layers.
- Vanilla `velocityMultiplier(0.85f)` adds moderate horizontal drag without resetting
  vertical velocity or applying a lingering status effect. Jumping and gravity remain
  vanilla; walking speed recovers immediately outside snow. Like vanilla soul sand,
  the horizontal drag also applies to other entities contacting the drift.
- `SnowMovementTest` uses real client movement input and checks client/server sinking,
  thin/full/stacked snow, jump height, walking speed recovery and vanilla leaf resources.
- Optional shader setup uses Complementary Reimagined r5.9.3, Iris 1.9.6 and Sodium 0.7.3.
  Exact official Modrinth 1.21.6 compatibility metadata and SHA-512 hashes are checked.

## 1.2.0 changes

- Remove the starter shelter generator, persistent world state, template/tag, blueprint
  and generator tool. Existing built terrain is untouched; a legacy player arrival
  receipt is still registered for migration. Vanilla spawn selection is unchanged.
  Starter receipt types are registered during mod initialization, before player save
  data is read, rather than waiting for the first JOIN callback.
- A registered integer data component with a persistent codec and packet codec carries
  elapsed outdoor seconds between campfire/furnace block entities and their item stacks.
  Legacy block-entity attachments remain readable. Unlit states retain elapsed time.
- Common-side BlockItem placement and Block loot-return injections restore/preserve
  the clock. A depleted campfire is immediately unlit; partial recovery cannot be
  converted into a full lifetime by simply placing it again.
- Sneaking with an empty main hand and right-clicking a campfire returns its item with
  the clock intact. Vanilla block removal drops the cooking ingredients; world/player
  modification permissions are checked. Other mining loot behavior is unchanged.
- Once per second, the server restores ceil(exposureLimit / heatRecoverySeconds) seconds
  for each supported item in player inventory, hotbar and offhand. Zero elapsed time
  removes the component so fully charged items stack with unused ones. Dropped items,
  container inventories and offline time do not recover. Carried items do not heat players.
- Item bar methods use vanilla durability width/color calculations (13 steps,
  green-to-red HSV), retaining ordinary weapon durability. There is no GUI renderer
  injection, OpenGL call or optional-mod internal dependency. Lava remains a world
  cooling mechanic and has no bucket charge component.
- StarterAndHeatItemsTest covers two new saves in one process, reopened receipts,
  pickup with cooking food, real loot/placement, depleted placement, real inventory
  recovery, five item bars and client sync, weapon durability and item save/reopen.

## 1.2.1 changes

- Supported items display their normal durability bar only while elapsed exposure is
  positive. Fresh creative/catalog stacks and fully recovered items no longer show a
  permanent full bar. Original non-heat item durability is unchanged.
- The public Fabric ItemTooltipCallback appends localized Chinese/English descriptions
  for campfires, furnaces, normal/soul torches and lava buckets. Percent and remaining /
  total exposure seconds come from the item's synced component. Pickup, recovery and
  cover rules are explained; lava has a source cooling description, not fictitious
  per-bucket durability.
- Normal/soul floor and wall torches provide weak warmth. A position-based PersistentState
  retains outdoor seconds with a chunk index. Chunk-load palette checks only scan sections
  containing torches; one already-loaded nearby chunk per second discovers command/legacy
  torches. Item placement registers immediately. Only nearby loaded chunk indexes advance.
- At 45 exposed seconds a torch block is removed without normal loot, then one vanilla
  ItemEntity with an exhausted clock is spawned. Block loot preserves partial clocks for
  ordinary mining/support loss. Torches recover on the ground or in player inventory in
  about five seconds; a common BlockItem.place HEAD check denies placement until full and
  does not consume the item. Campfire/furnace dropped items still do not recover.
- Loaded torch item entities are tracked through Fabric load/unload events in a weak map.
  Recovery waits for twenty actual entity ticks since its last step; a copied tracked
  ItemStack synchronizes progress. Unticked/unloaded entities and offline time do not
  receive repeated or catch-up recovery. Full/removed items are discarded from tracking.
- Calendar stages use persisted vanilla timeOfDay / 24000, including sleep advancement.
  Every three completed days adds 0.1 to the loss multiplier, capped at ten stages (2x).
  TemperatureManager supplies this multiplier to the pure temperature model only in the
  Overworld; dry shelter remains protected and warming rates are unchanged. Existing
  worlds use their existing calendar days; time commands also alter progression.
- HeatSources sums only visible, distance-attenuated sources and stops at its cap. The
  scan sphere, loaded-region guards and collision ray checks are unchanged.
- Campfire wear has no creative-mode exemption. Runtime comparisons cover creative and
  survival players, frozen daylight, tree cover/removal and save/reopen. Covered and paused
  worlds intentionally stop exposure; the user's unspecified intermittent case has not
  been reproduced, so no particular save or mode is declared the cause.
- WinterProgressionTest exercises real drops, cooldown/placement, ground/inventory recovery,
  stacked heat, walls, calendar loss and persistence. HeatTooltipTest exercises the actual
  ItemStack tooltip callback and mouse hover, language reloads and screenshots. Pure unit
  checks cover stage boundaries/caps, safe shelter, combined heat and invalid config fields.
