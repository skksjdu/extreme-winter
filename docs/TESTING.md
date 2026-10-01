# 26.0.7 新手慢节奏最终验证

2026-10-01（Asia/Shanghai），Minecraft26.1.2 / Fabric Loader0.19.5 / API0.155.3+26.1.2 / 单人。原目录、现有Gradle9.8.0/Loom1.18.2/JDK26 release25。A—E均已实施；未安装真实PCL实例、未发布、未推送、未创建PR。

完整候选JAR矩阵使用同一SHA256 `2fc9c705bd4c48e0783a81a4d470fdc14d7c4c5db33b881f60641fb55ee17059`，每组均完整19个真实客户端/内置服务端入口，不使用winterTests筛选。34个JUnit，0失败/错误/跳过。Iris通过运行时API断言实际开关和当前包名，不能仅凭模组存在或配置文件宣称光影已开。

| 组 | 实际组合 | 结果 | 日志 |
| --- | --- | --- | --- |
| A | 基础Fabric | 19入口完整通过；7m 6s | work/test-26.0.7-A.log |
| B | Sodium | 19入口完整通过；6m 54s | work/test-26.0.7-B.log |
| C | Sodium+Iris，光影关 | 19入口完整通过；6m 50s | work/test-26.0.7-C.log |
| D | Sodium+Iris+Complementary Reimagined r5.9.3，实际开 | 19入口完整通过；6m 57s | work/test-26.0.7-D.log |
| E | Sodium+Iris光影关+Lithium/FerriteCore/ModMenu/Cloth Config | 19入口完整通过；6m 52s | work/test-26.0.7-E.log |

D组合保留了现有Iris/Complementary的`BIOME_SULFUR_CAVES`自定义uniform警告；运行时仍确认Iris active=true和正确包名，自然地形、雪块/树叶及HUD截图通过目视核对。该结果不表示光影包所有群系特效都已逐一验证；没有为消除警告修改第三方包或升级依赖。

另外Terralith2.6.1/Tectonic3.0.28/Lithostitched1.8.0，与独立缓存的Sodium0.9.1/Iris1.11.4及用户Eclipse ZIP组合，coldModdedBiomes=true/false各自启动。两组都实际启用Eclipse-Shader-Unstable.zip。Yellowstone分别为-0.5雪 / 原温度雨，实际400tick后雪层/水源冰/屋顶保护/客户端额外粒子和降水匹配；原版主世界保持冷，Nether/End群系保持原值，重进保留雪。日志为work/beginner-E-terralith-on.log/off.log，原ZIP及真实实例配置未修改。

| 冷群系开关 | 温度 / 降水 | 实际雪层 | 实际冰源 | 光影 |
| --- | --- | ---: | ---: | --- |
| on | -0.5 / SNOW | 578 | 6 | 实际Eclipse启用 |
| off | 0.24775 / RAIN | 0 | 0 | 实际Eclipse启用 |

群系夹具临时使用radius=0、samples=64，并处于暴雪窗口；发行配置未改变。这些计数不代表默认积雪速度。新截图保存在outputs/visual-26.0.7，包含E组合、D实际光影和Terralith开关两组。

19入口覆盖A/B/C/D全部新手机制：配置/三颗心保护/原版伤害、时间日历独立、睡觉/真实暂停/重进、天气排期/真实雪冰、装备及组件保留、热水袋/炖菜、九表补给/外部数据包覆盖、五类自然地形、炉具/漏斗/拆装/燃烧同步/卸载、热源范围/墙体/不加载、农业/骨粉、七项实际进度、生产远征保存闭环、手册中文六页和英文六页共十二张渲染、原创像素资产、视觉独立开关与HUD骑乘/水下/F1。

C组首轮在装备测试退出世界时发生测试框架线程互等；两次线程栈与失败日志保留在work/beginner-E-C-thread-1.txt、thread-2.txt及test-26.0.7-C-stalled.log。装备测试退出前让最后库存/聊天包和旁观转换完成两个tick后再关闭世界，生产JAR哈希未变；A/B已通过结果保留，C/D/E完整续跑通过。未删除断言或改动正式机制。

真实旧存档：保留正式26.0.2 JAR SHA256 d7d8d4f0bf695ae4832d961b8c71746318a8b36aec7a75ffdac72bfd7ee9410c，实际创建100日存档；当前JAR打开副本时世界日历2400000保持，冬季仅3tick准备期，JOIN温暖度42.0、随后准备期正常回温。旧5煤/3苹果/箱中7铁保留；旧物资收据不再发营火，独立手册只发一次。源world-1夹具不改。日志work/beginner-E-legacy-create-1.log、legacy-upgrade-1.log。

