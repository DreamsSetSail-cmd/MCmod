package com.clion.echoesofoblivion.gametest;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
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

    /** 两个实体的属性必须已注册，否则创建时会抛异常。 */
    @GameTest(template = "echoesofoblivion:empty")
    public static void entitiesHaveAttributes(GameTestHelper helper) {
        if (!checkAttributes(helper, ModEntities.SILENT_AGGREGATE.get(), "silent_aggregate")) {
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
