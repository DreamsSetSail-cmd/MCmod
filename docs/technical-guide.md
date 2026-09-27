# Echoes of Oblivion — 技术实现说明

> 面向接手者 / 维护者的完整技术手册。读完本文你应该能：独立构建、运行、验证、
> 修改任意系统，并知道每个设计决策背后的取舍与踩过的坑。
>
> 配套文档：`README.md`（概览）、`ASSETS.md`（素材）、`CREDITS.md`（授权）、
> `docs/story-bible.md`（叙事设计）、`PUBLISHING.md`（发布）。

---

## 1. 技术栈与工程约束

| 项 | 值 | 说明 |
| --- | --- | --- |
| Minecraft | 1.20.6 | |
| Forge | 50.2.10 | `mods.toml` 声明 `[50,)` |
| Java | 21 | `build.gradle` 用 toolchain 固定，不要依赖本机 JDK 版本 |
| Gradle | 9.3.1（wrapper） | 发行包走华为云镜像 |
| ForgeGradle | `[7.0.3,8)` | 会自动下载并**反编译** Minecraft |
| 映射 | `official`（Mojang 官方名） | 用官方名写代码，不要用 MCP 名 |
| 模组 ID | `echoesofoblivion` | 包名 `com.clion.echoesofoblivion`（历史遗留，与 group 一致即可） |

### 1.1 首次构建很慢，这是正常的

ForgeGradle 需要下载 Minecraft 并反编译，**首次约 5~10 分钟**。上游日志里
`Minecraft Maven has finished, took 0:17` 只是最后一步，别被它误导。

### 1.2 两个非标准配置，都有原因

```properties
net.minecraftforge.gradle.merge-source-sets=true   # 编译产物落在 build/sourceSets/main
org.gradle.caching=false                            # 见下
```

**为什么关掉构建缓存**：本机曾出现 `compileJava FROM-CACHE` 却**没有还原 class 输出**的情况——
`BUILD SUCCESSFUL`，但 jar 从 380 KB 掉到 260 KB 且**一个 `.class` 都没有**。
`merge-source-sets` 改变了产物目录，缓存恢复与它交互不良。这是个**静默坏产物**，
比多花几十秒严重得多。**如果你要重新开启缓存，务必在每次构建后确认 jar 里有 55 个 class。**

---

## 2. 目录结构

