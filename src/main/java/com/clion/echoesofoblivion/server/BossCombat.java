package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.RenderStatePacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Boss 战斗判定（阶段 6）。
 *
 * <p>「以记忆为武器」的核心规则集中在这里：
 * <ul>
 *   <li>只有玩家<b>已收集</b>的记忆才能使用，未收集的索引直接拒绝（防作弊）。</li>
 *   <li>每条记忆只能用一次（一次战斗中），避免刷键。</li>
 *   <li>概念伤害随「共鸣」加成——如果这条记忆的连接线索也都收集齐了，伤害翻倍。</li>
 *   <li>稳定性归零触发终结演出与结算奖励。</li>
 * </ul>
 */
public final class BossCombat {

    /** 单条记忆的基础概念伤害。 */
    private static final int BASE_DAMAGE = 12;

    /** 共鸣（连接线索齐备）时的额外倍率。 */
    private static final float RESONANCE_MULTIPLIER = 2.0f;

    /** 允许使用记忆的最大距离（格）。 */
    private static final double MAX_RANGE = 48.0;

    private BossCombat() {
    }

    /** 处理玩家的「记忆攻击」。 */
    public static void onMemoryAttack(ServerPlayer player, int memoryIndex) {
        if (player == null || memoryIndex < 0 || memoryIndex >= MemoryRegistry.size()) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(player);

        // 1. 必须是已收集的记忆
        if (!progress.has(memoryIndex)) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.not_collected"), true);
            return;
        }

        // 2. 范围内必须有 Boss
        Optional<SilentAggregateEntity> bossOpt = findBoss(level, player);
        if (bossOpt.isEmpty()) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.no_target"), true);
            return;
        }
        SilentAggregateEntity boss = bossOpt.get();

        // 3. 战斗内一次性使用
        String usedKey = "echoes_boss_used_" + memoryIndex;
        if (player.getPersistentData().getBoolean(usedKey)) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.already_used"), true);
            return;
        }
        player.getPersistentData().putBoolean(usedKey, true);

        // 4. 概念伤害 + 共鸣加成
        var entry = MemoryRegistry.byIndex(memoryIndex);
        boolean resonant = progress.isResonant(entry);
        int damage = resonant
            ? Math.round(BASE_DAMAGE * RESONANCE_MULTIPLIER)
            : BASE_DAMAGE;

        boolean defeated = boss.reduceStability(damage);

        level.sendParticles(ParticleTypes.FLASH, boss.getX(), boss.getY() + 2.0, boss.getZ(),
            3, 0.5, 0.5, 0.5, 0.0);
        level.playSound(null, boss.blockPosition(), ModSounds.BOSS_HIT.get(),
            SoundSource.HOSTILE, 1.0f, resonant ? 1.4f : 1.0f);

        player.displayClientMessage(Component.translatable(
            resonant ? "message.echoesofoblivion.boss.resonant_hit" : "message.echoesofoblivion.boss.hit",
            damage, boss.getStability()), true);

        // 5. 同步阶段
        ModNetwork.sendToPlayer(RenderStatePacket.bossPhase(boss.getPhase(), boss.stabilityRatio()), player);

        if (defeated) {
            onBossDefeated(level, player, boss);
        }
    }

    /** 恐惧光环的侵蚀增长入口（由 Boss 实体调用）。 */
    public static void addAuraCorruption(Player player, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(serverPlayer);
        progress.addCorruption(amount);
        data.markDirty();

        ModNetwork.sendToPlayer(
            new com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket(
                progress.corruption(), progress.corruptionRatio()),
            serverPlayer);
    }

    /** 终结演出与结算（阶段 6 第 5 条）。 */
    private static void onBossDefeated(ServerLevel level, ServerPlayer player, SilentAggregateEntity boss) {
        // 最后一句台词：它在感谢你把它们带回来。
        // 这是全篇最恐怖的一句——玩家以为自己赢了，而它在感谢污染成功。
        boss.announceDying();

        // 收敛：所有东西同时收回去（v1.2.0 的环境级收尾表现）
        BossAura.collapse(boss, level);

        level.playSound(null, boss.blockPosition(), ModSounds.BOSS_DEATH.get(),
            SoundSource.HOSTILE, 1.5f, 0.8f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            boss.getX(), boss.getY() + 2.0, boss.getZ(), 4, 2.0, 2.0, 2.0, 0.0);
        level.sendParticles(ParticleTypes.SOUL,
            boss.getX(), boss.getY() + 2.0, boss.getZ(), 120, 3.0, 3.0, 3.0, 0.06);

        // 叙事结算
        player.displayClientMessage(Component.translatable("message.echoesofoblivion.boss.defeated"), false);
        // 结局纪念品
        player.getInventory().placeItemBackInInventory(
            new net.minecraft.world.item.ItemStack(
                com.clion.echoesofoblivion.item.ModItems.SHARD_OF_TRUTH.get(), 1));

        ModNetwork.sendToPlayer(RenderStatePacket.bossDefeat(), player);
        // 光环随之消散，否则玩家的雾会被永久压着
        BossAura.clearAura(player);
        boss.discard();
    }

    /** 找到范围内最近的 Boss。 */
    public static Optional<SilentAggregateEntity> findBoss(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MAX_RANGE);
        List<SilentAggregateEntity> bosses = level.getEntitiesOfClass(SilentAggregateEntity.class, area);
        return bosses.stream()
            .filter(entity -> !entity.isRemoved())
            .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    /**
     * 召唤仪式：玩家在感染区块内、已见证全部记忆、手持寂静之眼右键。
     *
     * @return 是否成功召唤
     */
    public static boolean trySummon(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(player);

        if (!com.clion.echoesofoblivion.memory.MemoryProgression.isComplete(progress)) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.need_memories"), true);
            return false;
        }
        if (!com.clion.echoesofoblivion.MirrorChunkHandler.isPlayerInfected(player)) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.need_infected"), true);
            return false;
        }
        if (findBoss(level, player).isPresent()) {
            return false;
        }

        SilentAggregateEntity boss = ModEntities.SILENT_AGGREGATE.get().create(level);
        if (boss == null) {
            return false;
        }
        var target = player.blockPosition().offset(player.getDirection().getNormal().multiply(8));
        boss.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(boss);

        level.playSound(null, target, ModSounds.BOSS_SUMMON.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5,
            150, 2.0, 2.5, 2.0, 0.04);
        player.displayClientMessage(Component.translatable("message.echoesofoblivion.boss.summoned"), false);
        return true;
    }

    /** 玩家离开世界时清理「本场战斗已用过的记忆」标记。 */
    public static void resetUsedMemories(ServerPlayer player) {
        for (int index = 0; index < MemoryRegistry.size(); index++) {
            player.getPersistentData().remove("echoes_boss_used_" + index);
        }
    }
}
