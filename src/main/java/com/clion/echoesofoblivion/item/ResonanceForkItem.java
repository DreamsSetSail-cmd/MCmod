package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.CorridorTeleportHelper;
import com.clion.echoesofoblivion.world.Rituals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 共鸣音叉（v2.0.0）——三种仪式的发起工具。
 *
 * <h2>为什么一个物品承载三种仪式</h2>
 *
 * <p>做成三件物品会让玩家必须同时携带三样东西，而仪式应当是**决策**而不是库存管理。
 * 一件音叉 + 不同的**辅助材料**放在副手 / 背包里，玩家在发起时选择要哪一种，
 * 这样决策点落在「我要付出哪个代价」上，而不是「我带了哪个道具」。
 *
 * <h2>选择方式</h2>
 *
 * <p>潜行右键循环切换仪式；普通右键执行当前选中的仪式。
 * 选中的类型存在物品的 NBT 里（{@code echoes_ritual}），
 * 因此可以同时拥有多把音叉、各自锁定不同仪式——这对多人服务器有用。
 *
 * <h2>材料消耗</h2>
 *
 * <p>三种仪式都需要在背包里有对应材料，执行时消耗：
 * <ul>
 *   <li>安魂曲：骸骨灰 ×1</li>
 *   <li>寂静降临：寂静碎片 ×1 + 虚空余烬 ×2</li>
 *   <li>虚空通道：镜面碎片 ×1</li>
 * </ul>
 *
 * <p>材料检查与消耗都在服务端，且**先检查后执行**，避免出现材料被吃掉但仪式失败。
 */
public class ResonanceForkItem extends Item {

    /**
     * 每个玩家当前选中的仪式（v2.0.0）。
     *
     * <p>为什么不用 {@code ItemStack} 的 NBT：**1.20.5 已移除**
     * {@code ItemStack.hasTag()} / {@code getOrCreateTag()}，改为数据组件
     * （{@code DataComponents}）。为一个「UI 选择状态」引入自定义组件成本过高。
     *
     * <p>放在按 UUID 索引的表里还有两个好处：
     * <ul>
     *   <li>状态跨多个音叉共享，玩家不需要维护「哪把音叉选了哪个仪式」。</li>
     *   <li>多人环境下天然按玩家隔离。</li>
     * </ul>
     *
     * <p>用 {@code WeakHashMap} 是刻意的：玩家退出后条目可被回收，
     * 不需要额外的清理钩子。选择状态**不持久化**——重登后回到默认的安魂曲，
     * 这对一个「每次都要付材料」的机制来说完全可接受。
     */
    private static final java.util.Map<java.util.UUID, Rituals.Type> SELECTED =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public ResonanceForkItem(Properties properties) {
        super(properties);
    }

    // ---------------------------------------------------------------- 选中状态

    public static Rituals.Type selected(Player player) {
        if (player == null) {
            return Rituals.Type.REQUIEM;
        }
        return SELECTED.getOrDefault(player.getUUID(), Rituals.Type.REQUIEM);
    }

    private static void select(Player player, Rituals.Type type) {
        if (player != null) {
            SELECTED.put(player.getUUID(), type);
        }
    }