```
├── src/main/java/com/clion/echoesofoblivion/
│   ├── EchoesOfOblivionMod.java      主类：注册 7 个 DeferredRegister + 总线订阅 + 配置
│   ├── Config.java                   ForgeConfigSpec（COMMON / CLIENT 两个 spec）
│   ├── ConfigHelper.java             配置读取的安全包装（见 §6.1）
│   ├── CorruptionEventHandler.java   侵蚀值推进与同步（服务端）
│   ├── MirrorChunkHandler.java       镜像区块扩散、重力闪烁、感染区亡魂
│   ├── block/
│   │   ├── ModBlocks.java            方块与方块实体注册
│   │   ├── MemoryCrystalBlock.java   记忆水晶（客户端发包）
│   │   ├── UnstablePortalBlock.java  无碰撞传送门，进入即传送
│   │   └── entity/MemoryCrystalBlockEntity.java
│   ├── item/
│   │   ├── ModItems.java             物品注册（42 件）
│   │   ├── CorridorKeyItem.java      点燃传送门（含 2×3 门框校验）
│   │   ├── EyeOfSilenceItem.java     Boss 召唤凭证
│   │   ├── ResonanceForkItem.java    三种仪式（v2.0.0）
│   │   ├── ExcavationShovelItem.java 取样记忆水晶 + 开启被封残片
│   │   ├── MemoryVialItem.java       降侵蚀，代价是烧掉最近一段记忆
│   │   ├── StabilizerItem.java       90 秒免疫重力闪烁（含静态计时表）
│   │   ├── MemoryBladeItem.java      对亡魂追加伤害、对聚合体 0 伤害
│   │   ├── ShatterStaffItem.java     射线检测 + 落点范围驱散
│   │   ├── ResearcherLanternItem.java 持有期间侵蚀增长减半
│   │   ├── MemoryScrollItem.java     打开记忆图鉴
│   │   ├── RuinsCompassItem.java     螺旋区块扫描找水晶
│   │   └── LoreFragmentItem.java     12 段残片（含 Kind 分类与阅读界面）
│   ├── entity/
│   │   ├── ModEntities.java          实体注册 + 属性（@EventBusSubscriber 自动挂载）
│   │   ├── PhantomEntity.java        亡魂
│   │   ├── MirrorEntity.java         镜中的你（不透明渲染）
│   │   └── SilentAggregateEntity.java Boss：免疫、恐惧光环、稳定性
│   ├── memory/
│   │   ├── MemoryRegistry.java       5 段记忆的静态目录（单一事实来源）
│   │   ├── MemoryEntry.java          记忆 record（id/键/线索/颜色/连接线索）
│   │   ├── MemoryProgression.java    进度里程碑的世界联动
│   │   ├── PlayerMemoryData.java     SavedData：按 UUID 分桶的进度容器
│   │   └── PlayerProgress.java       单玩家进度 + 侵蚀值 + forget()（焚烧用）
│   ├── network/
│   │   ├── ModNetwork.java           ChannelBuilder + 9 个包的注册
│   │   └── packets/                  9 个 CustomPacketPayload
│   ├── server/
│   │   ├── ServerPacketHandler.java  水晶交互判定 + 进度同步
│   │   ├── BossCombat.java           记忆攻击、恐惧光环、召唤、终结结算
│   │   ├── BossAura.java             恐惧光环的渲染/侵蚀副作用
│   │   ├── MemoryBurn.java           记忆焚烧：按主题施加效果（潜行 + R）
│   │   └── RitualScheduler.java      仪式的延迟任务队列（静态总线订阅者）
│   ├── world/
│   │   ├── ModWorldGen.java          ResourceKey + 自定义 Feature 注册
│   │   ├── RealityData.java          镜像区块的 SavedData
│   │   ├── CorridorTeleportHelper.java 双向传送与落点铺设
│   │   ├── Rituals.java              三种仪式的效果实现
│   │   ├── RuinPileFeature.java      废墟地物
│   │   └── MonumentFeature.java      纪念碑（残片的放置来源）
│   ├── sound/ModSounds.java          11 个 SoundEvent
│   ├── datagen/                      GatherDataEvent + 两个 provider
│   └── client/                       所有仅客户端代码
│       ├── ClientData.java           客户端状态缓存（无逻辑权威）
│       ├── ClientPacketHandler.java  包处理（DistExecutor 隔离后调用）
│       ├── ClientTickHandler.java    每 tick 与登录/登出
│       ├── ClientScreenEffects.java  传送全屏过渡（直绘）
│       ├── CorruptionVisuals.java    侵蚀雾效（ViewportEvent）
│       ├── PhantomFlashes.java       纯客户端幻影闪现
│       ├── ShapeRenderer.java        自绘彩色立方体工具
│       ├── PhantomRenderer.java      亡魂渲染（自绘几何）
│       ├── SilentAggregateRenderer.java Boss 渲染（核心 + 符文环）
│       └── screen/MemoryVisionScreen.java 幻境屏幕
├── src/main/resources/
│   ├── META-INF/mods.toml            模组元数据（作者、MIT、依赖）
│   ├── assets/echoesofoblivion/      贴图、音效、sounds.json、21 个语言文件
│   └── data/echoesofoblivion/        维度、群系、地物、配方、战利品表、gameTest 结构
├── src/generated/resources/          datagen 输出（唯一来源，勿手改）
├── tools/                            开发工具（见 §8）
└── docs/                             设计与翻译规格
```

---

## 3. 核心系统实现

### 3.1 注册体系

主类构造中统一挂载 7 个 `DeferredRegister`（方块、方块实体、物品、实体、创造标签页、
音效、世界生成 Feature），外加配置注册与网络通道注册。

**实体属性**用 `@Mod.EventBusSubscriber(bus = MOD)` 自动挂载，
**不要**再在主类里手动 `modBus.addListener(ModEntities::registerAttributes)`——那会双重注册。

### 3.2 网络层（9 个包）

```java
public static final SimpleChannel CHANNEL = ChannelBuilder
    .named(EchoesOfOblivionMod.id("main"))
    .networkProtocolVersion(2)
    .clientAcceptedVersions(Channel.VersionTest.exact(2))
    .serverAcceptedVersions(Channel.VersionTest.exact(2))
    .simpleChannel();
```

| ID | 包 | 方向 | 内容 |
| --- | --- | --- | --- |
| 0 | `MemoryCrystalUsePacket` | C2S | 方块坐标（**只有坐标**） |
| 1 | `MemoryVisionPacket` | S2C | 记忆索引 + 是否首次 |
| 2 | `MemorySyncPacket` | S2C | 进度位掩码 + 线索 CSV |
| 3 | `CorruptionUpdatePacket` | S2C | 侵蚀值 + 比例 |
| 4 | `ShadowSyncPacket` | S2C | 影子状态码 |
| 5 | `RealityShiftPacket` | S2C | 区块 long 列表 + 是否增量 + 阶段 |
| 6 | `PortalTravelPacket` | S2C | 传送反馈 |
| 7 | `RenderStatePacket` | S2C | 通用表现指令（kind 区分） |
| 8 | `MemoryAttackPacket` | C2S | 记忆索引 |

