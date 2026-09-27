# Echoes of Oblivion 技术大纲

技术架构总览：环境、包结构、核心系统、关键技术点与参考实现。

---

## 1. 构建环境

| 项目 | 值 |
| --- | --- |
| Minecraft | 1.20.6 |
| Forge | 50.2.0（`forge_version_range=[0,)`） |
| Java | 21（`java.toolchain.languageVersion = 21`） |
| 映射 | Parchment `2024.06.16-1.20.6`（官方名 + 社区参数名/Javadoc） |
| ForgeGradle | `net.minecraftforge.gradle` `[6.0.24, 6.2)` + Parchment Librarian `1.+` |
| 构建任务 | `build` / `runClient` / `runServer` / `runData` / `gameTestServer` |
| 数据生成 | `runData --mod echoesofoblivion --all --output src/generated/resources/ --existing src/main/resources/` |
| 关键开关 | `reobf = false`（运行时使用官方映射）、`copyIdeResources = true` |
| 资源合并 | `sourceSets.main.resources` 包含 `src/generated/resources`，`processResources` 用 `${...}` 占位符展开 `gradle.properties` 属性 |

### 1.1 构建产物

- JAR 清单：`Specification-Title=echoesofoblivion`、`Implementation-Version=mod_version` 等
- `maven-publish` 发布到 `mcmodsrepo`（`file://${projectDir}/mcmodsrepo`）

---

## 2. 包结构（完整规划）

```
src/main/java/com.dreamcolin.echoesofoblivion
├── EchoesOfOblivionMod.java        # 主类：@Mod，MOD_ID，注册全部 DeferredRegister
├── Config.java                     # ForgeConfigSpec（COMMON / CLIENT 两个 Spec）
├── ModBlocks.java                  # DeferredRegister<Block>，方块属性工厂
├── ModItems.java                   # DeferredRegister<Item>，BlockItem 绑定
├── ModCreativeTabs.java            # DeferredRegister<CreativeModeTab>
├── ModDimensions.java              # DeferredRegister<DimensionType>，维度 key
├── ModWorldGen.java                # 生物群系 / 结构 / 地物 / 刷怪规则注册入口
├── ModSounds.java                  # DeferredRegister<SoundEvent>
├── ModEntities.java                # DeferredRegister<EntityType<?>>
├── ModBiomeModifiers.java          # (可选) biome modifier 挂接刷怪
├── block/
│   ├── MemoryCrystalBlock.java     # 水晶方块（use 交互 → 数据包）
│   └── UnstablePortalBlock.java    # 不稳定传送门方块（阶段 2）
├── block/entity/                   # （按需）方块实体：水晶动画状态等
├── item/                           # 自定义物品（传送道具、记忆卷轴等）
├── entity/
│   ├── SilentAggregateEntity.java  # Boss：伤害免疫、记忆攻击、阶段机
│   ├── SilentAggregateRenderer.java# Boss 渲染（粒子/拼合外观）
│   ├── PhantomEntity.java          # 亡魂（镜像区块刷怪）
│   └── PhantomRenderer.java
├── memory/
│   ├── MemoryData.java             # 记忆内容：ID、标题、正文、线索、解锁条件
│   ├── MemoryRegistry.java         # 记忆注册表（DeferredRegister<Codec> 或静态表）
│   ├── MemoryManager.java          # 收集/校验/触发逻辑（服务端）
│   └── PlayerMemoryData.java       # SavedData：已收集 ID 集合
├── network/
│   ├── ModNetwork.java             # SimpleChannel 注册 + 包枚举
│   ├── MemoryVisionPacket.java     # S2C 幻境内容（StreamCodec）
│   ├── MemoryCrystalUsePacket.java # C2S 触摸水晶
│   ├── ShadowSyncPacket.java       # S2C 影子状态
│   └── RealityShiftPacket.java     # S2C 镜像区块/侵蚀同步
├── world/
│   ├── biome/SilentCorridorBiome.java
│   ├── structure/RuinsStructure.java        # 废墟结构 + piece
│   ├── feature/MemoryCrystalFeature.java    # 水晶散布地物
│   └── dimension/SilentCorridorGenerator.java # (可选) 自定义 ChunkGenerator
├── client/
│   ├── ClientEvents.java           # @OnlyIn 客户端事件：屏幕打开、渲染钩子
│   ├── screen/MemoryVisionScreen.java
│   ├── render/ShadowLayerRenderer.java      # 影子异常层
│   ├── render/CorruptionOverlayRenderer.java# 侵蚀值 Overlay
│   └── effect/CorruptionShaders.java        # 后期着色器管理
├── util/
│   ├── CorridorTeleportHelper.java # 维度传送逻辑
│   └── RandomUtils.java
└── datagen/
    ├── ModDataGenerators.java      # 聚合所有 Provider
    ├── ModBlockStateProvider.java
    ├── ModItemModelProvider.java
    ├── ModLootTableProvider.java
    ├── ModRecipeProvider.java
    ├── ModTagProvider.java
    └── ModDimensionProvider.java   # 维度/生物群系 JSON
```

