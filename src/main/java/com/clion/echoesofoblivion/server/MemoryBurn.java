package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket;
import com.clion.echoesofoblivion.network.packets.MemorySyncPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.RealityData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

/**
 * 记忆焚烧（v2.0.0）——**牺牲一段已见证的记忆，换取一次即时效果**。
 *
 * <h2>设计定位</h2>
 *
 * <p>这是全篇最有代价感的机制，也是最直接地把主题做成玩法的一次：
 *
 * <blockquote>你为了活下去，正在把你知道的东西消耗掉。</blockquote>
 *
 * <p>Boss 战里「每条记忆只能用一次」是**战斗内**的消耗，而焚烧是**永久**的：
 * 被烧掉的记忆会从进度里移除，图鉴上重新变回被划掉的那一行，
 * 而你**再也无法重新见证它**（因为水晶按坐标确定性分配，
 * 但如果五段都见证过，水晶只会给你「回响」）。
 *
 * <p>也就是说：**焚烧是不可逆的自我减损。** 玩家会赢，然后更少地知道自己为什么而战。
 *
 * <h2>三段记忆三种效果</h2>
 *
 * <p>效果按记忆的<strong>主题</strong>设计，不是按数值随便配的：
 *
 * <table>
 *   <tr><th>记忆</th><th>主题</th><th>焚烧效果</th></tr>
 *   <tr><td>0 陨落 / 3 最后的实验</td><td>仪器的光</td>
 *       <td><b>灼烧</b>：驱散半径 24 内全部亡魂，并在原地留下一圈光</td></tr>
 *   <tr><td>1 最初的寂静 / 2 建筑师</td><td>桥与静默介质</td>
 *       <td><b>静默</b>：半径 24 内的亡魂被禁言与定身 30 秒（不驱散，只是停下）</td></tr>
 *   <tr><td>4 警告</td><td>说出来的话</td>
 *       <td><b>警告</b>：清除周围 3x3 区块的感染，并临时把侵蚀值压回 50 以下</td></tr>
 * </table>
 *
 * <p><b>注意最后一项</b>：「警告」的效果是清除感染——这是把她的行为做成了能力。
 * 她当年做的事就是试图告诉别人真相以阻止扩散；焚烧这段记忆，
 * 等于你替她把那句话说了出来，代价是这句话从此不再存在。
 */
public final class MemoryBurn {

    /** 焚烧的通用半径。 */
    private static final double RADIUS = 24.0;

    /** 静默效果的持续时间（tick）。30 秒。 */
    private static final int SILENCE_TICKS = 600;

    private MemoryBurn() {
    }