#### 三条必须遵守的规则

**规则一：包类里禁止出现客户端类型。**
旧代码在包里直接 `import net.minecraft.client.Minecraft`，服务端加载时抛
`NoClassDefFoundError`——**服务端必崩**。正确写法：

```java
public static void handle(MemoryVisionPacket packet, CustomPayloadEvent.Context context) {
    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
        () -> () -> ClientPacketHandler.openVision(packet.memoryIndex(), packet.firstTime())));
    context.setPacketHandled(true);
}
```

`DistExecutor.unsafeRunWhenOn` 的 lambda 只在对应分发端被求值，因此那个 `ClientPacketHandler`
引用在服务端永远不会被解析。

**规则二：编解码必须用 `FriendlyByteBuf`，不是 `RegistryFriendlyByteBuf`。**
`SimpleChannel.messageBuilder` 默认 `BUF = FriendlyByteBuf`，用错类型会编译失败：

```
不兼容的类型: StreamCodec<RegistryFriendlyByteBuf, X>无法转换为StreamCodec<FriendlyByteBuf, X>
```

**规则三：注册用 `.codec(...)` 而不是分别 `.encoder()` + `.decoder()`。**
编码解码来自同一个 `StreamCodec`，**从结构上排除字段顺序不一致的静默错误**。

### 3.3 进度与侵蚀值（服务端权威）

```
PlayerMemoryData (SavedData, 锚定主世界)
   └─ Map<UUID, PlayerProgress>
          ├─ Set<Integer> collected      已见证的记忆索引
          ├─ Set<String>  clues          已获得线索
          └─ int corruption              侵蚀值 0~100
```

- 存储锚点固定为**主世界**：玩家进度与所在维度无关。
- 读取容错：损坏的 UUID 条目跳过，保证存档可升级。
- `markDirty()` 必须在任何修改后调用，否则不写盘。

**侵蚀值只有一个权威来源**（就是这个 `corruption`）。
旧代码同时维护 `persistentData["echoes_corruption"]` 和 `SavedData` 两套数值，
而客户端界面用的那个**根本没有来源**，永远显示 0。**不要再引入第二套数值。**

### 3.4 记忆系统

```
MemoryRegistry（静态目录，5 条）
   ├─ MemoryEntry { id, titleKey, contentKey, clueKey, clue, color, connectedClues }
   └─ 查询：byIndex / byId / byClue / indexOf
```

**水晶给哪条记忆是确定性推导的**，不是随机的：

```java
public static int crystalMemoryIndex(BlockPos pos) {
    int hash = pos.getX() * 73856093 ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
    hash ^= hash >>> 13; hash *= 0x5bd1e995; hash ^= hash >>> 15;
    return Math.floorMod(hash, MemoryRegistry.size());
}
```

用固定混合函数而非 `Random` 的原因：同一坐标在任何时候、任何客户端都得到同一条记忆，
**玩家可以在地图上记住哪块水晶讲了什么**。若该记忆已见证，沿环形向后找一条未见证的，
保证玩家总能获得新内容（`ServerPacketHandler.onCrystalUse`）。

**服务端校验**（缺一不可）：方块确实是记忆水晶 → 玩家距离 ≤ 6 格 → 命中冷却。

### 3.5 维度与传送

**维度用超平坦生成器，不用噪声。**

```json
{ "type": "echoesofoblivion:silent_corridor",
  "generator": { "type": "minecraft:flat",
    "settings": { "layers": [ 基岩1 / 深板岩4 / 黑石1 ], "lakes": false, "features": true,
                  "biome": "echoesofoblivion:silent_biome" },
    "biome_source": { "type": "minecraft:fixed", "biome": "echoesofoblivion:silent_biome" } } }
```

选择理由：噪声设置（`noise_settings`）需要完整的密度函数图，**写错一个字段就崩服**；
超平坦只有 layers 数组，不可能写错，且「走廊」本来就是个平坦空间。

**传送必须按 `coordinate_scale` 换算**（本维度为 2.0）：

```java
// 主世界 → 走廊：坐标除以 2；走廊 → 主世界：乘以 2
```

落点带 ±12 格随机偏移（不稳定感），并调用 `buildLandingPad` 铺 3×3 落脚平台，
避免掉进虚空或被埋进方块。

### 3.6 镜像区块（现实坍缩）

```
RealityData (SavedData)
   ├─ Set<Long> infected      区块的 ChunkPos.asLong 编码
   ├─ int phase               0~3，由感染规模推导
   └─ spreadFrom(player)      以玩家为中心、密度随距离衰减地扩散
```