实际危险：HARD模式1生命饥饿、6生命30格跌落、4生命原版僵尸AI、4生命预耗尽氧气后原版溺水，都真实死亡并原版重生，个人收获/炉具/暴雪收据及进度保持，温暖/180秒保护重置而不重发物资。无护甲站入粉雪180tick，原版冻结与伤害启动五秒去重，本模组不追加伤害。实际点燃完整黑曜石门，下界往返、末地进入、首次通关界面正常关闭返回；时钟在实际维度旅行继续，无日历跳级。日志work/beginner-E-survival-2.log；首轮未处理原版WinScreen而失败，已保留并修正测试步骤。

真实时间及性能：准备开始和真实60/270分钟边界，每段1200tick约60秒，边界前30秒由夹具设置，不表示连续4.5小时试玩。固定种子20261001/视距8/模拟5/854×480/95作物/8炉场景，有效模组CPU p95最高.42ms（目标≤2ms）；额外视觉开关两轮均值1193.78→1101.27FPS，下降7.75%（目标≤10%）。首轮30FPS限帧数据弃用于视觉比较；另一次CPU最大5.3722ms，不能将p95目标误写为每tick上限。堆峰含原版/测试设施，不证明没有长期泄漏。完整采样、机器信息、方法与局限见beginner-26.0.7-performance.md/json。

封存包198583字节，77个Java25类、12个必要Mixin、10个原创16px贴图；JSON可解析、双语键一致，无测试/计时钩子/第三方模组/光影/原版视觉覆盖。目标配置v3、coldModdedBiomes=true、maxSnowLayers=16，保留审阅过的自定义及未知字段。新视觉开关仅改变表现。安装/变化/范围见release-26.0.7.md，旧26.0.3/.4/.5/.6成品保留。

已知范围：单人自动流程；没有用户主观验收、真实PCL安装、多人/专服、大型整合包/Voxy、长时间内存泄漏压力或连续4.5小时开发者试玩。雪冰/生产/物资夹具加速与自然场景分开，实际交互通过不表示自然漫长寻路已经试玩；袋12分钟/食物5分钟完整等待未追加，实际区间/持久边界已验证。静态冷群系不表示动态暖季，缓和不融旧雪也不能关闭原版雪冰。


---

# 26.0.6 生产与家园验证

2026-10-01（Asia/Shanghai）。Minecraft26.1.2；本轮未安装真实PCL实例，未发布/push/PR。

`work/beginner-D-game-final-2.log`：完整 build、34 JUnit（0失败/错误/跳过）、全部18个真实客户端/内置服务端入口通过，6分48秒。首个全套日志因会话中断停在第16项，Java进程已退出，未计作成功；第二次完整退出码0另存于 `work/beginner-D-game-final-status.json`。

D01—D04集成回归：实际炉具合成/界面/72分钟同步、漏斗、燃料守恒、拆装剩余tick、暂停/真实区块卸载/重进；按区块索引和40tick缓存的范围/遮挡/不加载查询；七类原版randomTick .5/.25抽样、骨粉与温室条件；七项真实操作进度、loot/UI预览不触发、32份收成、暴雪结束、重进。既有A/B/C护甲、天气、五自然地形、补给和新手基础同步通过。

D05完整闭环：原版树苗实际生成6根橡木，生存砍树并拾取；原木合成木板，消费20圆石/4铁合成熔炉与取暖炉；原版炉以3木板烧2原木，410实际tick后取出2木炭。1木炭入新炉并连续充热220tick，剩余9381tick且燃料槽空；随后精确200tick消耗200tick。温室光15、作物热0.900614，27次原版随机增长尝试成熟，实际收获4份马铃薯并重种；烤成马铃薯，与夹具提供的肉/胡萝卜和木板合成碗制作炖菜，实际客户端进食获得6饥饿/暖身效果。

脚本逐运行tick外出48格，确认脱离炉热而携带袋+.03；实际打开固定原版村庄补给表seed1的箱子并以原版槽位转移煤，返回补入炉具。返回剩余9048tick、存档累计979tick、个人收成4；重进保存未用燃料、重种作物、炉剩余进度、收获收据与冬季时钟，无离线追赶。新加载后的方块实体继续正常燃烧，差值限于实际重进tick。

夹具起始明确提供普通工具、20圆石、5铁、3皮革、肉/种子/骨粉/树苗，以及平台/屋顶。作物使用固定种子随机tick加速；移动是按tick的位置脚本；远征箱是固定原版表夹具，未声称在自然村庄徒步发现或实等数小时成熟。12分钟袋/5分钟食物/180或270分钟阶段的完整等待仍未执行，边界与实际区间分别记录。

中英手册全部12页用原版书界面实际渲染，按实际字体114px宽换行，文字高度≤128px；截图在 `outputs/manual-26.0.6`，已目视核验。D英语页面缩短以避免页底截断，保留当前已解锁炉具与温室规则。