---

## 3. 注册体系

### 3.1 统一入口模式

```java
@Mod(EchoesOfOblivionMod.MOD_ID)
public class EchoesOfOblivionMod {
    public static final String MOD_ID = "echoesofoblivion";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoesOfOblivionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModDimensions.DIMENSION_TYPES.register(modBus);
        ModWorldGen.register(modBus);          // biome/structure/feature 注册器
        ModNetwork.register();                  // SimpleChannel 静态初始化
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
```

### 3.2 注册表清单与对应事件

| 注册内容 | 注册表 Key | 关键事件 |
| --- | --- | --- |
| 方块 | `ForgeRegistries.BLOCKS` | `RegisterEvent` |
| 物品 | `ForgeRegistries.ITEMS` | `RegisterEvent` |
| 方块实体 | `ForgeRegistries.BLOCK_ENTITY_TYPES` | `RegisterEvent` |
| 实体 | `ForgeRegistries.ENTITY_TYPES` | `EntityAttributeCreationEvent`（属性）、`EntityRenderersEvent.RegisterRenderers`（渲染器）、`EntityRenderersEvent.RegisterLayerDefinitions`（模型层） |
| 音效 | `ForgeRegistries.SOUND_EVENTS` | `RegisterEvent` |
| 创造标签页 | `Registries.CREATIVE_MODE_TAB` | `BuildCreativeModeTabContentsEvent`（填物品） |
| 维度类型 | `Registries.DIMENSION_TYPE` | 数据驱动，`DeferredRegister<DimensionType>` |
| 生物群系 | `Registries.BIOME` | 数据驱动（JSON） |
| 结构 | `Registries.STRUCTURE_TYPE` | `RegisterEvent` |
| 地物 | `Registries.FEATURE` | `RegisterEvent` |

> 注意：1.20.6 中 `Registries.X` 为 `ResourceKey` 来源；数据驱动对象（维度/群系）可完全用 `runData` 输出 JSON，不必在代码注册。

### 3.3 事件订阅风格

- MOD 总线：`@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD)` → 注册、配置、属性、渲染器
- FORGE 总线：`@Mod.EventBusSubscriber(modid = MOD_ID)` → 世界 tick、刷怪、实体事件、按键

---

## 4. 维度（Silent Corridor）

### 4.1 维度类型 JSON（`data/echoesofoblivion/dimension_type/silent_corridor.json`）

```json
{
  "ultrawarm": false,
  "natural": false,
  "coordinate_scale": 2.0,
  "has_skylight": false,
  "has_ceiling": true,
  "ambient_light": 0.05,
  "fixed_time": 18000,
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0,
  "effects": "echoesofoblivion:corridor_effects",
  "min_y": 0,
  "height": 256,
  "logical_height": 256,
  "infiniburn": "#minecraft:infiniburn_overworld"
}
```

要点：
- `fixed_time`：固定夜晚时间（无昼夜）
- `effects`：可自定义 `DimensionSpecialEffects`（暗色天空盒、雾）
- `ambient_light` 极低：制造死寂昏暗
- `monster_spawn_light_level=0`：按光照规则不刷怪（配合自定义群系双重控制）

### 4.2 维度声明（`data/echoesofoblivion/dimension/silent_corridor.json`）

```json
{
  "type": "echoesofoblivion:silent_corridor",
  "generator": {
    "type": "minecraft:noise",
    "settings": "echoesofoblivion:corridor_noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "echoesofoblivion:silent_biomes"
    }
  }
}
```