- 上限 `MAX_INFECTED = 4096`，防止存档无限膨胀。
- 只在主世界推进，只在服务端。
- 客户端通过 `RealityShiftPacket` 接收**增量**（`add=true`）或**全量**（登录/跨维度）。

区块内效果（`MirrorChunkHandler.applyInfectedEffects`）：
重力闪烁（改 `deltaMovement` + `hurtMarked`）、反向粒子（`REVERSE_PORTAL`、负速度 `END_ROD`）、
亡魂成形（有总量上限，避免刷爆）。

### 3.7 Boss 战

**免疫**：

```java
@Override
public boolean hurt(DamageSource source, float amount) {
    if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
    // 播放无效化反馈，返回 false
    return false;
}
```

放行 `BYPASSES_INVULNERABILITY` 是必要的——否则 `/kill` 和虚空伤害也无效，
Boss 会变成无法清除的实体。

**稳定性取代生命值**：`MAX_STABILITY = 100`，阶段在 ≤60 / ≤30 时推进到 1 / 2。
`reduceStability()` 返回**是否被击败**，由调用方决定结算，实体自身不 `discard`。

**记忆攻击**（`BossCombat.onMemoryAttack`）四道校验：
① 索引合法 ② 玩家已收集该记忆（防作弊）③ 范围内有 Boss（≤48 格）④ 本场战斗未用过
（用 `persistentData["echoes_boss_used_<i>"]` 标记）。

伤害：基础 12，**共鸣（连接线索齐备）翻倍**。

**召唤三条件**（缺一不可）：见证全部 5 段 + 身处感染区块 + 手持寂静之眼。

### 3.8 v2.0.0 的四个新系统

#### 记忆焚烧（`server/MemoryBurn.java`）

潜行 + 记忆攻击键触发。`MemoryAttackPacket` 的潜行分支转发到这里，
非潜行分支仍归 `BossCombat`——**两条路径共用一个按键**，靠潜行状态分流。

```java
progress.forget(memoryIndex);   // 不可逆
data.markDirty();
ServerPacketHandler.syncProgress(player, progress);   // 图鉴当 tick 变回划痕
```

效果按**记忆索引**分发，不按数值配置：

| 索引 | 主题 | 效果 |
| --- | --- | --- |
| 0, 3 | 仪器的光 | `discard()` 半径 24 内全部亡魂 |
| 1, 2 | 桥与静默介质 | `setSilent(true)` + `setNoAi(true)` 30 秒，**不驱散** |
| 4 | 说出来的话 | 清 3x3 区块感染 + 侵蚀压回 50 |

**静默分支的易错点**：恢复时必须过滤 `isRemoved()`。
实体在被定身的 30 秒里完全可能被别的原因移除，对已移除实体调用
`setSilent` 会抛异常。

`forget()` **刻意不移除线索**——线索是「你已经理解了这件事」的痕迹，
与记得它的来源是两回事。

#### 三种仪式（`item/ResonanceForkItem.java` + `world/Rituals.java`）

音叉右键执行当前仪式，潜行右键切换。材料检查与扣除在 `ResonanceForkItem`，
效果实现在 `Rituals`，**延迟效果**（如 Descent 的 30 秒静音结束）交给
`RitualScheduler`。

`RitualScheduler` 必须在主类里**以类对象显式注册**：

```java
MinecraftForge.EVENT_BUS.register(com.clion.echoesofoblivion.server.RitualScheduler.class);
```

静态 `@SubscribeEvent` 方法不会因为类被「用到」就自动挂载——
不注册的话队列永远不会被 tick，仪式效果看起来像没生效。

#### 残片（`item/LoreFragmentItem.java` + `client/screen/FragmentScreen.java`）

12 段残片各有独立的**注册名**（`fragment_<id>`），共用一个类，靠构造参数带 `id` 与 `Kind`。
这样物品栏里能显示不同的名字与图标，而不需要 12 个类。

`Kind` 枚举同时携带语言键后缀与提示颜色，并在 `isFoil()` 里区分：
私人信件与祈祷有附魔光效，因为它们是唯一「有个人」的两类。

**易错点**：物品注册名是 `fragment_<id>`，所以物品名键是
`item.echoesofoblivion.fragment_<id>`；而正文与标题是
`fragment.echoesofoblivion.<id>.title` / `.text`。**这是两套不同的命名**，
漏掉前者会让残片在物品栏里显示为原始键名（v2.0.0 修过这个缺陷）。

#### 残片龛（`block/FragmentNicheBlock.java`）

12 段残片在世界里的**唯一来源**。它存在的理由是一条推论：
**「没有配方」必须同时给出另一条获取途径**，否则这 12 件物品在生存模式下
完全不可得，整套叙事就是死的。

