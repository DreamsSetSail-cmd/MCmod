package com.clion.echoesofoblivion.world;

import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.RealityShiftPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 镜像区块（Mirror Chunks）感染状态的权威数据源（阶段 5）。
 *
 * <p>替代旧的「在玩家周围随机撒实体」的临时逻辑。要点：
 * <ul>
 *   <li>状态持久化在 {@code SavedData} 里，重启后感染区不会消失。</li>
 *   <li>扩散只在服务端推进，客户端通过 {@link RealityShiftPacket} 接收增量。</li>
 *   <li>感染总数有上限，避免存档无限膨胀。</li>
 * </ul>
 */
public class RealityData extends SavedData {

    private static final String DATA_NAME = "echoes_reality";

    /** 单次扩散的半径（区块）。 */
    public static final int SPREAD_RADIUS = 3;

    /** 最大感染区块数，防止存档无上限增长。 */
    public static final int MAX_INFECTED = 4096;

    private final Set<Long> infected = new HashSet<>();
    private int phase;

    public RealityData() {
    }

    public static RealityData load(CompoundTag tag, HolderLookup.Provider registries) {
        RealityData data = new RealityData();
        for (long chunk : tag.getLongArray("infected")) {
            data.infected.add(chunk);
        }
        data.phase = tag.getInt("phase");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("infected", infected.stream().mapToLong(Long::longValue).toArray());
        tag.putInt("phase", phase);
        return tag;
    }

    /** 取主世界的实例——镜像区块是主世界现象，与走廊无关。 */
    public static RealityData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new Factory<>(RealityData::new, RealityData::load, DataFixTypes.SAVED_DATA_MAP_DATA),
            DATA_NAME
        );
    }

    public int size() {
        return infected.size();
    }

    public int phase() {
        return phase;
    }

    public boolean isInfected(int chunkX, int chunkZ) {
        return infected.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public boolean isInfected(double x, double z) {
        return isInfected((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    /** 全部感染区块的 long 编码，用于首次同步。 */
    public List<Long> snapshot() {
        return new ArrayList<>(infected);
    }

    /**
     * 以玩家为中心扩散一轮。
     *
     * <p>密度随距离衰减：中心必感染，边缘概率低，形成不规则的扩散形状而不是
     * 整齐的正方形。
     *
     * @return 本轮新增的区块（可能为空）
     */
    public List<Long> spreadFrom(ServerPlayer player) {
        if (infected.size() >= MAX_INFECTED) {
            return List.of();
        }
        int centerX = player.blockPosition().getX() >> 4;
        int centerZ = player.blockPosition().getZ() >> 4;

        List<Long> added = new ArrayList<>();
        outer:
        for (int dx = -SPREAD_RADIUS; dx <= SPREAD_RADIUS; dx++) {
            for (int dz = -SPREAD_RADIUS; dz <= SPREAD_RADIUS; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > SPREAD_RADIUS) {
                    continue;
                }
                double chance = 1.0 - distance / (SPREAD_RADIUS + 1.0);
                if (player.getRandom().nextDouble() > chance) {
                    continue;
                }
                if (infected.add(ChunkPos.asLong(centerX + dx, centerZ + dz))) {
                    added.add(ChunkPos.asLong(centerX + dx, centerZ + dz));
                    if (infected.size() >= MAX_INFECTED) {
                        break outer;
                    }
                }
            }
        }

        if (!added.isEmpty()) {
            // 感染范围越大，阶段越高（供表现层使用）
            phase = Math.min(3, infected.size() / 128);
            setDirty();
        }
        return added;
    }

    /** 手动感染一个区块（供调试与仪式召唤使用）。 */
    public void infect(int chunkX, int chunkZ) {
        if (infected.add(ChunkPos.asLong(chunkX, chunkZ))) {
            setDirty();
        }
    }

    public void clearAll() {
        infected.clear();
        phase = 0;
        setDirty();
    }

    /** 全量同步给一名玩家（登录 / 维度切换时）。 */
    public void syncFull(ServerPlayer player) {
        ModNetwork.sendToPlayer(new RealityShiftPacket(snapshot(), false, phase), player);
    }

    /** 把增量广播给主世界里的所有玩家。 */
    public void broadcastIncrement(ServerLevel level, List<Long> added) {
        if (added.isEmpty() || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        RealityShiftPacket packet = new RealityShiftPacket(added, true, phase);
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(packet, player);
        }
    }
}
