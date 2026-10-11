package com.clion.echoesofoblivion.gametest;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.clion.echoesofoblivion.memory.Contradictions;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.server.NameSteleObservation;
import com.clion.echoesofoblivion.item.LoreFragmentItem;
import com.clion.echoesofoblivion.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * 服务端逻辑的自动化验证。运行方式：{@code gradlew runGameTestServer}
 *
 * <h2>为什么这里刻意不调用 {@code entity.tick()}</h2>
 *
 * <p>实测在 Forge 50.2.10 的 gameTest 环境下，<b>任何会移动的 {@code LivingEntity}
 * 在 tick 时都会抛异常</b>，堆栈为：
 * <pre>
 * ForgeConfigSpec$ConfigValue.get
 *   &lt;- ForgeHooks.isLivingOnLadder      （Forge 自己的代码）
 *   &lt;- LivingEntity.onClimbable
 *   &lt;- LivingEntity.tick
 * </pre>
 * 原因是 {@code ForgeHooks.isLivingOnLadder} 读取 {@code ForgeConfig.SERVER}，
 * 而游戏测试服务器不加载服务端配置——这是<b>环境限制，与被测模组无关</b>。
 *
 * <p>因此本类只做<em>不依赖实体 tick</em> 的断言：静态数据一致性、直接方法调用、
 * 方块放置、属性注册。凡是需要实体真正跑 tick 才能验证的行为（恐惧光环累积、
 * 亡魂追击、Boss 阶段随时间的表现），交给 {@code runClient} 实机验证。
 */
@GameTestHolder(EchoesOfOblivionMod.MOD_ID)
public final class BossGameTests {

    private BossGameTests() {
    }

    /**
     * 常规武器必须被完全免疫：{@code hurt} 返回 false 且生命值不变。
     *
     * <p>这是「概念 Boss 战」设计的地基——若失效，玩家可以拿剑砍死 Boss，
     * 「以记忆为武器」的整套玩法就没有意义了。
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void aggregateIsImmuneToWeapons(GameTestHelper helper) {
        SilentAggregateEntity boss = createWithoutTicking(helper, ModEntities.SILENT_AGGREGATE.get());
        if (boss == null) {
            return;
        }
        float healthBefore = boss.getHealth();

        boolean damaged = boss.hurt(helper.getLevel().damageSources().generic(), 1000.0f);

        if (damaged) {
            helper.fail("Boss 不应被常规伤害命中，但 hurt() 返回了 true");
            return;
        }
        if (Math.abs(boss.getHealth() - healthBefore) > 0.001f) {
            helper.fail("Boss 生命值被改变了: " + healthBefore + " -> " + boss.getHealth());
            return;
        }
        helper.succeed();
    }

    /** 稳定性从满值扣到 0 的过程中，阶段应依次推进 0 → 1 → 2，并在归零时报告被击败。 */
    @GameTest(template = "echoesofoblivion:empty")
    public static void stabilityDrivesPhase(GameTestHelper helper) {
        SilentAggregateEntity boss = createWithoutTicking(helper, ModEntities.SILENT_AGGREGATE.get());
        if (boss == null) {
            return;
        }

        if (boss.getPhase() != 0) {
            helper.fail("初始阶段应为 0，实际为 " + boss.getPhase());
            return;
        }
        if (boss.getStability() != SilentAggregateEntity.MAX_STABILITY) {
            helper.fail("初始稳定性应为 " + SilentAggregateEntity.MAX_STABILITY
                + "，实际为 " + boss.getStability());
            return;
        }

        boss.reduceStability(45);
        if (boss.getPhase() != 1) {
            helper.fail("稳定性 " + boss.getStability() + " 时应为阶段 1，实际为 " + boss.getPhase());
            return;
        }

        boss.reduceStability(30);
        if (boss.getPhase() != 2) {
            helper.fail("稳定性 " + boss.getStability() + " 时应为阶段 2，实际为 " + boss.getPhase());
            return;
        }

        if (!boss.reduceStability(100)) {
            helper.fail("稳定性归零时应返回「已击败」，实际稳定性为 " + boss.getStability());
            return;
        }
        if (boss.getStability() != 0) {
            helper.fail("稳定性不应为负，实际为 " + boss.getStability());
            return;
        }
        helper.succeed();
    }