```java
// 按玩家背包里还没有的索引顺序发放
for (var entry : ModItems.fragments().entrySet()) {
    if (!carried.stream().anyMatch(s -> s.is(entry.getValue().get()))) {
        return new ItemStack(entry.getValue().get(), 1);
    }
}
```

三个刻意的决定：

1. **确定性发放，而不是随机**。12 段的集合上随机必然导致赠券收集者问题——
   玩家会反复拿到读过的段落而永远缺某一段。改成按缺失索引发放后，
   「顺序不可控」依然成立：**找到哪个龛才是不可控的那一部分**。
2. **按背包判定而不是进度存档**。丢掉的段落必须能再拿到；
   按进度判定会造成永久不可恢复的死锁。
3. **`noLootTable()`**。破坏它不给掉落——它是一次性的历史遗留物，
   不是一个可以搬回家的家具。

#### 稳定剂的静态计时表（`item/StabilizerItem.java`）

```java
private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
```

- 用**游戏刻**（`level.getGameTime()`）而不是墙钟：暂停、卡顿、重启都以世界时间为准。
- 用 **UUID** 而不是玩家实例：实例会在维度切换后被替换。
- 查询时**惰性清理**过期项（不需要额外的 tick 任务），
  另外在 `PlayerLoggedOutEvent` 里显式清理，避免长跑服务器上这张表无限增长。

`MirrorChunkHandler` 在施加重力闪烁前查询它——这是「新物品改变既有系统行为」
的最小侵入写法：既有系统只多一个条件判断，不需要知道物品的存在。

---

## 4. 渲染实现（Forge 1.20.6 的特殊情况）

### 4.1 GUI 覆层体系已被移除

**这是本项目最重要的技术发现。** 以下类在 Forge 50.2.10 的 jar 里**逐个核实均不存在**
（源码里只剩注释掉的声明）：

`IGuiOverlay`、`ForgeGui`、`RegisterGuiOverlaysEvent`、`RenderGuiEvent`

若照旧教程写覆层，会直接编译失败。因此：

| 需求 | 实际实现 |
| --- | --- |
| 全屏过渡（传送渐隐） | `RenderLevelStageEvent.Stage.AFTER_LEVEL` + `RenderSystem` 直绘四边形 |
| 侵蚀压迫感 | `ViewportEvent.ComputeFogColor` + `RenderFog` 改雾色与雾距 |
| 幻境屏幕 | 继承 `Screen`（这个没问题） |

**直绘的关键细节**（`ClientScreenEffects.drawFullscreenQuad`）：

- 用 `RenderSystem.getModelViewMatrix()` 的副本作变换，**不要**用 `event.getPoseStack().last().pose()`——
  后者在本环境返回类型不一致（会报 `找不到符号: 方法 last()`）。
- 顶点写在相机附近（z = -0.1）并临时关深度测试，避免被地形裁掉。
- 用 `Tesselator.getInstance().getBuilder()` + `buffer.begin(...)` + `Tesselator.end()`——
  本环境的 API 是这一套（**不是** 1.20.5+ 的 `begin(...)` 返回 `MeshData`）。

### 4.2 实体用几何体而非模型

`ShapeRenderer` 直接向 `VertexConsumer` 输出彩色立方体，因此亡魂与聚合体
**不需要模型文件与贴图**就能显示。

```java
consumer.vertex(pose.pose(), x, y, z)
    .color(argb).uv(0, 0)
    .overlayCoords(OverlayTexture.NO_OVERLAY)
    .uv2(LightTexture.FULL_BRIGHT)
    .normal(pose, nx, ny, nz)
    .endVertex();
```

渲染类型用 `RenderType.lightning()`——**加法混合、不采样贴图**，
因此没有贴图也能正常工作，视觉上接近「灵体发光」。

**升级为正式模型的路线**：新建 `ModelLayerLocation`，在
`EntityRenderersEvent.RegisterLayerDefinitions` 注册 `LayerDefinition`，
再把渲染器改为继承 `MobRenderer<T, M>`；届时才需要 `textures/entity/*.png` 按标准 UV 绘制。

### 4.3 TickEvent 的正确用法

1.20.6 起 `TickEvent.Phase` 与 `TickEvent(Phase)` 构造器标记 `forRemoval`。
**用子类**：

```java
@SubscribeEvent
public static void onClientTick(TickEvent.ClientTickEvent.Post event) { ... }   // ✅
@SubscribeEvent
public void onPlayerTick(TickEvent.PlayerTickEvent.Post event) { ... }          // ✅
```

不要写 `event.phase != TickEvent.Phase.END`。

### 4.4 渲染器不要每帧新建 Random