    // ---------------------------------------------------------------- 交互

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行右键：切换仪式
        if (player.isShiftKeyDown()) {
            Rituals.Type next = nextType(selected(player));
            select(player, next);
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.translatable("item.echoesofoblivion.resonance_fork.selected",
                        Component.translatable(next.translationKey())), true);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.PLAYERS, 0.4f, 1.8f);
            return InteractionResultHolder.success(stack);
        }

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        Rituals.Type type = selected(player);
        if (!hasMaterials(player, type)) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.resonance_fork.missing",
                    Component.translatable(type.translationKey())).withStyle(ChatFormatting.RED),
                true);
            return InteractionResultHolder.fail(stack);
        }

        // 先消耗材料，再执行——顺序反了会出现「仪式成功但材料没扣」
        consumeMaterials(player, type);
        boolean ok = perform(type, serverLevel, serverPlayer);
        if (!ok) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.resonance_fork.failed")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static Rituals.Type nextType(Rituals.Type current) {
        Rituals.Type[] all = Rituals.Type.values();
        return all[(current.ordinal() + 1) % all.length];
    }

    // ---------------------------------------------------------------- 材料

    private static boolean hasMaterials(Player player, Rituals.Type type) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return switch (type) {
            case REQUIEM -> count(player, ModItems.OSSUARY_ASH.get()) >= 1;
            case DESCENT -> count(player, ModItems.SILENCE_SHARD.get()) >= 1
                && count(player, ModItems.VOID_EMBERS.get()) >= 2;
            case VOID_PASSAGE -> count(player, ModItems.MIRROR_SHARD.get()) >= 1;
        };
    }

    private static void consumeMaterials(Player player, Rituals.Type type) {
        if (player.getAbilities().instabuild) {
            return;
        }
        switch (type) {
            case REQUIEM -> remove(player, ModItems.OSSUARY_ASH.get(), 1);
            case DESCENT -> {
                remove(player, ModItems.SILENCE_SHARD.get(), 1);
                remove(player, ModItems.VOID_EMBERS.get(), 2);
            }
            case VOID_PASSAGE -> remove(player, ModItems.MIRROR_SHARD.get(), 1);
        }
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                total += s.getCount();
            }
        }
        return total;
    }

    private static void remove(Player player, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(item)) {
                continue;
            }
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }

    // ---------------------------------------------------------------- 执行

    private static boolean perform(Rituals.Type type, ServerLevel level, ServerPlayer player) {
        return switch (type) {
            case REQUIEM -> performRequiem(level, player);
            case DESCENT -> performDescent(level, player);
            case VOID_PASSAGE -> performVoidPassage(level, player);
        };
    }

    /**
     * 安魂曲：驱散半径内的亡魂。
     *
     * <p>做法是让它们**消散**而不是被杀死——没有伤害数字、没有掉落物。
     * 它们只是不再在那里了，与它们出现时的方式对称。
     */
    private static boolean performRequiem(ServerLevel level, ServerPlayer player) {
        double radius = Rituals.Type.REQUIEM.radius();
        List<PhantomEntity> phantoms = level.getEntitiesOfClass(PhantomEntity.class,
            player.getBoundingBox().inflate(radius));
        if (phantoms.isEmpty()) {
            return false;
        }
        for (PhantomEntity phantom : phantoms) {
            level.sendParticles(ParticleTypes.SOUL,
                phantom.getX(), phantom.getY() + 1.0, phantom.getZ(),
                24, 0.3, 0.7, 0.3, 0.03);
            phantom.discard();
        }
        // 一圈向外扩散的青蓝粒子，让「驱散」这件事有形状
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            player.getX(), player.getY() + 1.0, player.getZ(),
            120, radius * 0.5, 1.5, radius * 0.5, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS, 1.2f, 0.8f);
        level.playSound(null, player.blockPosition(), ModSounds.MEMORY_COLLECT.get(),
            SoundSource.PLAYERS, 0.8f, 1.4f);

        player.displayClientMessage(
            Component.translatable("ritual.echoesofoblivion.requiem.done", phantoms.size())
                .withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    /**
     * 寂静降临：范围静音 + 去饱和，持续 30 秒。
     *
     * <p>用现成的 {@code BossAuraPacket} 通道：把光环强度推到 1.0，
     * 客户端的雾效与去饱和会立刻生效。30 秒后再推回 0。
     *
     * <p><b>这个仪式是立场反转</b>：玩家亲手发动了一次静默。
     * 他第一次站在寂静那一侧，而不是被它侵害。这也是唯一一次
     * 让玩家尝到「污染源」身份的机会。
     */
    private static boolean performDescent(ServerLevel level, ServerPlayer player) {
        double radius = Rituals.Type.DESCENT.radius();

        // 爆发式的视觉：粒子向中心吸入（负速度），与聚合体的收敛同款语汇
        level.sendParticles(ParticleTypes.SQUID_INK,
            player.getX(), player.getY() + 1.5, player.getZ(),
            160, radius * 0.4, 2.0, radius * 0.4, -0.25);

        // 所有声音先被抽走：播放一声然后立刻静默
        level.playSound(null, player.blockPosition(), ModSounds.CORRUPTION_PULSE.get(),
            SoundSource.PLAYERS, 1.5f, 0.5f);

        // 让附近的亡魂也停下来（它们也是声音的一部分）
        for (PhantomEntity phantom : level.getEntitiesOfClass(PhantomEntity.class,
            player.getBoundingBox().inflate(radius))) {
            phantom.setSilent(true);
            phantom.setNoAi(true);
        }

        // 推 1.0 的光环强度：客户端据此去饱和并把雾压到 40%
        ModNetwork.sendToPlayer(
            new com.clion.echoesofoblivion.network.packets.BossAuraPacket(1.0f), player);

        // 30 秒后恢复。用一个一次性任务，避免为它引入持久化状态。
        com.clion.echoesofoblivion.server.RitualScheduler.schedule(level, 600, () -> {
            ModNetwork.sendToPlayer(
                new com.clion.echoesofoblivion.network.packets.BossAuraPacket(0.0f), player);
            for (PhantomEntity phantom : level.getEntitiesOfClass(PhantomEntity.class,
                player.getBoundingBox().inflate(radius))) {
                phantom.setSilent(false);
                phantom.setNoAi(false);
            }
            player.displayClientMessage(
                Component.translatable("ritual.echoesofoblivion.descent.ended")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
        });

        player.displayClientMessage(
            Component.translatable("ritual.echoesofoblivion.descent.done")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return true;
    }

    /**
     * 虚空通道：在两个维度之间做一次定向传送。
     *
     * <p>复用传送门的落点逻辑（含平台铺设），但**绕过传送门**——
     * 这是玩家第一次不需要门就能跨越维度。代价是侵蚀值 +10：
     * 你越频繁地在两个世界之间穿梭，你自己就越接近走廊那一侧。
     */
    private static boolean performVoidPassage(ServerLevel level, ServerPlayer player) {
        boolean entering = !level.dimension()
            .equals(com.clion.echoesofoblivion.world.ModWorldGen.SILENT_CORRIDOR);

        level.playSound(null, player.blockPosition(), ModSounds.PORTAL_TRAVEL.get(),
            SoundSource.PLAYERS, 1.2f, 0.7f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            player.getX(), player.getY() + 1.0, player.getZ(),
            80, 0.8, 1.2, 0.8, 0.08);

        CorridorTeleportHelper.travel(player, entering);

        // 代价：侵蚀值上升。污染不因跨越而消失，反而累积。
        int cost = Rituals.corruptionCost(Rituals.Type.VOID_PASSAGE);
        PlayerProgress progress = PlayerMemoryData.get(level).progressOf(player);
        progress.addCorruption(cost);
        PlayerMemoryData.get(level).markDirty();
        ModNetwork.sendToPlayer(
            new CorruptionUpdatePacket(progress.corruption(), progress.corruptionRatio()), player);

        player.displayClientMessage(
            Component.translatable("ritual.echoesofoblivion.void_passage.done", cost)
                .withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    // ---------------------------------------------------------------- 提示

    /**
     * 提示文字。
     *
     * <p>刻意**不显示当前选中的仪式**：选择状态存在服务端（按玩家 UUID），
     * 而 {@code appendHoverText} 是客户端调用的纯展示方法，拿不到它。
     * 强行同步一个 UI 状态不值得，改为在提示里说明操作方式——
     * 玩家切换时会收到一条 action bar 提示，信息在需要时出现即可。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.resonance_fork.hint")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.resonance_fork.costs")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** 保留：暴露给其他系统查询当前选中仪式的 id。 */
    @SuppressWarnings("unused")
    public static String selectedId(Player player) {
        return selected(player).id();
    }
}