    /**
     * 焚烧指定索引的记忆。由 {@code MemoryAttackPacket} 的潜行分支调用。
     *
     * @return 是否真的烧掉了（索引无效或未见证时返回 false）
     */
    public static boolean burn(ServerPlayer player, int memoryIndex) {
        if (player == null || memoryIndex < 0 || memoryIndex >= MemoryRegistry.size()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(player);

        if (!progress.has(memoryIndex)) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.burn.not_witnessed")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        MemoryEntry entry = MemoryRegistry.byIndex(memoryIndex);

        // 1. 从进度里移除——这一步是不可逆的
        progress.forget(memoryIndex);
        data.markDirty();

        // 2. 演奏：先吸入再爆发，语汇与聚合体的收敛相反
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            player.getX(), player.getY() + 1.2, player.getZ(),
            100, 1.0, 1.2, 1.0, 0.06);
        level.playSound(null, player.blockPosition(), ModSounds.CORRUPTION_PULSE.get(),
            SoundSource.PLAYERS, 1.4f, 0.6f);
        level.playSound(null, player.blockPosition(), ModSounds.BOSS_HIT.get(),
            SoundSource.PLAYERS, 1.0f, 1.4f);

        // 3. 按主题施加效果
        String effectKey = applyEffect(level, player, memoryIndex);

        // 4. 立刻同步进度，让图鉴上那一行重新变回划痕
        syncProgress(player, progress);

        player.displayClientMessage(
            Component.translatable("message.echoesofoblivion.burn.done",
                    entry.title(), Component.translatable(effectKey))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return true;
    }

    /** 按记忆索引施加对应效果，返回效果名（语言键后缀）。 */
    private static String applyEffect(ServerLevel level, ServerPlayer player, int memoryIndex) {
        return switch (memoryIndex) {
            // 陨落 / 最后的实验 → 仪器的光：灼烧，直接驱散
            case 0, 3 -> {
                int cleared = burnNearbyPhantoms(level, player, true);
                yield cleared > 0 ? "message.echoesofoblivion.burn.effect.purge"
                                  : "message.echoesofoblivion.burn.effect.purge_none";
            }
            // 最初的寂静 / 建筑师 → 桥与静默：定身但不驱散
            case 1, 2 -> {
                int frozen = burnNearbyPhantoms(level, player, false);
                yield frozen > 0 ? "message.echoesofoblivion.burn.effect.silence"
                                 : "message.echoesofoblivion.burn.effect.silence_none";
            }
            // 警告 → 说出来的话：清除感染，并压住自己的侵蚀
            case 4 -> {
                int chunks = purifyAround(level, player);
                int before = PlayerMemoryData.get(level).progressOf(player).corruption();
                if (before > 50) {
                    PlayerMemoryData.get(level).progressOf(player).setCorruption(50);
                    PlayerMemoryData.get(level).markDirty();
                    ModNetwork.sendToPlayer(new CorruptionUpdatePacket(50, 0.5f), player);
                }
                yield chunks > 0 ? "message.echoesofoblivion.burn.effect.warning"
                                 : "message.echoesofoblivion.burn.effect.warning_none";
            }
            default -> "message.echoesofoblivion.burn.effect.purge_none";
        };
    }

    /**
     * 处理半径内的亡魂。
     *
     * @param destroy true = 直接消散（灼烧）；false = 禁言定身一段时间（静默）
     * @return 受影响的亡魂数量
     */
    private static int burnNearbyPhantoms(ServerLevel level, ServerPlayer player, boolean destroy) {
        List<PhantomEntity> phantoms = level.getEntitiesOfClass(PhantomEntity.class,
            player.getBoundingBox().inflate(RADIUS));
        if (phantoms.isEmpty()) {
            return 0;
        }
        for (PhantomEntity phantom : phantoms) {
            level.sendParticles(ParticleTypes.SOUL,
                phantom.getX(), phantom.getY() + 1.0, phantom.getZ(),
                20, 0.3, 0.6, 0.3, 0.02);
            if (destroy) {
                phantom.discard();
            } else {
                phantom.setSilent(true);
                phantom.setNoAi(true);
            }
        }
        if (!destroy) {
            // 静默会结束——但那之后它们还是在那里
            RitualScheduler.schedule(level, SILENCE_TICKS, () ->
                phantoms.stream()
                    .filter(p -> !p.isRemoved())
                    .forEach(p -> {
                        p.setSilent(false);
                        p.setNoAi(false);
                    }));
        }
        // 一圈向外的光，让效果有形状
        level.sendParticles(ParticleTypes.END_ROD,
            player.getX(), player.getY() + 1.0, player.getZ(),
            60, RADIUS * 0.4, 1.5, RADIUS * 0.4, 0.04);
        return phantoms.size();
    }

    /** 清除玩家周围 3x3 区块的感染（「警告」的效果）。 */
    private static int purifyAround(ServerLevel level, ServerPlayer player) {
        RealityData reality = RealityData.get(level);
        ChunkPos center = new ChunkPos(player.blockPosition());

        List<Long> removed = new java.util.ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                if (reality.isInfected(cx, cz)) {
                    removed.add(ChunkPos.asLong(cx, cz));
                }
            }
        }
        if (removed.isEmpty()) {
            return 0;
        }
        int cleared = reality.removeChunks(removed);
        reality.syncFull(player);

        for (long packed : removed) {
            BlockPos p = new BlockPos(ChunkPos.getX(packed) << 4, player.blockPosition().getY(),
                ChunkPos.getZ(packed) << 4);
            level.sendParticles(ParticleTypes.END_ROD,
                p.getX() + 8, p.getY() + 1.0, p.getZ() + 8, 24, 7.0, 2.0, 7.0, 0.02);
        }
        return cleared;
    }

    /** 复用 ServerPacketHandler 的位掩码拼装，避免两处实现漂移。 */
    private static void syncProgress(ServerPlayer player, PlayerProgress progress) {
        ServerPacketHandler.syncProgress(player, progress);
    }
}