正式包：191308字节，76个Java25类，12 Mixin，SHA256 `c4d41d14d17d4776164f8328f0d2c8ad44864b7d583236baad479e161f6c682e`。所有JSON可解析、中英键一致；无测试类/诊断Mixin/第三方包/原版视觉覆盖。`package-26.0.6.json`、JAR、sha256和target-config均在outputs；配置继续保留coldModdedBiomes=true、maxSnowLayers=16。旧26.0.3/.4/.5包保留。

最终E仍待执行：自有像素资产/可关白雾与霜边、正式A—E兼容矩阵、实际Complementary/Eclipse与Terralith额外组、固定场景CPU/FPS/内存、真实时间分段、实际死亡保持/原版危险/跨维度/骑乘、26.0.7最终交付和聚焦源码commit。用户主观试玩独立于自动通过。

---

# 26.0.5 保暖与远征版验证

日期：2026-10-01（Asia/Shanghai）。Minecraft 26.1.2 / Fabric 0.19.5 / API 0.155.3+26.1.2，JDK26 编译 release25。沿用现有工具链。

C01—C06 已实现。新增内衬组件/保留数据配方、运行时钟热水袋、炖菜、六页原版书籍手册、温暖计、可放置气象仪和限定内置表的附加补给。A/B 源码和旧交付均保留；本轮未安装真实 PCL 实例、未推送或发布，源码最终统一提交。

## 已观察验证

- build 与 31 个 JUnit 测试通过，含新三项部位权重、乘法叠加、浸水分离和有限值规则。
- 完整 14 个真实客户端入口通过；未用筛选模式替代此轮全套回归。最终日志 `work/beginner-C-game-final-4.log`，早期失败日志保留。过程中修正两格距离的取点、服务器打开书前组件同步，以及追加手册不改变既有物资快捷栏顺序；真实交互夹具改用原有石铲，不覆盖营火，不以全新耐久石铲匹配已经磨损的物品。
- 护甲：真实工作台合成保留自定义名称、耐久、染色、饰纹、附魔和无关组件；重复内衬无配方。真实工作台合并、铁砧材料修理/两种输入方向合并、锻造台升级并拿取结果，内衬保留。带衬皮革胸甲空气倍率 .80。
- 热水袋：原版合成空袋；实际水源使用和水锅使用，后者只耗一层。实际 5 秒部分充热、离开立即归零、10 秒满热、石墙遮挡、多个只补 .03、存箱/掉落/复制均保留同一到期 tick。保存重进停钟，部分充热归零，到期边界失效；12 分钟到期用同一真实时钟的加速边界，不声称实际等了 12 分钟。
- 炖菜：原版持续进食，实得 6 饥饿值/4.8 饱和度和10温暖度；两个堆叠食用返碗、最后一个转为碗；满饥饿不能开始普通进食；再次食用刷新 5 分钟到期而不累加。五分钟持续期按存档时钟组件及边界检查，未实际静候五分钟。
- 手册：新旧物资收据分开；重复加入和重进不重复给；书+羊毛能再制作，实际打开原版 BookViewScreen。六页中温室/取暖炉标注尚未启用；每次打开刷新后续版本页面。英文阅读页已缩短，避免截图暴露的底部截断；最终首屏渲染已检查。
- 仪器：实际合成温暖计及气象仪、BlockItem 放置和右键。温暖计只报告温暖度/阶段/庇护/有效热源；气象仪从 WinterStatusSync 读取真实服务端计划。
- 九个目标原版内置战利品表逐个实际采样，原物资和附加物资同时存在。平原村庄 6000 个种子样本：追加一次的概率约1/3，燃料/内衬/空袋/炖菜为5:3:1:1，数量区间2—6/1/1/1—2；真实外部数据包替换原表后没有追加；配置关闭重载后九表均不追加；真实已开箱重载不刷物资。具体计数见日志。
- 五个实际生成的固定种子：202610010平原、202610011雪地、202610012沙漠、202610013森林、202610014蘑菇海岛。定位后等待目标区块加载，执行原版生存采集、挖掘自然洞顶、放置实际开局营火；实际两秒回温。没有通过生成平台或赠送稀有物资完成庇护，旧营火和16火把即可使用。属于脚本驱动的生存操作检查，并非用户主观路线试玩；海岛是本种子的蘑菇海岛，不代表所有小海岛都有木材。


---

# 26.0.4 天气、预警与缓和版验证

2026-10-01，Minecraft 26.1.2 / Fabric。权威清单 A01—A09、B01—B04 已实施并验证；C—E 尚未实现，继续开发。原项目目录、旧成品与原用户存档保留；没有安装到真实实例、推送或发布。

统一天气控制器新增 scheduled/legacy/vanilla；默认累计 85 分钟预警、90 分钟首次暴雪、持续 3 分钟、退潮 2 分钟。后续在上次暴雪结束后 45—75 分钟来临，持续 5—7 分钟；所有排期、种子和事件序号保存。普通降雪/缓和按 10—20 分钟窗口交替。暴雪露天空气失温 ×1.5，浸水和恢复不重复放大；独立天气 HUD、有限原版雪粒子及天气音量风声可关闭。