- 噪声设置：`worldgen/noise_settings`（基岩层、洞穴规则、表面规则——废墟地表）
- 群系源：`multi_noise` 或 `fixed`（整个维度单一群系最省事）

### 4.3 生物群系 JSON（`worldgen/biome/silent_corridor.json`）

- `temperature/humidity`：极冷极干
- `effects`：暗灰天空色、雾密度、无音乐；`sky_color`、`fog_color`、`water_color` 全灰
- `spawners`：**完全空**（`monster/creature/ambient` 均无条目）
- `carvers`/`features`：只挂 `memory_crystal` 散布地物与废墟结构
- `downfall=0`、`precipitation=none`

### 4.4 结构（废墟遗迹）

```java
public class RuinsStructure extends Structure {
    public static final Codec<RuinsStructure> CODEC = RecordCodecBuilder.create(b ->
        b.group(Structure.settingsCodec()).apply(b, RuinsStructure::new));

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onTopOfChunkCenter(ctx, Heightmap.Types.WORLD_SURFACE_WG,
            builder -> builder.addPiece(new RuinsPiece(ctx.structureTemplateManager())));
    }
}
```

- 注册：`DeferredRegister<StructureType<?>>` + `BuiltinRegistries.STRUCTURES` 数据驱动（`worldgen/structure` + `structure_set` JSON）
- Piece：`StructurePiece` 子类用 `StructureTemplate` 拼接废墟建筑（`runData` 可输出 NBT 模板）
- 结构集：`worldgen/structure_set/corridor_ruins.json`（`placement` 间距 24~48 区块，`salt` 固定）

### 4.5 地物（水晶散布）

```java
public class MemoryCrystalFeature extends Feature<MemoryCrystalFeatureConfig> {
    public MemoryCrystalFeature() {
        super(MemoryCrystalFeatureConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<MemoryCrystalFeatureConfig> ctx) {
        // 1. 随机选取方块位置（地表 ± 偏移）
        // 2. 检查放置合法性（空气/替换表层方块、不贴水）
        // 3. 放置 1~4 个 memory_crystal（random 旋转/微偏移）
        return true;
    }
}
```

- `ConfiguredFeature`/`PlacedFeature` JSON：`worldgen/configured_feature/memory_crystal_scatter.json`、`worldgen/placed_feature/...`
- 挂到群系 `features` 列表中（`["echoesofoblivion:memory_crystal_scatter"]`）

### 4.6 传送

- 方式 A（推荐）：物品/方块右键 → 服务端 `TeleportationHelper`：

```java
public static void teleportToCorridor(ServerPlayer player) {
    ServerLevel target = player.getServer().getLevel(CorridorDimension.KEY);
    // 以玩家当前位置为基础，按 coordinate_scale 换算，加随机偏移（不稳定感）
    BlockPos dest = player.blockPosition().offset(
        player.getRandom().nextInt(-32, 33), 0, player.getRandom().nextInt(-32, 33));
    player.teleportTo(target, dest.getX(), target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, dest).getY(),
        dest.getZ(), player.getYRot(), player.getXRot());
}
```

- 方式 B：`UnstablePortalBlock` + `PortalShape` 判定（镜像原版末地门逻辑）
- 返回主世界：走廊内同样交互，注意 `teleportTo` 的 `SetSpawnPoint` 参数处理

---

## 5. 网络层

### 5.1 通道注册

```java
public static final String VERSION = "1";
public static SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        EchoesOfOblivionMod.id("main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals);

public static void register() {
    CHANNEL.registerMessage(0, MemoryCrystalUsePacket.class,
        MemoryCrystalUsePacket.STREAM_CODEC, ServerHandler::onCrystalUse);
    CHANNEL.registerMessage(1, MemoryVisionPacket.class,
        MemoryVisionPacket.STREAM_CODEC, ClientHandler::onMemoryVision);
    // 2: ShadowSyncPacket, 3: RealityShiftPacket, 4: BossStatePacket ...
}
```

### 5.2 数据包编码（StreamCodec + RegistryFriendlyByteBuf）