`PhantomRenderer` / `SilentAggregateRenderer` 把 `RandomSource` 作为**实例字段**，
而不是在 `render()` 里 `new Random()`。

---

## 5. 数据与资源

### 5.1 datagen 是唯一来源

方块状态与物品模型由代码生成，**`src/main/resources` 下的手写副本已删除**，
避免两处来源漂移。

```bash
./gradlew runData     # 输出到 src/generated/resources/
```

| Provider | 产物 |
| --- | --- |
| `ModBlockStateProvider` | `blockstates/*.json`、`models/block/memory_crystal.json`、`models/item/memory_crystal.json` |
| `ModItemModelProvider` | 其余 7 个 `models/item/*.json` |

`ExistingFileHelper` 由 `--existing src/main/resources` 初始化，因此
**引用了不存在的贴图时 datagen 会直接报错**——这是加新方块时最有用的一层保护。

> `processResources { exclude '.cache/**' }` 是必需的：datagen 会在输出目录写
> `.cache/` 作为增量标记，不排除就会打进 jar。

### 5.2 语言文件与校验

21 个语言文件，每个 59 个键。**修改后必须运行**：

```bash
python tools/check_lang.py
```

它校验：JSON 合法性、键集合与 `en_us` 完全一致、
`%s` / `%%` 占位符数量一致、记忆正文的字面量 `\n` 数量一致、`◈` 标记存在。

**为什么这个脚本是必需的**：翻译时把 `%s` 改成别的、或漏掉 `%%`，
客户端会在**运行时**崩溃（`UnknownFormatConversionException`），
而编译期完全看不出来。

### 5.3 音效必须是 Ogg Vorbis

**Minecraft 的 `SoundManager` 把事件路径硬编码为 `.ogg`。**
即使 `SoundEngine` 能解码 wav，`SoundManager` 也找不到它——实测放 wav 会留下
11 条 `File ... does not exist` 警告且**音效完全不播放**。

所以流程是：`SoundSynth.java` 生成 wav → ffmpeg 转 ogg → 放入 sounds 目录。

### 5.4 贴图由代码生成

`tools/make_textures.py` 用**纯 Python 标准库**手写 PNG（`zlib` + `struct`），
不依赖 Pillow。9 张 16×16 像素画。

**经验**：第一版全用字符网格手绘，走廊之钥的匙杆只有 1 像素宽、真理碎片糊成一条线、
虚空余烬被画布裁切。**16×16 下手绘网格精度不够**——对称图形（圆环、多边形）
应当用几何图元生成。脚本里为此实现了 `polygon` / `circle` / `ring` / `rect`。

---

## 6. 踩过的坑（按严重程度排序）

### 6.1 配置读取早于配置加载 → 抛异常

`ForgeConfigSpec.ConfigValue.get()` 在配置**尚未加载**时抛 `IllegalStateException`。
配置是在服务器加载配置阶段才就绪的，而 `runGameTestServer` 的世界 tick 更早。

**处理**：`ConfigHelper` 采取「先查 `isLoaded()`、再兜 `RuntimeException`」的双保险，
回退值显式写成与注册时相同的默认值。

> **排查记录**：最开始我以为是自己的配置读取有问题，加探针打出完整堆栈后发现
> 真正的来源是 `ForgeHooks.isLivingOnLadder` ——**Forge 自己的代码**。
> 详见 §7.2。`ConfigHelper` 作为防御性改动保留，但它不是那次崩溃的根因。

### 6.2 服务端加载客户端类 → 必崩

见 §3.2 规则一。**任何 `client` 包之外的代码都不得 `import` 客户端类型。**

### 6.3 客户端/服务端判断写反 → 核心链路断开

`MemoryCrystalBlock` 原本写的是 `if (level.isClientSide) return SUCCESS;`——
客户端直接返回、**从不发包**，于是整个记忆收集链路是断的。

正确方向：**客户端发包，服务端判定**。

### 6.4 数据包方向反转

`sendToServer` 原本用 `PacketDistributor.SERVER`，且由**服务端**调用。
正确：客户端 `CHANNEL.send(payload, PacketDistributor.SERVER.noArg())`。

### 6.5 维度数据缺字段 → 加载即崩

原 `noise_settings` 用旧版数值格式、缺 `vegetation` 字段；`dimension_type` 缺
`bed_works`/`has_raids`/`piglin_safe`/`respawn_anchor_works`；群系用了 1.20.6 已改名的
`precipitation`（现为 `has_precipitation` 布尔值）。

### 6.6 群系静默回退到平原

维度 JSON 的 flat `settings` 缺 `biome` 字段时，日志只给一条
`Unknown biome, defaulting to plains`，**不报错**——但水晶与废墟都不会生成。
**看到这条日志必须立刻修。**