## 已执行的验证

- build、compileGametestJava 通过；28 项 JUnit（12 配置、11 温暖度、5 排期），失败/错误/跳过均为 0。最终构建日志 `work/beginner-B-pause-final.log`。
- 完整真实客户端/内置服务器回归 **11 个入口**通过，3 分 13 秒，日志 `work/beginner-B-game-final.log`。冷群系粒子保护随后由正式 JAR on/off 回归；最新睡觉/暂停断言由完整 WeatherTest 入口补充通过，1 分钟，日志 `work/beginner-B-pause-final.log`。
- 天气规则使用正式时间常量，将独立测试世界运行时钟设到边界检查，不更改发行常量。真实 tick 生成雪方块和水源冰；屋顶保护、列上限、原版降水渐出后缓和保持晴朗、旁观模式停钟、跨维度同一个 SavedData、保存重进不重抽或离线补算均通过。
- **实际睡觉**：原版床入睡后醒到早晨，寒潮只增加实际运行 tick；排期不变。**实际单人暂停菜单**：Minecraft.isPaused 为真，等待 40 客户端 tick 后世界时钟完全不变，再恢复正常运行。
- 正式 JAR 使用独立 Terralith 2.6.1 / Tectonic 3.0.28 / Lithostitched 1.8.0 / Sodium 0.9.1 / Iris 1.11.4 / Eclipse Unstable 实例分别执行开关 on/off。日志均含 `Using shaderpack: Eclipse-Shader-Unstable.zip`，确实启用光影。
- on：Yellowstone 温度 -0.5、客户端 SNOW、实际 597 雪层、9 冰源，屋顶/维度/读档/额外雪花通过，1 分 25 秒；off：温度 .24775、客户端 RAIN、0 雪/0 冰且无额外雪花，1 分 16 秒。日志 `work/beginner-B-terralith-on.log`、`work/beginner-B-terralith-off.log`。测试为可观察性临时 radius=0/samples=64；目标配置仍是 radius=4/samples=16。
- 包结构通过：42 个玩法 Java 25 类、4 个原有 Mixin，没有测试类、嵌套第三方 JAR 或原版视觉资源覆盖。文件 106409 字节；SHA-256 `a980fd8b7c4b8534b6b928f6fc62adc7ae85cd0e2f815401798da4fc037d5167`，证据 `outputs/package-26.0.4.json`。

## 交付与边界

成品 `outputs/extreme-winter-26.0.4.jar`、同名 sha256、目标配置 `outputs/extreme-winter-26.0.4-target-config.json`。目标配置保留用户 coldModdedBiomes=true 与明确 maxSnowLayers=16，不应用到真实游戏实例。代码默认 coldModdedBiomes=false。

缓和停止本模组额外雪冰采样，不清理旧雪；静态群系降温与原版雪冰仍可能发生，没有实现动态暖季。HUD 平时每 10 秒校准倒计时。B 的跨维度检查验证共享状态，不等于玩家真实跨维度旅程。

最终 E 尚需性能 p95/FPS、正式 A—E 全矩阵及 Complementary、实际参数长期分段、粉雪方块流程、其他原版危险与真实玩家跨维度、骑乘等检查。未收到用户主观试玩反馈。失败尝试保留；首次睡觉夹具因修改日历后立即睡觉尚未更新原版环境而失败，等原版环境推进两 tick 后通过，没有修改玩法计时来迁就测试。

安装前备份实例配置与世界，仅安装对应正式 JAR。游戏依然是 Minecraft 26.1.2 / Loader 0.19.5 / Fabric API 0.155.3+26.1.2 / Java 25+。已有版本和目标配置保留。当前 A/B 源码均未提交，等待完整 C—E 结束按项目约定做一次聚焦提交。

---

历史报告（不作为新版验证）：

# 26.0.3 新手基础版验证与交付

2026-10-01，Minecraft 26.1.2 / Fabric，基线 848b261。全部工作在原项目及 work 独立实例，真实用户实例/存档未修改，未推送/发布。旧成品及 mvp-26.0.0 标签保留。

- 构建及 GameTest 源码编译通过；22 项 JUnit 通过（11 配置、11 温暖度规则），失败/错误/跳过均 0。
- 完整真实客户端/内置服务器回归 10 个入口通过，2 分 49 秒；无检查被删除。新增健康下限、预警/保护持久化、准备期、旧炉具/火把清理、16 层边界、溺水与真实死亡重生。
- 包检查：33 个 Java 25 类、4 个原有 Mixin；测试类、第三方 JAR 和原版视觉覆盖均未进入成品。SHA-256：`9073bca625e15fa050d3771094bfc4c47fb1ecc70a9bb9fe658ee5f051887ce8`。
- HUD 目视检查通过：火焰、箭头、庇护符号位置可读，未与饱食度重叠；水下回归通过。尚未进行最终光影/骑乘矩阵。
- 目标配置文件 extreme-winter-26.0.3-target-config.json 保留实际 coldModdedBiomes=true，明确 maxSnowLayers=16。未写入真实实例。

