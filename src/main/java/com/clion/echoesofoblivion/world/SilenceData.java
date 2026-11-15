package com.clion.echoesofoblivion.world;

import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.SilenceSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 静默区块（Silence Zones，v2.6.0）——**玩家自己造出的「走廊」**。
 *
 * <h2>它是什么</h2>
 *
 * <p>v2.6.0 让玩家可以重建那台仪器。激活之后，一片区域会**永久**变得没有声音。
 * 这是全篇最重的一次玩家行为：那个文明为了阻止扩散造了这台仪器，
 * 而它的成功恰恰需要清空一切声音。玩家现在做了同一件事。
 *
 * <h2>为什么它真的有用</h2>
 *
 * <p>如果静默区只是个装饰，这个机制就只是氛围。所以它必须**真的阻止扩散**：
 *
 * <ul>
 *   <li>静默区块**不可能被感染**——静默优先于污染。</li>
 *   <li>激活时会**清除**半径内已有的感染（交给 {@link RealityData#removeChunks}）。</li>
 * </ul>
 *
 * <p>这也是设定上的正解：那个文明造仪器就是为了阻止扩散，它确实有效。
 * 恐怖不在于它没用，而在于**有用**——代价是让世界的一部分从此没有声音。
 *
 * <h2>为什么不可撤回</h2>
 *
 * <p>**没有任何物品能消除静默。** 净化剂能清除感染，但静默不是感染，
 * 它是**缺席**。你不能把'没有'清理掉。
 *
 * <p>这条规则是刻意的，也让这个机制成为全篇唯一一次真正不可逆的世界改变
 * （记忆焚烧不可逆，但它只影响一个玩家；静默区影响整个世界）。
 *
 * <h2>为什么要扩散</h2>
 *
 * <p>不扩散的话，它就是一个安全的据点，而模组的整个前提是「一切都在慢慢被消耗」。
 * 它必须**也是被消耗的东西**：静默会缓慢地向四周吃掉世界。
 *
 * <p>上限 {@link #MAX_SILENCED} 存在的原因与镜像区块不同：那里是防止存档膨胀，
 * 这里是防止**玩家用几百块核心把整个世界变成静默区**——
 * 那会让游戏彻底失去张力，而不是让它更阴森。
 */
public class SilenceData extends SavedData {

    private static final String DATA_NAME = "echoes_silence";

    /** 激活时覆盖的半径（区块）。 */
    public static final int ACTIVATION_RADIUS = 3;

    /** 最大静默区块数。 */
    public static final int MAX_SILENCED = 2048;

    /**
     * 每次扩散最多尝试的源区块数。
     *
     * <p>静默区可能有上千个区块；每次全量扫描既慢又没有必要。
     * 随机抽样 {@value} 个源，扩散速度就与区域大小无关。
     */
    private static final int SPREAD_SOURCES = 24;

    /** 每个源区块每次扩散新增邻居的期望数量。 */
    private static final int SPREAD_STEPS = 1;

    private final Set<Long> silenced = new HashSet<>();

    public SilenceData() {
    }

    public static SilenceData load(CompoundTag tag, HolderLookup.Provider registries) {
        SilenceData data = new SilenceData();
        for (long chunk : tag.getLongArray("silenced")) {
            data.silenced.add(chunk);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("silenced", silenced.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    /** 取主世界的实例——仪器只影响主世界（与镜像区块同一取舍）。 */
    public static SilenceData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new Factory<>(SilenceData::new, SilenceData::load, DataFixTypes.SAVED_DATA_MAP_DATA),
            DATA_NAME
        );
    }

    public int size() {
        return silenced.size();
    }

    public boolean isSilenced(int chunkX, int chunkZ) {
        return silenced.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public boolean isSilenced(double x, double z) {
        return isSilenced((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    public List<Long> snapshot() {
        return new ArrayList<>(silenced);
    }

    /**
     * 激活仪器：把一片区域变成静默区，并从感染集合里移除这些区块。
     *
     * @return 本次新增的静默区块（供增量同步与表现使用）
     */
    public List<Long> silenceArea(ServerLevel level, int centerX, int centerZ, int radius) {
        List<Long> added = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // 圆形而不是方形：方形的边界太明显，看起来像区块没有加载好
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.35) {
                    continue;
                }
                long key = ChunkPos.asLong(centerX + dx, centerZ + dz);
                if (silenced.add(key)) {
                    added.add(key);
                }
            }
        }
        if (added.isEmpty()) {
            return added;
        }
        setDirty();

        // 静默优先于污染：把这片区域从感染集合里摘掉。
        //
        // 这是这个机制的**全部意义**——它真的阻止扩散。那个文明造仪器就是为了
        // 阻止扩散，而它有效。恐怖之处在于有效，而不在于无效。
        RealityData reality = RealityData.get(level);
        reality.removeChunks(added);

        return added;
    }

    /**
     * 扩散一轮（由 {@code MirrorChunkHandler} 的等级 tick 驱动，间隔远长于感染扩散）。
     *
     * <p>只从**已有的静默区块**往外长，因此静默区永远不会凭空出现——
     * 它总是从某一台仪器开始。这一点很重要：玩家必须记得是自己按下的。
     *
     * @return 本轮新增的区块
     */
    public List<Long> spread(ServerLevel level, RandomSource random, int maxNew) {
        if (silenced.isEmpty() || silenced.size() >= MAX_SILENCED) {
            return List.of();
        }
        List<Long> all = snapshot();
        List<Long> added = new ArrayList<>();

        for (int source = 0; source < SPREAD_SOURCES && added.size() < maxNew; source++) {
            long origin = all.get(random.nextInt(all.size()));
            int ox = ChunkPos.getX(origin);
            int oz = ChunkPos.getZ(origin);

            for (int step = 0; step < SPREAD_STEPS; step++) {
                int nx = ox + random.nextInt(3) - 1;
                int nz = oz + random.nextInt(3) - 1;
                if (nx == ox && nz == oz) {
                    continue;
                }
                long key = ChunkPos.asLong(nx, nz);
                if (silenced.add(key)) {
                    added.add(key);
                    if (silenced.size() >= MAX_SILENCED) {
                        break;
                    }
                }
            }
        }

        if (!added.isEmpty()) {
            setDirty();
            // 静默区推进时同样清除它吃掉的感染
            RealityData.get(level).removeChunks(added);
        }
        return added;
    }

    /** 全量同步给一名玩家（登录 / 维度切换时）。 */
    public void syncFull(ServerPlayer player) {
        ModNetwork.sendToPlayer(new SilenceSyncPacket(snapshot(), false), player);
    }

    /** 把增量广播给主世界里的所有玩家。 */
    public void broadcastIncrement(ServerLevel level, List<Long> added) {
        if (added.isEmpty() || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        SilenceSyncPacket packet = new SilenceSyncPacket(added, true);
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(packet, player);
        }
    }
}