```java
public record MemoryVisionPacket(ResourceLocation memoryId, int crystalColor) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryVisionPacket> STREAM_CODEC =
        StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MemoryVisionPacket::memoryId,
            ByteBufCodecs.VAR_INT, MemoryVisionPacket::crystalColor,
            MemoryVisionPacket::new);
}
```

- 服务端处理：校验玩家权限/距离/冷却 → 调用 `MemoryManager` → 回包
- 客户端处理：`DistExecutor.unsafeRunWhenOn(Dist.CLIENT)` 中 `Minecraft.getInstance().setScreen(...)`
- 方向约定：玩家交互 C2S（校验），服务端权威后再 S2C 广播

### 5.3 包清单

| 包 | 方向 | 内容 | 用途 |
| --- | --- | --- | --- |
| `MemoryCrystalUsePacket` | C2S | 方块坐标 | 触摸水晶请求 |
| `MemoryVisionPacket` | S2C | memoryId、颜色 | 打开幻境屏幕 |
| `ShadowSyncPacket` | S2C | 影子状态码 | 影子异常表现同步 |
| `RealityShiftPacket` | S2C | 感染区块列表增量 | 镜像区块状态同步 |
| `BossStatePacket` | S2C | 阶段/稳定性 | Boss 战斗状态 |

---

## 6. 持久化（SavedData / Capability）

### 6.1 玩家记忆进度（SavedData）

```java
public class PlayerMemoryData extends SavedData {
    private final Set<ResourceLocation> collected = new HashSet<>();

    public static PlayerMemoryData get(ServerLevel level) {
        return level.getDataStorage()
            .computeIfAbsent(Factory, "echoesofoblivion_player_memory");
    }
    // serialize 用 CompoundTag: ListTag of memory ids
    // setDirty() 每次收集后调用
}
```

> 1.20.6 存档命名空间：`level.getDataStorage()`；若需跨存档全局数据用 `ServerLifecycleHooks`。

### 6.2 镜像区块集合（全局 SavedData）

```java
public class RealityData extends SavedData {
    private final Set<ChunkPos> infected = new HashSet<>();
    // 扩散 tick：随机游走从边缘向外扩展，setDirty() 持久化
}
```

- 服务端权威：扩散只在 `ServerLevel` 的 `LevelTickEvent` 中推进
- 客户端只读：通过 `RealityShiftPacket` 增量同步

### 6.3 侵蚀值

- 方案 A：玩家 `SavedData`（推荐，简单可靠）
- 方案 B：`AttachCapabilitiesEvent<Entity>` + `ICapabilityProvider`（与旧版本兼容性更好）
- 客户端显示：仅基于收到的数值做 Overlay，不在客户端改逻辑值

---

## 7. 客户端渲染

### 7.1 幻境屏幕（MemoryVisionScreen）

```java
public class MemoryVisionScreen extends Screen {
    private int ticks = 0;
    // onInit: 全屏黑 + 渐入（alpha 随时间）
    // render(): 
    //   1. RenderSystem.enableBlend / defaultBlendFunc
    //   2. 绘制全屏黑色矩形（透明度渐入渐出）
    //   3. 绘制记忆文本（FormattedText，居中，带抖动偏移）
    //   4. 渐变色覆盖（按 memoryId 的 crystalColor 混合）
    // 按键 Esc/点击跳过
}
```

- 应继承 `Screen` 而非 `ContainerScreen`（无容器）
- 渲染需在 `GAME` 线程，`tick()` 更新动画时间
- 性能：全屏矩形用 `Tesselator` 绘制一次即可，避免每帧创建 buffer

### 7.2 影子异常（渲染层）

```java
// 方案：覆写 PlayerRenderer 或添加 RenderLayer
public class ShadowLayerRenderer extends EntityRenderer<Player> {
    // 客户端定时器驱动影子行为状态机：
    //   IDLE → POINTING → TREMBLING → LAGGING/AHEAD
    // 渲染：绘制玩家第二份模型/投影，应用以下变换：
    //   - 延迟/超前：用历史 tick 位置的姿态（保存玩家最近 N 帧的姿态快照）
    //   - 指向：手臂旋转指向目标方向（需要手动应用 limb swing 之外的旋转）
    //   - 颤抖：小幅随机平移/旋转，频率随侵蚀值升高
}
```