构建日志 `work/beginner-A-focused-5.log`；完整回归 `work/beginner-A-game-final.log`；包证据 `outputs/package-26.0.3.json`。最初缓存读取受限，提权使用原工具链后解决；旧语义断言、登录保护和重生观察时点的问题已修复，失败尝试日志保留。

冻结去重使用实际原版 freeze 伤害 API；站入粉雪方块的完整操作、睡觉/暂停真实分段、其他原版危险及所有地形/光影组合留在后续 B/E。固定公式不是路线试玩。尚未收到用户主观试玩反馈，不阻塞后续开发。

安装：Minecraft 26.1.2 / Loader 0.19.5 / API 0.155.3+26.1.2 / Java 25+；备份游戏实例配置与世界后仅安装当前 JAR，不装 sources/gametest JAR；已有 v2 配置自动备份迁移，显式无限雪不会被全局静默改写。

---

历史报告（不作为新版验证）：

# 26.0.2 / Minecraft 26.1.2 模组群系气候修复验证

2026-09-30，修复指定实例使用 Terralith 时，主世界扩展群系仍下雨且不形成真实积雪的问题。
只读检查存档玩家所在区块确认其群系为 `terralith:yellowstone`；安装包定义温度为 **0.24775**。
旧版仅修改 `minecraft:` 群系，实际日志只修改 160 个群系中的 54 个。

新增独立开关 `coldModdedBiomes=false`，缺省配置保留原行为；指定实例已启用该项。
开启后同样通过 Fabric API 将模组主世界群系降至 -0.5 并启用降水，不替换群系 ID 或存档地形。

- `build compileGametestJava` 通过；**18 项单元测试**通过，0 失败、0 错误。
- 默认配置正式 JAR 的原版 A 组 **9 个游戏测试入口全部通过**，2m 25s。
- 指定实例中的 **Terralith 2.6.1 / Tectonic 3.0.28 / Lithostitched 1.8.0 / Sodium 0.9.1 / Iris 1.11.4 / Eclipse Unstable**，在独立普通地形世界中分别测试开关关闭与开启，均通过，各 59s。
- 关闭：Yellowstone 温度 0.24775，服务器与客户端均为 `RAIN`，实际采样 0 雪层，水源保留。
- 开启：温度 -0.5，客户端为 `SNOW`，实际 tick 形成 **625 层**积雪，水源结冰；屋顶下无雪，下界与末地温度不变，存档重进后气候与雪层正确。
  新测试为稳定采样，将半径临时设为 0、每批设为 64，仅影响测试内存；用户实例仍使用半径 4、每批 16 的原配置。因此该层数不代表默认存档的积雪速度。
- 开启后日志为 **149/160** 群系修改，新增覆盖 Terralith 的 95 个群系；没有增加 Terralith 硬依赖或客户端渲染 Mixin。
- Eclipse 日志与截图确认实际启用。原光影设置保留；光影可能给雨天地表绘制白色覆盖，测试以服务器真实雪方块与客户端降水类型为依据。

成品：`outputs/extreme-winter-26.0.2.jar`，SHA-256：
`d7d8d4f0bf695ae4832d961b8c71746318a8b36aec7a75ffdac72bfd7ee9410c`。
包检查通过：31 个玩法类、Java 25 字节码、原有四个 Mixin、测试代码隔离、无资源覆盖或可选模组硬依赖。

已安装到 `D:\迅雷下载\PCL 正式版 2.12.7.3\.minecraft\versions\26.1.2-Fabric 0.19.5`，
只替换本模组并启用新开关；旧 JAR 和原配置备份在 `work/terralith-climate/install-backup`。
29 个其他模组/光影/游戏配置文件哈希一致；未修改用户存档。
无需创建新存档，完整重启游戏后继续原存档，积雪会逐步生成。

日志：`work/build-26.0.2.log`、`work/test-26.0.2-A.log`、`work/terralith-climate/review-off.log` 和 `review-on.log`。
截图：`outputs/terralith-climate-off/on-26.0.2.png` 与 `terralith-climate-comparison-26.0.2.png`。
包与安装记录：`outputs/package-26.0.2.json`、`outputs/installation-26.0.2.json`。
尚未验收指定实例的全部界面/性能模组及 Voxy 远景缓存刷新，也未覆盖其他主世界群系模组或多人。

# 26.0.1 / Minecraft 26.1.2 移植验证（历史记录）

2026-09-30，以 `mvp-26.0.0` 固定的最新版 MVP 为基础，移植到 **Minecraft 26.1.2 / Fabric / 单人**。
成品：`outputs/extreme-winter-26.0.1.jar`；原 26.0.0 成品与标签保留。
Loader **0.19.5** / Fabric API **0.155.3+26.1.2** / Gradle 9.8.0 / Loom 1.18.2。
使用本机 JDK 26.0.1 构建与运行，输出 Java 25 字节码；游戏最低 Java 25。

