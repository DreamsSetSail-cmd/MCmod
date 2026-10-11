package com.clion.echoesofoblivion.memory;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    /**
     * 见证顺序（v2.2.0）。
     *
     * <p>{@code collected} 是 {@code HashSet}，**不保留顺序**。但 v2.2.0 的核心机制是
     * 「按错误顺序见证记忆会浮现矛盾的碎片」——这需要知道顺序，
     * 而顺序一旦丢失就无法重建。
     *
     * <p>因此单独维护一个按见证先后排列的列表。它与 {@code collected} 冗余，
     * 但两者回答不同的问题：{@code collected} 回答「知不知道」，
     * 这个列表回答「什么时候知道的」。顺序表只用于叙事判定
     * （见 {@link #witnessOrder()}），任何逻辑判定仍然以 {@code collected} 为准。
     */
    private final List<Integer> order = new ArrayList<>();

    private final Set<String> clues = new HashSet<>();
    private int corruption;

    /**
     * 距离上一次「它叫你的名字」过去了多少秒（v1.2.0）。
     *
     * <p>放在存档里而不是内存里，是为了让这个计数在重登后仍然有效——
     * 否则玩家可以通过反复重连来刷出那句话，把它变成一个可触发的彩蛋，
     * 而它必须始终像是**偶然发生的**。
     */
    private int secondsSinceNameCall;

    public boolean has(int memoryIndex) {
        return collected.contains(memoryIndex);
    }

    /**
     * 遗忘一段记忆（v2.0.0，供记忆焚烧使用）。
     *
     * <p><b>这是不可逆的。</b> 被遗忘的记忆会从进度里移除，图鉴上重新变回划痕，
     * 而水晶不会再提供它（当全部记忆都已见证时，水晶只给「回响」）。
     * 设计意图见 {@code MemoryBurn}：玩家为了活下去，正在把知道的东西消耗掉。
     *
     * <p>注意**不移除线索**：线索是「你已经理解了这件事」的痕迹，
     * 记住一个结论与记得它的来源是两回事。这也让焚烧留下一点余温——
     * 你忘了内容，但还记得它教过你什么。
     *
     * @return 是否真的遗忘了（原本就没见证过时返回 false）
     */
    public boolean forget(int memoryIndex) {
        // 顺序表也要清掉：焚烧之后这段记忆不再「曾经被知道」，
        // 留着它会让后续的逆序判定读到一段已经被消耗掉的记忆。
        order.remove(Integer.valueOf(memoryIndex));
        return collected.remove(memoryIndex);
    }

    public Set<Integer> collected() {
        return Set.copyOf(collected);
    }

    public int collectedCount() {
        return collected.size();
    }

    /**
     * 见证顺序（v2.2.0）：按玩家**第一次**看到每段记忆的先后排列。
     *
     * <p>返回的是不可变副本。只用于叙事判定——任何「知不知道」的问题
     * 都应该问 {@link #has(int)}。
     */
    public List<Integer> witnessOrder() {
        return List.copyOf(order);
    }

    /**
     * 一段记忆是否在另一段**之前**被见证。
     *
     * <p>这是 v2.2.0「矛盾碎片」的唯一判定入口。为了让它只依赖一个概念，
     * 这里把三种边界情况都收在一个方法里：
     *
     * <ul>
     *   <li>两段都见证过 → 比较它们在顺序表里的位置。</li>
     *   <li>两段见证过但顺序表里缺失（例如老存档升级而来）→ 返回 false。
     *       **不确定时不做判定**，因为错误的「你搞错了顺序」比缺失这句提示更糟。</li>
     *   <li>任一段没见证过 → 返回 false。</li>
     * </ul>
     *
     * @return true 表示 {@code earlier} 确实先于 {@code later} 被见证
     */
    public boolean witnessedBefore(int earlier, int later) {
        if (!collected.contains(earlier) || !collected.contains(later)) {
            return false;
        }
        int firstIndex = order.indexOf(earlier);
        int secondIndex = order.indexOf(later);
        if (firstIndex < 0 || secondIndex < 0) {
            return false;
        }
        return firstIndex < secondIndex;
    }

    /**
     * @return 若这次收集是新记忆则返回 true（用于决定是否播放音效、是否推进叙事）
     */
    public boolean collect(int memoryIndex) {
        boolean isNew = collected.add(memoryIndex);
        if (isNew) {
            order.add(memoryIndex);
        }
        return isNew;
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

    // ---------------------------------------------------------------- 「它叫你的名字」

    /** 每秒调用一次。 */
    public void tickNameCallTimer() {
        if (secondsSinceNameCall < Integer.MAX_VALUE) {
            secondsSinceNameCall++;
        }
    }

    public int secondsSinceNameCall() {
        return secondsSinceNameCall;
    }

    public void resetNameCallTimer() {
        this.secondsSinceNameCall = 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("corruption", corruption);
        tag.putInt("name_call_timer", secondsSinceNameCall);
        tag.putIntArray("collected", collected.stream().mapToInt(Integer::intValue).toArray());
        tag.putString("clues", String.join(",", clues));
        // v2.2.0：见证顺序。单独一个键，老存档没有它也不会出错——
        // load() 会把顺序表留空，而 witnessedBefore() 在信息不足时返回 false。
        tag.putIntArray("witness_order", order.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }

    public static PlayerProgress load(CompoundTag tag) {
        PlayerProgress progress = new PlayerProgress();
        progress.corruption = clamp(tag.getInt("corruption"));
        progress.secondsSinceNameCall = Math.max(0, tag.getInt("name_call_timer"));
        for (int index : tag.getIntArray("collected")) {
            progress.collected.add(index);
        }
        // v2.2.0：见证顺序。
        //
        // 老存档（v2.1 及以前）没有这个键，此时顺序表为空。这是**安全**的：
        // witnessedBefore() 在顺序未知时返回 false，也就是「不判定」，
        // 于是老存档不会凭空冒出矛盾碎片。缺失一句提示，好过冒出一句错的。
        //
        // 只接受「已收集」的索引，避免损坏的存档把顺序表撑成任意内容。
        for (int index : tag.getIntArray("witness_order")) {
            if (progress.collected.contains(index) && !progress.order.contains(index)) {
                progress.order.add(index);
            }
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