    /** 记忆库必须自洽：索引/ID/线索三种查找互相一致，且连接线索不自指。 */
    @GameTest(template = "echoesofoblivion:empty")
    public static void memoryRegistryIsConsistent(GameTestHelper helper) {
        int size = MemoryRegistry.size();
        if (size != 5) {
            helper.fail("记忆条目数应为 5，实际为 " + size);
            return;
        }
        for (int i = 0; i < size; i++) {
            var entry = MemoryRegistry.byIndex(i);
            if (MemoryRegistry.indexOf(entry.id()) != i) {
                helper.fail("indexOf 与 byIndex 不一致，索引 " + i);
                return;
            }
            if (MemoryRegistry.byId(entry.id()).isEmpty()) {
                helper.fail("byId 找不到 " + entry.id());
                return;
            }
            if (MemoryRegistry.byClue(entry.clue()).isEmpty()) {
                helper.fail("byClue 找不到线索 " + entry.clue());
                return;
            }
            // 连接线索若包含自身，共鸣判定会永远无法成立
            if (entry.connectedClues().contains(entry.clue())) {
                helper.fail("记忆 " + entry.id() + " 的连接线索包含了自己");
                return;
            }
        }
        helper.succeed();
    }

    /** 记忆水晶可放置、可被识别。 */
    @GameTest(template = "echoesofoblivion:empty")
    public static void memoryCrystalIsPlaceable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.MEMORY_CRYSTAL.get());
        helper.assertBlockPresent(ModBlocks.MEMORY_CRYSTAL.get(), pos);
        helper.succeed();
    }

    /**
     * v2.0.0：残片龛可放置、可被识别。
     *
     * <p>它是 12 段残片在世界里的唯一来源（残片刻意没有合成配方），
     * 因此这个方块一旦注册出错，整套考古叙事在生存模式下就不可达。
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void fragmentNicheIsPlaceable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.FRAGMENT_NICHE.get());
        helper.assertBlockPresent(ModBlocks.FRAGMENT_NICHE.get(), pos);
        helper.succeed();
    }

    /**
     * v2.0.0：残片注册表必须完整且命名一致。
     *
     * <p>三个容易静默出错的地方：
     * <ol>
     *   <li>段数不是 12（漏注册一段，玩家永远集不齐）。</li>
     *   <li>注册名不是 {@code fragment_<id>}——{@code ModItems} 按这个规则注册，
     *       语言键也按这个拼；一旦脱节，物品栏里就显示原始键名
     *       （v2.0.0 修过这个缺陷，加测试防止复发）。</li>
     *   <li>{@code fragmentId} 与注册表键不一致——{@code FragmentNicheBlock}
     *       与语言键都依赖它。</li>
     * </ol>
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void loreFragmentsAreRegistered(GameTestHelper helper) {
        var fragments = ModItems.fragments();
        if (fragments.size() != 12) {
            helper.fail("残片应为 12 段，实际注册了 " + fragments.size() + " 段");
            return;
        }
        for (var entry : fragments.entrySet()) {
            var item = entry.getValue().get();
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getPath().equals("fragment_" + entry.getKey())) {
                helper.fail("残片 " + entry.getKey() + " 的注册名应为 fragment_"
                    + entry.getKey() + "，实际为 " + id);
                return;
            }
            if (!(item instanceof LoreFragmentItem fragment)) {
                helper.fail("残片 " + entry.getKey() + " 不是 LoreFragmentItem");
                return;
            }
            if (!fragment.fragmentId().equals(entry.getKey())) {
                helper.fail("残片 " + entry.getKey() + " 的 fragmentId 是 "
                    + fragment.fragmentId());
                return;
            }
        }
        helper.succeed();
    }

    /**
     * v2.2.0：见证顺序必须被真实记录，且逆序判定要正确。
     *
     * <p>这一版的核心机制是「按错误顺序见证会浮现矛盾的碎片」，
     * 而它完全依赖顺序表。顺序表一旦失效，机制会**静默**消失——
     * 玩家只是永远看不到矛盾碎片，没有任何报错。
     * 因此这里的断言是必要的，不是锦上添花。
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void witnessOrderIsTracked(GameTestHelper helper) {
        PlayerProgress progress = new PlayerProgress();

        // 空进度：任何比较都应为 false
        if (progress.witnessedBefore(0, 1)) {
            helper.fail("空进度不应该判定出任何先后关系");
            return;
        }

        // 按 4 → 3 → 1 的顺序见证
        progress.collect(4);
        progress.collect(3);
        progress.collect(1);

        // 顺序表应保留见证次序，而不是像 Set 那样无序
        if (!progress.witnessOrder().equals(java.util.List.of(4, 3, 1))) {
            helper.fail("见证顺序应为 [4, 3, 1]，实际为 " + progress.witnessOrder());
            return;
        }
        // 4 早于 3：这正是「先听到警告再看仪器」，应当被判出来
        if (!progress.witnessedBefore(4, 3)) {
            helper.fail("4 早于 3 见证，witnessedBefore(4,3) 应为 true");
            return;
        }
        // 反向应为 false
        if (progress.witnessedBefore(3, 4)) {
            helper.fail("witnessedBefore(3,4) 应为 false");
            return;
        }
        // 未见证的索引不参与判定
        if (progress.witnessedBefore(0, 4) || progress.witnessedBefore(4, 2)) {
            helper.fail("未见证的记忆不应参与顺序判定");
            return;
        }

        // 重复见证不应改变顺序（重读旧记忆不能把它挪到末尾）
        progress.collect(4);
        if (!progress.witnessOrder().equals(java.util.List.of(4, 3, 1))) {
            helper.fail("重复见证改变了顺序表：" + progress.witnessOrder());
            return;
        }

        // 焚烧（forget）必须同时清掉顺序表，
        // 否则后续判定会引用一段已经被消耗掉的记忆
        progress.forget(4);
        if (progress.witnessOrder().contains(4)) {
            helper.fail("焚烧后顺序表仍含有被烧掉的记忆：" + progress.witnessOrder());
            return;
        }

        helper.succeed();
    }

    /**
     * v2.2.0：矛盾碎片的选择必须稳定且只在真正逆序时触发。
     *
     * <p>顺带校验 {@code Contradictions.REVERSALS} 里每个键都确实存在于
     * {@code MemoryRegistry} 的索引范围内——索引写错会让机制静默失效。
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void contradictionsTriggerOnlyInReverseOrder(GameTestHelper helper) {
        int size = MemoryRegistry.size();
        for (Contradictions.Reversal reversal : Contradictions.REVERSALS) {
            if (reversal.earlier() < 0 || reversal.earlier() >= size
                || reversal.later() < 0 || reversal.later() >= size) {
                helper.fail("逆序对的索引超出记忆范围：" + reversal);
                return;
            }
            if (reversal.earlier() == reversal.later()) {
                helper.fail("逆序对的两端相同：" + reversal);
                return;
            }
        }

        // 正序见证：不该有任何矛盾碎片
        PlayerProgress inOrder = new PlayerProgress();
        for (int i = 0; i < size; i++) {
            inOrder.collect(i);
        }
        if (Contradictions.forPlayer(inOrder) != null) {
            helper.fail("按 0..4 顺序见证不应触发矛盾碎片，实际触发 "
                + Contradictions.forPlayer(inOrder));
            return;
        }

        // 逆序见证：先 4 再 3，应命中「警告在仪器之前」那一对
        PlayerProgress reversed = new PlayerProgress();
        reversed.collect(4);
        reversed.collect(3);
        String key = Contradictions.forPlayer(reversed);
        if (key == null) {
            helper.fail("先见证 4 再见证 3 应当触发矛盾碎片");
            return;
        }
        if (!key.equals("message.echoesofoblivion.contradiction.warning_before_apparatus")) {
            helper.fail("命中的矛盾碎片不是优先级最高的那一对，实际为 " + key);
            return;
        }
        if (!Contradictions.isReversalKey(key)) {
            helper.fail("命中的键不在已登记的逆序对里：" + key);
            return;
        }

        helper.succeed();
    }

    /**
     * v2.2.0：「第一段没说完的话」依赖的残片必须存在。
     *
     * <p>渲染层按 ID 硬编码了一段残片（见 {@code MemoryVisionScreen}）。
     * 它按 **lore fragment** 的 ID 匹配（{@code her_last_note}），
     * 因此这里断言这个 ID 确实已注册——否则那个细节会静默失效，
     * 而它恰恰是这一版的主题句。
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void unfinishedFragmentExists(GameTestHelper helper) {
        if (!ModItems.fragments().containsKey("her_last_note")) {
            helper.fail("「第一段没说完的话」依赖的残片 her_last_note 未注册");
            return;
        }
        helper.succeed();
    }

    /**
     * v2.3.0：名字碑的方块实体与「刻名代价」的倍率必须成立。
     *
     * <p>两个容易静默出错的地方：
     * <ol>
     *   <li>方块实体的注册名与方块对不上——碑会放下去但立刻变成「无效方块实体」，
     *       刻名静默失败。</li>
     *   <li>倍率没被应用或应用成了 &lt;= 1.0——刻名变成一个纯亏的装饰行为，
     *       而这正是这一版要避免的（代价必须真实，收益必须是零）。</li>
     * </ol>
     */
    @GameTest(template = "echoesofoblivion:empty")
    public static void nameSteleIsInscribable(GameTestHelper helper) {
        // 方块实体类型必须已注册，且绑定了名字碑方块
        if (ModBlocks.NAME_STELE_BE.get() == null) {
            helper.fail("名字碑的方块实体类型未注册");
            return;
        }
        if (!ModBlocks.NAME_STELE_BE.get().isValid(ModBlocks.NAME_STELE.get().defaultBlockState())) {
            helper.fail("名字碑的方块实体类型没有绑定到名字碑方块");
            return;
        }

        // 倍率必须是「加快」，而且严格大于 1
        if (NameSteleObservation.CORRUPTION_MULTIPLIER <= 1.0f) {
            helper.fail("刻名的侵蚀倍率应大于 1，实际为 "
                + NameSteleObservation.CORRUPTION_MULTIPLIER);
            return;
        }
        // 应用倍率：3 点增长 → 约 4 点
        int base = 4;
        int scaled = NameSteleObservation.applyMultiplier(base,
            NameSteleObservation.CORRUPTION_MULTIPLIER);
        if (scaled <= base) {
            helper.fail("倍率未被应用：" + base + " → " + scaled);
            return;
        }
        // 观测语句必须有内容，否则刻名之后什么都不会发生
        if (NameSteleObservation.observationCount() < 3) {
            helper.fail("观测语句过少：" + NameSteleObservation.observationCount());
            return;
        }

        // 方块可放置
        BlockPos pos = new BlockPos(2, 2, 3);
        helper.setBlock(pos, ModBlocks.NAME_STELE.get());
        helper.assertBlockPresent(ModBlocks.NAME_STELE.get(), pos);

        helper.succeed();
    }

    /** 两个实体的属性必须已注册，否则创建时会抛异常。 */
    @GameTest(template = "echoesofoblivion:empty")
    public static void entitiesHaveAttributes(GameTestHelper helper) {        if (!checkAttributes(helper, ModEntities.SILENT_AGGREGATE.get(), "silent_aggregate")) {
            return;
        }
        if (!checkAttributes(helper, ModEntities.PHANTOM.get(), "phantom")) {
            return;
        }
        helper.succeed();
    }

    private static boolean checkAttributes(GameTestHelper helper, EntityType<?> type, String name) {
        Entity entity = type.create(helper.getLevel());
        if (entity == null) {
            helper.fail("无法创建实体: " + name);
            return false;
        }
        // getAttributes() 定义在 LivingEntity 上
        if (!(entity instanceof LivingEntity living)) {
            helper.fail("实体不是 LivingEntity: " + name);
            return false;
        }
        if (living.getAttributes() == null
            || !living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
            helper.fail("实体缺少已注册的属性: " + name);
            return false;
        }
        return true;
    }

    /**
     * 创建实体但<b>不</b>把它加入世界。
     *
     * <p>{@code helper.spawn()} 会把实体注册进 {@code EntityTickList} 并接受
     * {@code ServerLevel} 的 tick 驱动；在 gameTest 环境下这会因 Forge 读取未加载的
     * 服务端配置而崩溃（见类注释）。因此这里只用 {@code type.create(level)} 拿到实例，
     * 让断言在不受 tick 干扰的情况下检查纯逻辑。
     */
    private static <E extends Entity> E createWithoutTicking(GameTestHelper helper, EntityType<E> type) {
        E entity = type.create(helper.getLevel());
        if (entity == null) {
            helper.fail("无法创建实体: " + type.getDescriptionId());
            return null;
        }
        entity.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return entity;
    }
}