## 移植范围与构建

- 更新游戏目标、Fabric API 及模组版本；游戏依赖严格限定为 `26.1.2`。
- 玩法源码、配置默认值、方块/物品/附件标识和存档格式保持基线。
  两版 JAR 的 **31 个玩法类与全部资源逐字节一致**，只有 `fabric.mod.json` 和
  `META-INF/MANIFEST.MF` 中的游戏版本等元数据不同；文件集合无增删。
  比较记录：`work/port-26.0.1-content-comparison.json`。
- `build compileGametestJava` 通过，17 项单元测试通过，失败、错误、跳过均为 0。
  包检查同时核对 `gradle.properties` 中的 Minecraft、Loader、Fabric API 版本，
  并保留全部原有资源、Mixin、Java 字节码及测试代码隔离检查。
- `Prepare-Compat.ps1` 默认从项目配置读取游戏版本；测试截图使用 26.0.1 后缀。

## 正式 JAR 单人兼容矩阵

每组使用独立 `work/run-production-26.1.2-A` 至 `-E` 实例及正式 JAR，注册的
9 个真实游戏测试入口全部通过，共 45 次入口回归，覆盖树冠慢速降雪、96 层跨格积雪、最高层边界、屋顶保护、
原版粒子、庇护边界、热源/火把计时与回收、寒冷阶段、雪地移动、初始营火、
体温/HUD、自然地形、中英文悬停和存档保存重进。

可选组件沿用基线版本，逐个在线核对其 26.1.2 支持范围及 SHA-512，并验证复制文件。
清单：`work/compat/26.1.2/versions.json`；这些组件仅用于测试，不进入模组成品。
本轮实际渲染器为 NVIDIA GeForce RTX 4060 Laptop GPU，OpenGL 驱动 591.86。

| 配置 | 组合 | 结果 | Gradle 时长 |
| --- | --- | --- | --- |
| A | Fabric API | 通过 | 2m 54s |
| B | A + Sodium 0.8.9 | 通过 | 2m 37s |
| C | B + Iris 1.10.9，关闭光影 | 通过 | 2m 41s |
| D | C + Complementary Reimagined r5.9.3，实际启用光影 | 通过 | 2m 39s |
| E | C + Lithium 0.24.7 / FerriteCore 9.0.0 / Mod Menu 18.0.2 / Cloth Config 26.1.154，关闭光影 | 通过 | 2m 30s |

D 的光影状态由 Iris 公共 API 断言和 `Using shaderpack: ComplementaryReimagined_r5.9.3.zip`
日志同时确认；其余含 Iris 的组合关闭光影。基础与光影截图已目视检查。

## 交付与限制

成品 79859 字节、31 个 Java 25 类、4 个通用 Mixin；无测试类、嵌套第三方 JAR、
可选模组硬依赖或原版视觉覆盖。包清单 `outputs/package-26.0.1.json`，
SHA-256：`89ef011558e6a8d46c9071e0ca37d9bebcd4ee8b82208d531c54c4db14d7f969`。
日志：`work/test-26.0.1-A.log` 至 `-E.log`；汇总 `work/matrix-26.0.1-summary.log`。
单元报告：`build/reports/tests/test/index.html`。
截图：`outputs/canopy-snow-26.0.1-vanilla.png` / `-complementary.png`，以及
`heat-bars`、`warmth-hud`、`visuals` 的相同版本与组合后缀。

离线测试的认证/Realms 日志、Iris refmap、Sodium 驱动规避与 Complementary
`BIOME_SULFUR_CAVES` 警告保留，另有 Gradle/Java API 弃用警告；未修改第三方文件或关闭检查。
本轮未测试多人/专用服务器、用户旧存档跨游戏版本升级、Java 25 运行时或其他光影包。
未安装到用户游戏实例或发布到 GitHub。

---

# 26.0.0 MVP / 树冠降雪与无上限积雪验证

2026-09-30，模组 **26.0.0** 作为后续开发的 MVP 基线，目标 **Minecraft 26.1 / Fabric / 单人**。
成品：`outputs/extreme-winter-26.0.0.jar`；范围与后续维护约定见 [MVP.md](MVP.md)。
Loader 0.19.5 / Fabric API 0.145.1+26.1 / Gradle 9.8.0 / Loom 1.18.2。
JDK 26.0.1 构建与运行，Java 25 字节码；游戏最低 Java 25。

## 本轮真实行为

- 树冠下的地面增加额外积雪采样，每第四个普通积雪批次运行一次，即约为露天的四分之一频率。
  检查的是同一批已加载列；树冠上已经堆积的雪不妨碍找到树下地面。