- 只影响渲染，不影响碰撞/逻辑（纯客户端，避免同步问题）
- 姿态历史：`clientLevel` 内维护 `ArrayDeque<PlayerPose>`（每 tick 入队、弹出旧帧）

### 7.3 侵蚀值 Overlay + 后期效果

```java
@SubscribeEvent
public static void onRenderOverlay(CustomizeGuiOverlayEvent event) {
    float value = ClientCorruptionState.get();   // 客户端缓存
    if (value <= 0) return;
    // 1. Vignette：绘制径向渐变贴图，透明度 ∝ value
    // 2. 色偏：RenderSystem.setShaderColor 对 GUI 叠加冷色调
    // 3. 晃动：event.setPartialTick 期间偏移 renderTarget 采样（或改用着色器）
}
```

- 可叠加全屏 `PostEffect`（`Minecraft.getInstance().gameRenderer` 的 `PostChain`），1.20.6 在 `RenderLevelStageEvent` 或 `ViewportEvent` 相关时机触发；注意性能与兼容性，优先用 Overlay + 简单色偏方案。
- 幻影闪现：`RenderLevelStageEvent` 阶段内绘制半透明贴图（距离随侵蚀值变化）。

### 7.4 Boss 渲染

```java
public class SilentAggregateRenderer extends MobRenderer<SilentAggregateEntity, ...> {
    // 阶段 1: 半透明旋涡粒子 + 方块拼合（使用 BlockModelShaper 渲染几组黑曜石/紫珀方块）
    // 阶段 2: 表面裂隙贴图交替 + 粒子暴增
    // 阶段 3: 不稳定抖动 + 收敛坍塌动画（实体缩放 scale 逼近 0 时触发死亡演出）
}
```

- 阶段切换由服务端同步 `BossStatePacket` 驱动，客户端仅渲染

---

## 8. 实体

### 8.1 SilentAggregateEntity（Boss）

```java
public class SilentAggregateEntity extends Monster {
    // 属性：maxHealth 大、knockbackResistance 1、movementSpeed 低、followRange 大
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);   // 允许 /kill、虚空伤害
        }
        // 播放免疫反馈（粒子 + 音效），返回 false
        return false;
    }
    // 阶段状态机：stability 0~100，由 memory 攻击降低
    // tick(): 恐惧光环 → 对范围内玩家施加侵蚀增长（服务端）
    //        ：按阶段切换技能（召唤幻影、扭曲空间、吞噬射线）
}
```

### 8.2 PhantomEntity（亡魂）

```java
public class PhantomEntity extends Mob {
    // AI 目标：跟踪最近玩家（followRange 大）
    // 生成：MobSpawnEvent.FinalizeSpawn 中检查所在区块 ∈ 镜像区块集合，
    //       命中则改为 spawn phantom（并记入抑制计数）
    // 行为：不与玩家直接对拼，靠近后缓慢吸取侵蚀（与 Boss 光环协同）
}
```

### 8.3 注册与属性

```java
// ModEntities.java
DeferredRegister<EntityType<?>> ENTITIES =
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
RegistryObject<EntityType<SilentAggregateEntity>> SILENT_AGGREGATE = ENTITIES.register(
    "silent_aggregate", () -> EntityType.Builder.of(SilentAggregateEntity::new, MobCategory.MONSTER)
        .sized(3.5f, 4.0f).fireImmune().build("silent_aggregate"));

// 属性
@SubscribeEvent
public static void attributes(EntityAttributeCreationEvent event) {
    event.put(SILENT_AGGREGATE.get(),
        Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 800)
            .add(Attributes.FOLLOW_RANGE, 48)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .build());
}
```

---

## 9. 世界与游戏事件

| 事件 | 用途 |
| --- | --- |
| `ServerTickEvent` / `LevelTickEvent` | 镜像区块扩散定时器、Boss 全局判定 |
| `MobSpawnEvent.FinalizeSpawn` | 感染区块内追加/替换亡魂生成 |
| `PlayerTickEvent` | 侵蚀值自然衰减/累积（服务端） |
| `EntityJoinLevelEvent` | 进入维度时应用传送后处理（重生点、光照） |
| `PlayerChangedDimensionEvent` | 记录跨维度状态、触发返回叙事 |
| `CustomizeGuiOverlayEvent` | 客户端侵蚀值 Overlay |
| `RenderLevelStageEvent` | 幻影闪现、天空盒定制 |
| `BuildCreativeModeTabContentsEvent` | 标签页内容填充 |

