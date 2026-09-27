package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryProgression;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.MemoryVisionPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 服务端包处理（阶段 1 / 3）。
 *
 * <p>放在 {@code server} 包内是为了明确「此类只在服务端执行」，绝不引用任何客户端类型。
 */
public final class ServerPacketHandler {

    /** 允许交互的最大距离平方（6 格），防止远距离伪造包。 */
    private static final double MAX_INTERACTION_DISTANCE_SQ = 36.0;

    /** 同一水晶的重复目睹冷却（tick）。 */
    private static final int REUSE_COOLDOWN_TICKS = 40;

    private ServerPacketHandler() {
    }

    /**
     * 触摸记忆水晶：
     * <ol>
     *   <li>校验方块与距离</li>
     *   <li>由坐标<b>确定性</b>推导本枚水晶提供的记忆（客户端无法指定）</li>
     *   <li>若该记忆已被目睹，沿环形向后找一条尚未目睹的，保证玩家总能获得新内容</li>
     *   <li>记录进度与线索，回送幻境包</li>
     * </ol>
     */
    public static void onCrystalUse(BlockPos pos, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 1. 方块校验
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.MEMORY_CRYSTAL.get())) {
            return;
        }

        // 2. 距离校验
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
            > MAX_INTERACTION_DISTANCE_SQ) {
            return;
        }

        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(player);

        int base = crystalMemoryIndex(pos);
        int chosen = -1;
        MemoryEntry entry = null;
        for (int offset = 0; offset < MemoryRegistry.size(); offset++) {
            int candidate = (base + offset) % MemoryRegistry.size();
            if (!progress.has(candidate)) {
                chosen = candidate;
                entry = MemoryRegistry.byIndex(candidate);
                break;
            }
        }

        if (entry == null) {
            // 所有记忆都已目睹完毕：给一次「回响」，不改变进度
            int echo = Math.floorMod(base, MemoryRegistry.size());
            ModNetwork.sendToPlayer(new MemoryVisionPacket(echo, false), player);
            level.playSound(null, pos, ModSounds.MEMORY_COLLECT.get(), SoundSource.BLOCKS, 0.4f, 1.4f);
            return;
        }

        int previousCount = progress.collectedCount();
        boolean firstTime = progress.collect(chosen);
        progress.collectClue(entry.clue());
        // 收集记忆会加速侵蚀（设计：知识本身就是污染）
        progress.addCorruption(6);
        data.markDirty();

        ModNetwork.sendToPlayer(new MemoryVisionPacket(chosen, firstTime), player);
        level.playSound(null, pos, ModSounds.MEMORY_COLLECT.get(), SoundSource.BLOCKS, 0.8f, 1.0f);

        // 阶段 3 世界联动：跨过进度阈值时触发叙事与声音，并同步完整进度
        MemoryProgression.onMemoryCollected(player, level, previousCount, progress.collectedCount());
        syncProgress(player, progress);
    }

    /** 把完整进度（位掩码 + 线索 + 侵蚀）推给客户端。 */
    public static void syncProgress(ServerPlayer player, PlayerProgress progress) {
        int mask = 0;
        for (int index : progress.collected()) {
            if (index >= 0 && index < 32) {
                mask |= 1 << index;
            }
        }
        ModNetwork.sendToPlayer(
            new com.clion.echoesofoblivion.network.packets.MemorySyncPacket(
                mask, String.join(",", progress.clues())),
            player);
        ModNetwork.sendToPlayer(
            new com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket(
                progress.corruption(), progress.corruptionRatio()),
            player);
    }

    /**
     * 记忆图鉴的进度刷新请求（v1.1.0）。
     *
     * <p>无校验可言——它只要把该玩家目前的状态原样回送即可。
     * 用完整的 {@code MemorySyncPacket} 而不是增量，是因为位掩码本身就是完整状态，
     * 因此并发、乱序、重复请求都无害。
     */
    public static void onCodexRequest(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerMemoryData data = PlayerMemoryData.get(player.serverLevel());
        syncProgress(player, data.progressOf(player));
    }

    /**
     * 由方块坐标确定性推导记忆索引。
     *
     * <p>使用固定的混合函数而非 {@code Random}：同一个坐标在任何时候、任何客户端
     * 都会得到同一条记忆，因此玩家可以在地图上「记住哪块水晶讲了什么」。
     */
    public static int crystalMemoryIndex(BlockPos pos) {
        int hash = pos.getX() * 73856093 ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
        hash ^= hash >>> 13;
        hash *= 0x5bd1e995;
        hash ^= hash >>> 15;
        return Math.floorMod(hash, MemoryRegistry.size());
    }
}