- 默认 `maxSnowLayers=0`，露天和树冠下均不设人为厚度上限。两种场景真实累积到 **96 层 / 12 格**，
  超过之前的 64 层；跨格层数守恒。最高可放置层可填满，之后不越过世界高度。
  已升级配置中的正数上限继续生效，原有 64 层封顶断言保留；1.0 的旧默认值仍按既有规则迁移并备份。
- 真实服务器 tick 检查：在普通的采样批次中树下地面不增长，到第四批次后增长。
  玻璃、石头等实心屋顶阻止树下积雪；农作物、机器、水源以及未加载区块继续受保护。
- 雨雪中的寒冷主世界树叶下增加原版 `SNOWFLAKE` 粒子，每 5 tick 最多尝试 4 个候选位置，
  位于玩家周围水平 6 格。真实客户端粒子引擎已收到粒子；晴天和叶片下的玻璃屋顶均不新增粒子。
  不修改天气渲染器、光影源文件或原版材质。背景露天仍由原版/光影自身绘制降水。
- 冻结水源继续要求直达天空；体温及热源的 3×3 / 7 列庇护规则不变。
  其他初始物资、物品时钟、火把、寒冷阶段、雪地移动、HUD、中英文悬停和存档重进回归均通过。

## 最终正式 JAR 的兼容矩阵

每组独立实例、**9 个真实游戏测试入口全部通过**；Windows / Intel Arc Graphics，驱动 32.0.101.8132。
测试实例的积雪配置显式设为 0；原值备份在 `work/pre-canopy-26.0.0/config-A.json` 至 `config-E.json`。
可选依赖文件沿用上一轮已按 SHA-512 校验的 26.1 组合；D 的光影启用状态由 Iris API 及日志确认。

| 配置 | 组合 | 结果 | Gradle 时长 |
| --- | --- | --- | --- |
| A | Fabric API | 通过 | 2m 33s |
| B | A + Sodium 0.8.9 | 通过 | 2m 33s |
| C | B + Iris 1.10.9，关闭光影 | 通过 | 2m 30s |
| D | C + Complementary Reimagined r5.9.3，实际启用光影 | 通过 | 2m 37s |
| E | C + Lithium 0.24.7 / FerriteCore 9.0.0 / Mod Menu 18.0.2 / Cloth Config 26.1.154，关闭光影 | 通过 | 2m 31s |

17 项单元测试全部通过（10 项温度、7 项配置），失败、错误、跳过均为 0。
新增配置回归验证 `0`、显式 `64/4096` 及负数处理，文件原文保留。
首次 A 中新增世界高度测试把最高层错设为 `getMaxY()-1`；核对 26.1 字节码确认其为包含上界后，
改用 `getMaxY()` 并重跑，原越界断言保留。失败日志为 `work/test-26.0.0-canopy-A-first-failure.log`。

## 交付与限制

包检查通过：79857 字节、31 个 Java 25 类、4 个通用 Mixin。
无测试类、嵌套第三方 JAR、可选模组硬依赖或原版视觉覆盖。
SHA-256：`2bc0c2d78889543369126a256c787fbe627ebdd7654c412541c9059391090f72`。
日志 `work/test-26.0.0-A.log` 至 `work/test-26.0.0-E.log`，汇总 `work/matrix-26.0.0-mvp-summary.log`。
单元报告 `build/reports/tests/test/index.html`；包清单 `outputs/package-26.0.0.json`。
`outputs/canopy-snow-26.0.0-vanilla.png` 和 `outputs/canopy-snow-26.0.0-complementary.png`
展示树下积雪及雪花；其他最终截图使用 `heat-bars`、`warmth-hud`、`visuals` 的相同版本/组合后缀。

离线认证、Realms、测试窗口选项和第三方 Iris/Complementary 警告保留，没有修改第三方源文件。
未测试专用服务器/多人、用户已有存档跨游戏版本升级、Java 25 运行时、其他 GPU 与其他光影包。
当前在 JDK 26 上验收；未安装到用户的旧 1.21.6 实例或发布到 GitHub。

---

# 26.0.0 初次移植历史验证（树冠补充前）

2026-09-30，本地单人验收。模组版本 **26.0.0**，游戏版本 **26.1**。
成品：`work/pre-canopy-26.0.0/outputs/extreme-winter-26.0.0.jar`。本次没有安装到旧 1.21.6 实例，也没有发布到 GitHub。
Fabric Loader 0.19.5 / Fabric API 0.145.1+26.1 / Gradle 9.8.0 / Loom 1.18.2。
本机 JDK 26.0.1 构建与运行，以 `--release 25` 输出 Java 25 字节码，游戏最低 Java 25。

## 最终正式 JAR 的完整矩阵

每组使用独立的 26.1 测试实例和正式 JAR；8 个真实游戏测试入口全部通过。
Windows / Intel Arc Graphics，OpenGL 驱动 32.0.101.8132。可选依赖通过 Modrinth 元数据核对
Minecraft 26.1 支持范围，并逐个校验 SHA-512；缓存清单为 `work/compat/26.1/versions.json`。

