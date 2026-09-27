package com.clion.echoesofoblivion.memory;

import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;

/**
 * 单个玩家的记忆进度与意识侵蚀值。
 *
 * <p>旧实现把收集进度存在全局 {@code SavedData} 上，导致同一存档内所有玩家共享记忆。
 * 这里改为按玩家 UUID 分桶，每名玩家拥有独立进度。
 *
 * <p>侵蚀值是<b>唯一权威来源</b>：同步给客户端后由 Overlay / 影子异常消费，
 * 客户端不再自行推算。
 */
public final class PlayerProgress {

    /** 侵蚀值上限，超过后进入不可逆的「坍缩」表现。 */
    public static final int MAX_CORRUPTION = 100;

    private final Set<Integer> collected = new HashSet<>();
    private final Set<String> clues = new HashSet<>();
    private int corruption;

    public boolean has(int memoryIndex) {
        return collected.contains(memoryIndex);
    }

    public Set<Integer> collected() {
        return Set.copyOf(collected);
    }

    public int collectedCount() {
        return collected.size();
    }

    /**
     * @return 若这次收集是新记忆则返回 true（用于决定是否播放音效、是否推进叙事）
     */
    public boolean collect(int memoryIndex) {
        return collected.add(memoryIndex);
    }

    public void collectClue(String clue) {
        clues.add(clue);
    }

    public boolean hasClue(String clue) {
        return clues.contains(clue);
    }

    public Set<String> clues() {
        return Set.copyOf(clues);
    }

    /**
     * 阶段 3「非线性关联」判定：本条记忆的连接线索是否全部已被收集。
     */
    public boolean isResonant(MemoryEntry entry) {
        if (entry.connectedClues().isEmpty()) {
            return false;
        }
        return entry.connectedClues().stream().allMatch(this::hasClue);
    }

    public int corruption() {
        return corruption;
    }

    public float corruptionRatio() {
        return corruption / (float) MAX_CORRUPTION;
    }

    public void setCorruption(int value) {
        this.corruption = clamp(value);
    }

    public void addCorruption(int delta) {
        this.corruption = clamp(this.corruption + delta);
    }

    public static int clamp(int value) {
        return Math.max(0, Math.min(MAX_CORRUPTION, value));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("corruption", corruption);
        tag.putIntArray("collected", collected.stream().mapToInt(Integer::intValue).toArray());
        tag.putString("clues", String.join(",", clues));
        return tag;
    }

    public static PlayerProgress load(CompoundTag tag) {
        PlayerProgress progress = new PlayerProgress();
        progress.corruption = clamp(tag.getInt("corruption"));
        for (int index : tag.getIntArray("collected")) {
            progress.collected.add(index);
        }
        String rawClues = tag.getString("clues");
        if (!rawClues.isEmpty()) {
            for (String clue : rawClues.split(",")) {
                if (!clue.isBlank()) {
                    progress.clues.add(clue);
                }
            }
        }
        return progress;
    }
}