### 6.7 构建缓存产生缺少 class 的坏 jar

见 §1.2。

### 6.8 Windows 不记录可执行位 → CI 失败

`git` 在 Windows 上不记录 POSIX 可执行位，Linux runner 报
`./gradlew: Permission denied`（exit 126）。

**处理**：`git update-index --chmod=+x gradlew`（索引模式 `100644` → `100755`），
同时 workflow 里改用 `bash gradlew`，双保险。

### 6.9 CI 上 gameTest 偶发不退出

打 tag 那次的 `Run game tests` 挂了 **1 小时 31 分**，而同一流程在主分支上 2 分钟就过。
**本机反复跑从未复现**（8 次全过），因此无法定位根因，只能加超时兜底：
作业级 `timeout-minutes: 45`，gameTest 步骤级 `5`。

---

## 7. 验证体系

### 7.1 六种命令，按成本递增

| 命令 | 覆盖范围 | 耗时 |
| --- | --- | --- |
| `gradlew compileJava` | 编译正确性 | ~20s |
| `python tools/check_lang.py` | 21 个语言文件的一致性 | ~1s |
| `gradlew build` | 编译 + 打包（含 datagen 产物） | ~1min |
| `gradlew runGameTestServer` | 服务端逻辑（5 项断言） | ~3min |
| `gradlew runData` | datagen provider 输出 | ~2min |
| `gradlew runServer` | 数据包加载、维度注册 | ~2min |
| `gradlew runClient` | 资源解析、渲染路径、音效 | ~5min |

**`runClient` 是覆盖面最广的一环**：它真实加载全部资源，
因此音效缺失、模型不可解析、渲染器抛异常都在这一步暴露。

### 7.2 gameTest 的环境限制（重要）

`gametest/BossGameTests.java` **刻意不调用 `entity.tick()`**，也不用 `helper.spawn()`。

原因：在 Forge 50.2.10 的测试服务器里，**任何会移动的 `LivingEntity` 在 tick 时都会抛异常**：

```
ForgeConfigSpec$ConfigValue.get
  <- ForgeHooks.isLivingOnLadder      （Forge 自己的代码）
  <- LivingEntity.onClimbable
  <- LivingEntity.tick
```

`ForgeHooks.isLivingOnLadder` 读取 `ForgeConfig.SERVER`，而**测试服务器不加载服务端配置**。
这是环境限制，与模组无关，真机不会发生。

因此测试只覆盖不依赖 tick 的逻辑：武器免疫、稳定性阶段推进、记忆库一致性、
方块放置、属性注册。**凡是需要实体真正跑 tick 的行为，只能靠 `runClient` 实机验证。**

**gameTest 结构**：`@GameTest(template = "echoesofoblivion:empty")` 会去找
`data/echoesofoblivion/structures/empty.nbt`。原版没有 `minecraft:empty` 这个结构，
所以本项目自备一个——由 `tools/make_empty_structure.py` 生成
（纯 Python 手写 NBT + gzip，因为 Minecraft 侧用 `NbtIo.readCompressed` 读取）。

### 7.3 可以忽略的日志噪声

- `Missing sound for event: minecraft:item.goat_horn.play` —— 原版 1.20.6 自身缺的乐器音效
- `Assets URL ... uses unexpected schema` —— ForgeGradle 开发环境类路径提示
- `Ignoring duplicate module on SecureModuleFinder` —— LWJGL 多平台原生库
- `Configuration conflict: there is more than one oshi.properties` —— 依赖重复
- `[removal] ... has been deprecated` —— 见 §9.1

---

## 8. 开发工具（`tools/`）

| 文件 | 用途 |
| --- | --- |
| `SoundSynth.java` | 生成 8 个音效 wav（JDK `javax.sound`，无外部依赖） |
| `make_textures.py` | 生成 9 张像素画 PNG（纯标准库手写 PNG） |
| `make_empty_structure.py` | 生成 gameTest 用的空结构 NBT（纯标准库手写 NBT + gzip） |
| `check_lang.py` | 校验 21 个语言文件（**改语言后必跑**） |
| `cc0-assets/` | 已验证授权的 CC0 备用素材（含来源记录） |

**这三个生成器都不参与打包**，`build.gradle` 只纳入 `src/main/java`、
`src/main/resources`、`src/generated/resources`。

---

## 9. 已知技术债

### 9.1 三处 `forRemoval` 弃用调用

`EchoesOfOblivionMod.java` 第 31 / 48 / 49 行：

```java
FMLJavaModLoadingContext.get().getModEventBus();   // 31
ModLoadingContext.get().registerConfig(COMMON, Config.COMMON_SPEC);   // 48
ModLoadingContext.get().registerConfig(CLIENT, Config.CLIENT_SPEC);   // 49
```