| 配置 | 组合 | 结果 | Gradle 时长 |
| --- | --- | --- | --- |
| A | Fabric API | 通过 | 3m 3s |
| B | A + Sodium 0.8.9 | 通过 | 2m 56s |
| C | B + Iris 1.10.9，关闭光影 | 通过 | 2m 49s |
| D | C + Complementary Reimagined r5.9.3，实际启用光影 | 通过 | 3m 55s |
| E | C + Lithium 0.24.7 / FerriteCore 9.0.0 / Mod Menu 18.0.2 / Cloth Config 26.1.154，关闭光影 | 通过 | 2m 51s |

Iris 启动日志和公开 API 均检查光影状态：D 启用，其余含 Iris 的组合关闭。
Iris/Sodium 的文件名含 `26.1.1`，实际元数据覆盖 26.1；使用 Iris 声明的准确 Sodium 依赖。
各组均测试体温/HUD、初始物资、便携热源、火把冷却、气候阶段、积雪移动、
炉子燃料、岩浆与火把计时保存重进、自然地形以及中英文实际鼠标悬停。

## 新庇护规则与回归

- 头顶被覆盖且周围 **3×3 至少 7/9 列有顶**才提供庇护。单格石头、玻璃、树叶，
  一格宽横梁、屋檐边缘、正上方洞口及只有 6 列覆盖均按露天处理；7 列及完整屋顶、树冠、洞顶有效。
- 真实生存玩家在单块玻璃下运行 40 tick 仍失温，在完整玻璃顶下同样运行 40 tick 保温；
  营火在单格遮挡下增加耗损，完整屋顶下暂停。脚边积雪不计作屋顶，缺失区块不加载。
- 体温与全部热源耗损共用此判定；积雪生成和水源冻结独立保留直达天空约束。
- 26.1 移植使用官方名称，适配天气、世界时钟、保存数据、网络、HUD 和物品掉落 API；
  原有配置字段、组件及附件标识保留。测试中保存重进的体温、炉子、岩浆、火把计时通过。
- 初始营火不传送玩家、不生成建筑；同一客户端连续新建两个存档都只领取 1 个营火。
- 火把到期后准确掉落 1 个原版物品；未恢复满时不能放置且不扣数量。营火/炉子回收保留计时，
  普通石剑的原版耐久条及悬停不变。新庇护说明具有中英文翻译。
- 积雪行走速度比为 0.8570759，薄雪、满层及堆叠雪一致；地面/雪地跳跃高度均为 1.2522033 格。
  11 种树叶的 22 项模型/纹理资源在这批 Sodium 0.8.9 组合中均来自 vanilla。
- 16 项单元测试全部通过（10 项温度、6 项配置），失败、错误和跳过均为 0。
  PowerShell 构建、依赖准备及矩阵脚本的语法检查通过；`git diff --check` 通过。

## 包与证据

包检查通过：76092 字节、29 个 Java 25 类、4 个通用 Mixin。
没有测试类、嵌套第三方 JAR、可选模组硬依赖或原版视觉覆盖；HUD 使用 Fabric 公共 API，
没有客户端渲染 Mixin 或直接 OpenGL 调用。
SHA-256：`658b4ca0e9f83a94967494dd9270f996bcb5f710333e9f9693c1fe8d42009c5c`。

日志：`work/pre-canopy-26.0.0/test-A.log` 至 `work/pre-canopy-26.0.0/test-E.log`，汇总 `work/pre-canopy-26.0.0/matrix-summary.log`。
单元报告：`build/reports/tests/test/index.html`；包检查：`work/pre-canopy-26.0.0/outputs/package-26.0.0.json`。
截图：`work/pre-canopy-26.0.0/outputs/heat-bars-26.0.0-vanilla.png`、`work/pre-canopy-26.0.0/outputs/warmth-hud-26.0.0-vanilla.png`、
`work/pre-canopy-26.0.0/outputs/visuals-26.0.0-vanilla.png`、`work/pre-canopy-26.0.0/outputs/visuals-26.0.0-complementary.png`、
`work/pre-canopy-26.0.0/outputs/heat-bars-26.0.0-complementary.png`、`work/pre-canopy-26.0.0/outputs/warmth-hud-26.0.0-complementary.png`。
这些交付截图已目视检查。

日志中的离线认证/Realms 网络超时、测试窗口 Anisotropic Filtering 选项错误、
Iris refmap 提示，以及 Complementary 的 `BIOME_SULFUR_CAVES` 和旧 `stone_slab variant`
映射警告均保留；没有修改第三方光影源文件，具体情况以原始日志为准。
未验证专用服务器/多人、用户已有存档的跨游戏版本升级、Java 25 运行时、其他显卡和其他光影包。
本次在 JDK 26 上通过；最低 Java 25 由编译目标与包元数据确认。

---

# 1.2.1 历史验证报告

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