---

## 10. 配置（Config.java）

```java
public class Config {
    // COMMON Spec（服务端生效）
    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue CORRUPTION_GAIN_RATE = COMMON
        .comment("每秒侵蚀值自然增长量 (0=关闭)")
        .defineInRange("corruptionGainRate", 1, 0, 100);
    public static final ForgeConfigSpec.IntValue MIRROR_EXPAND_INTERVAL = COMMON
        .comment("镜像区块扩散间隔(秒)")
        .defineInRange("mirrorExpandInterval", 300, 10, 3600);
    // CLIENT Spec（仅客户端）
    public static final ForgeConfigSpec.DoubleValue OVERLAY_INTENSITY = CLIENT
        .comment("侵蚀 Overlay 强度系数")
        .defineInRange("overlayIntensity", 1.0, 0.0, 3.0);

    public static final ForgeConfigSpec COMMON_SPEC = COMMON.build();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT.build();

    // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC) 在构造中调用
    // ModConfigEvent 中刷新静态缓存字段
}
```

---

## 11. 数据生成（runData）

```java
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD)
public class ModDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new ModBlockStateProvider(output, helper));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, provider));
        gen.addProvider(event.includeServer(), new ModLootTableProvider(output, provider));
        gen.addProvider(event.includeServer(), new ModTagProvider(output, provider, helper));
        gen.addProvider(event.includeServer(), new ModWorldGenProvider(output, provider));  // 维度/群系/结构 JSON
    }
}
```

输出：blockstate/model JSON、合成表、战利品表、标签、维度/群系/结构数据文件。

---

## 12. 资源文件规划

```
src/main/resources/
├── pack.mcmeta
├── META-INF/mods.toml
└── assets/echoesofoblivion/
    ├── blockstates/memory_crystal.json
    ├── blockstates/unstable_portal.json
    ├── models/block/memory_crystal.json
    ├── models/item/memory_crystal.json
    ├── textures/block/memory_crystal.png
    ├── textures/entity/phantom.png
    ├── lang/en_us.json
    ├── lang/zh_cn.json
    ├── sounds.json
    └── sounds/memory_vision.ogg ...
└── data/echoesofoblivion/
    ├── dimension/silent_corridor.json
    ├── dimension_type/silent_corridor.json
    ├── worldgen/biome/silent_corridor.json
    ├── worldgen/configured_feature/memory_crystal_scatter.json
    ├── worldgen/placed_feature/memory_crystal_scatter.json
    ├── worldgen/structure/corridor_ruins.json
    ├── worldgen/structure_set/corridor_ruins.json
    ├── worldgen/noise_settings/corridor_noise.json
    └── loot_tables/blocks/memory_crystal.json
```

---

## 13. 关键风险与注意点

1. **客户端/服务端分离**：所有 `Screen`、渲染、`DistExecutor` 保护；服务端不得引用 `Minecraft`。
2. **数据包同步**：一切状态变更以服务端为准，客户端只做表现；Boss 阶段、镜像区块、侵蚀值均需 S2C 增量同步。
3. **1.20.6 API 差异**：`StreamCodec`/`RegistryFriendlyByteBuf` 取代旧 `FriendlyByteBuf` 消息 API；注册表以 `Registries` 引用为准；`ItemStack` 不再作为 Capability Provider（如需用数据组件）。
4. **性能**：镜像区块判定用 `Set<ChunkPos>` 哈希；Boss 粒子按距离分级；Overlay 不做每帧分配。
5. **维度生成成本**：噪声设置与群系 JSON 易出兼容问题，优先用 `fixed`/`multi_noise` 默认管线，结构生成前先跑 `runData` 验证。
6. **存档兼容**：`SavedData` 需处理升级/缺省（`readTag` 容错、默认集合空）。
7. **音量与沉浸**：环境音效使用 `SoundEvent` 资源（ogg），注意 1.20.6 音效文件必须存在于 `sounds.json` 引用。
8. **崩溃兜底**：`gameTestServer` 任务可快速验证服务端加载；`runClient` 冒烟测试全流程。