**目前只是警告，编译与运行都正常。** 我尝试过迁移但没能确认 1.20.6 的稳定替代：
`ModLoadingContext.getActiveContainer()` 可用，但 `ModContainer` 上**没有**
`registerConfig`（只有 `addConfig(ModConfig)`，需要先手搓 `ModConfig` 实例）；
而且 FML 的 sources jar 里没有这些类的源码可供核对签名。

**建议**：等升级到 1.21.x 时一并处理；或在有源码的环境里核对
`ModLoadingContext` 的 `@Deprecated` 注解指向哪个方法。

### 9.2 实体贴图未使用

`textures/entity/phantom.png`（32×64）与 `silent_aggregate.png`（64×64）
仍是色块。因为自绘几何渲染**不采样它们**，所以不影响观感；
升级为 `MobRenderer` + 正式模型时才需要补上。

### 9.3 废墟只有断墙（v2.0.0 已部分解决）

`RuinPileFeature` 生成断墙、碎砖、残柱，现在还会在约四分之一的废墟中心放一个
**残片龛**。名字碑（`blank_plaque` 作为成品方块）仍未落地，
方案见 `docs/story-bible.md` §7.3。

### 9.4 共鸣只有提示，没有第二段文本

`PlayerProgress.isResonant()` 已实现判定，但幻境屏幕只显示一行「回响共鸣」提示。
**这是叙事上最值得补的一块**：目前玩家无法在游戏内拼出记忆之间的因果链。
（v2.0.0 的 12 段残片从侧面缓解了这一点——历史因果现在可以靠收集拼出来，
但**记忆之间**的共鸣文本仍然只有一行。）
方案与文案草案见 `docs/story-bible.md` §7.1。

### 9.5 ~~记忆卷轴是普通物品~~（已解决）

`memory_scroll` 已实现为记忆图鉴界面（`MemoryCodexScreen`），
支持阅读已见证的记忆、查看划掉的条目与侵蚀状态。

### 9.6 残片龛的发放依赖背包状态

`FragmentNicheBlock.takeNextFragment()` 靠**遍历背包**判断玩家还没有哪一段。
这在单次交互里是 O(12)，完全不是问题；但它有一个语义特性值得记住：
**玩家把残片存进箱子后，可以去下一个龛再拿一段**。

这是刻意的取舍——按进度存档判定会让丢掉的段落永久不可恢复，
而「丢了就能再拿」的坏处只是玩家可以多拿几份重复的文本。
两者相比，前者是死锁，后者只是冗余。

---

## 10. 修改指南（常见任务）

### 加一段新记忆

1. `MemoryRegistry.MEMORIES` 增加 `MemoryEntry`（索引即 ID，**不要插在中间**——
   进度位掩码按索引存储，插中间会让老存档错位）。
2. 语言文件为 21 个语言各加 3 个键（title / content / clue），跑 `check_lang.py`。
3. 若需要新的连接线索，同步更新其它条目的 `connectedClues`。
4. 注意 `MemorySyncPacket` 用 **32 位掩码**，因此记忆总数**上限 32**。

### 加一个新方块

1. `ModBlocks` 注册，`ModItems` 注册对应 `BlockItem`。
2. 在 `ModBlockStateProvider.registerStatesAndModels()` 加一行
   （`simpleBlockWithItem` 最省事）。
3. 贴图放到 `textures/block/`，**跑 `gradlew runData` 验证贴图存在**
   （不存在会直接报错，这正是想要的）。
4. 语言文件加 `block.echoesofoblivion.<name>`。

### 改侵蚀值曲线

`Config.corruptionGainRate`（自然增长/秒）；走廊内的额外增长在
`CorruptionEventHandler.onPlayerTick` 里硬编码为 +2；收集记忆的惩罚在
`ServerPacketHandler.onCrystalUse` 里为 +6；Boss 光环在
`SilentAggregateEntity.applyFearAura`（近 4 / 远 2）。

**改完记得同步 `PlayerProgress.MAX_CORRUPTION` 的假设**——
影子状态阈值（20/40/60/80）与雾的起始点（`CorruptionVisuals.FOG_ONSET = 0.35`）
都按 0~100 设计。

### 发布新版本

```bash
# 1. 改 build.gradle 的 version（格式 <模组版本>-mc<MC版本>-<加载器>）
# 2. 同步 mods.toml 的 version
# 3. 更新 CHANGELOG.md
git commit -am "..."
git tag v1.1.0-mc1.20.6-forge
git push origin main --tags
```

推 tag 后 `.github/workflows/build.yml` 会自动构建、跑测试、创建 Release 并附上 jar。
详见 `PUBLISHING.md`。
