package com.clion.echoesofoblivion.memory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全存档的玩家进度容器（服务端权威）。
 *
 * <p>旧实现把收集进度直接放在 {@code SavedData} 根部，导致同一存档内所有玩家共享记忆；
 * 这里改为按 UUID 分桶。存储锚点固定为主世界，因为玩家进度与所在维度无关。
 */
public class PlayerMemoryData extends SavedData {

    private static final String DATA_NAME = "echoes_player_progress";

    private final Map<UUID, PlayerProgress> progress = new HashMap<>();

    public PlayerMemoryData() {
    }

    public static PlayerMemoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerMemoryData data = new PlayerMemoryData();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                data.progress.put(uuid, PlayerProgress.load(players.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // 容错：损坏的 UUID 条目跳过，保证存档可升级（技术大纲 §13.6）
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        progress.forEach((uuid, value) -> players.put(uuid.toString(), value.save()));
        tag.put("players", players);
        return tag;
    }

    /** 取主世界作为存储锚点。 */
    public static PlayerMemoryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new Factory<>(PlayerMemoryData::new, PlayerMemoryData::load, DataFixTypes.SAVED_DATA_MAP_DATA),
            DATA_NAME
        );
    }

    public PlayerProgress progressOf(UUID playerId) {
        return progress.computeIfAbsent(playerId, id -> new PlayerProgress());
    }

    public PlayerProgress progressOf(ServerPlayer player) {
        return progressOf(player.getUUID());
    }

    /** 任何修改后都必须调用，否则不会写盘。 */
    public void markDirty() {
        setDirty();
    }
}
