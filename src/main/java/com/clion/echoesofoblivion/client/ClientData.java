package com.clion.echoesofoblivion.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 客户端状态缓存（阶段 3 / 4）。
 *
 * <p>纯数据容器：<b>不包含任何逻辑权威</b>，所有值都来自服务端同步。
 * 旧实现会在客户端 tick 里随机改写影子状态，等于用随机数覆盖服务端的同步结果，
 * 这里彻底移除该行为。
 *
 * <p>本类只被客户端代码引用，因此可以安全地使用 {@code Set} 等普通类型。
 */
public final class ClientData {

    /** 影子状态常量（与服务端约定）。 */
    public static final int SHADOW_IDLE = 0;
    public static final int SHADOW_POINTING = 1;
    public static final int SHADOW_TREMBLING = 2;
    public static final int SHADOW_LAGGING = 3;
    public static final int SHADOW_AHEAD = 4;

    private static int corruptionLevel;
    private static float corruptionRatio;
    private static int shadowState = SHADOW_IDLE;
    private static int collectedMask;
    private static int realityPhase;
    private static int bossPhase = -1;
    private static float bossStability;
    /** 聚合体光环强度 0~1（v1.2.0）。用于抑制环境音，不是伤害。 */
    private static float bossAura;
    private static final Set<Long> infectedChunks = new HashSet<>();
    private static Set<String> clues = Set.of();

    private ClientData() {
    }

    // ---------------------------------------------------------------- 侵蚀值

    public static void setCorruption(int level, float ratio) {
        corruptionLevel = level;
        corruptionRatio = ratio;
    }

    public static int getCorruptionLevel() {
        return corruptionLevel;
    }

    public static float getCorruptionRatio() {
        return corruptionRatio;
    }

    public static void resetCorruption() {
        corruptionLevel = 0;
        corruptionRatio = 0.0f;
    }

    // ---------------------------------------------------------------- 影子

    public static void setShadowState(int state) {
        shadowState = state;
    }

    public static int getShadowState() {
        return shadowState;
    }

    // ---------------------------------------------------------------- 记忆进度

    public static void setCollectedMask(int mask) {
        collectedMask = mask;
    }

    public static int getCollectedMask() {
        return collectedMask;
    }

    /** 位掩码式进度查询，避免每次遍历集合。 */
    public static boolean hasCollected(int memoryIndex) {
        if (memoryIndex < 0 || memoryIndex >= 32) {
            return false;
        }
        return (collectedMask & (1 << memoryIndex)) != 0;
    }

    public static void markCollected(int memoryIndex) {
        if (memoryIndex >= 0 && memoryIndex < 32) {
            collectedMask |= 1 << memoryIndex;
        }
    }

    public static void setClues(Set<String> newClues) {
        clues = Collections.unmodifiableSet(new HashSet<>(newClues));
    }

    public static Set<String> getClues() {
        return clues;
    }

    public static boolean hasClue(String clue) {
        return clues.contains(clue);
    }

    // ---------------------------------------------------------------- 镜像区块

    public static void setInfectedChunks(Set<Long> chunks) {
        infectedChunks.clear();
        infectedChunks.addAll(chunks);
    }

    public static void addInfectedChunks(Set<Long> chunks) {
        infectedChunks.addAll(chunks);
    }

    public static Set<Long> getInfectedChunks() {
        return Collections.unmodifiableSet(infectedChunks);
    }

    public static boolean isChunkInfected(long chunkKey) {
        return infectedChunks.contains(chunkKey);
    }

    public static void setRealityPhase(int phase) {
        realityPhase = phase;
    }

    public static int getRealityPhase() {
        return realityPhase;
    }

    // ---------------------------------------------------------------- Boss（阶段 6）

    public static void setBossPhase(int phase) {
        bossPhase = phase;
    }

    /** -1 表示没有进行中的 Boss 战。 */
    public static int getBossPhase() {
        return bossPhase;
    }

    public static void setBossStability(float ratio) {
        bossStability = ratio;
    }

    public static float getBossStability() {
        return bossStability;
    }

    /** 聚合体光环强度 0~1。0 表示不在光环内（声音正常）。 */
    public static void setBossAura(float intensity) {
        bossAura = intensity;
    }

    public static float getBossAura() {
        return bossAura;
    }

    /** 断线或切换存档时清空，避免把上一个世界的数据带到下一个。 */
    public static void resetAll() {
        resetCorruption();
        shadowState = SHADOW_IDLE;
        collectedMask = 0;
        clues = Set.of();
        infectedChunks.clear();
        realityPhase = 0;
        bossPhase = -1;
        bossStability = 0.0f;
        bossAura = 0.0f;
    }
}
