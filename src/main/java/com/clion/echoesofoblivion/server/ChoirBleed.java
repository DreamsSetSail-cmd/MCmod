package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合唱渗出（v2.4.0）——**多人时，你会听到别人学到的东西**。
 *
 * <h2>backlog 说多人差异化「待评估」</h2>
 *
 * <p>{@code docs/ideas-backlog.md} 把这件事留作远期想法，并且提了一个方向：
 * 「你以为是同伴，其实他们也在被替换」。这一版的评估结论是：
 *
 * <blockquote>差异化，但不是让每个玩家特别，而是让所有玩家共同不可靠。</blockquote>
 *
 * <p>所以这里**不做**「每人一份独特内容」。那样每个玩家都会觉得自己是主角，
 * 与「感染者是所有人」正好相反。
 *
 * <p>这一版做的是：**当附近有另一个玩家时，极低概率你会在聊天栏看到
 * 一段你从未见证过的记忆的标题**，而它来自那个玩家已经知道的东西。
 *
 * <h2>为什么只给标题，不给正文</h2>
 *
 * <p>正文是玩家的进度，不能共享——那是整个模组的进度模型。但**标题**足够表达
 * 这件事：「有一段你知道而我不知道的东西，刚刚漏过来了。」
 *
 * <p>玩家会开始怀疑：**我知道的，是不是也是从别人那里漏过来的？**
 * 这个问题没有答案，而它正是这一版要留下的东西。
 *
 * <h2>为什么只在多人时生效</h2>
 *
 * <p>单人时这个机制没有对象，硬要触发就只能伪造一个来源——那就变成了
 * 一个普通的随机提示。所以它诚实地要求**至少两名玩家在附近**。
 *
 * <p>这是一条自我约束：**不为「让单人玩家也能看到」而伪造内容。**
 * 单人有单人该有的东西（前几版都是），合唱是多人独有的。
 *
 * <h2>只在「他已经知道、你还没有」时发生</h2>
 *
 * <p>这是机制的核心条件。如果两个人进度相同，什么都不会发生——
 * 因此它读起来不是随机噪声，而是**进度差异本身在渗漏**。
 */
public final class ChoirBleed {

    /** 附近有另一名玩家的判定半径（格）。 */
    private static final double NEARBY_RADIUS = 32.0;

    /** 判定间隔（tick）。每秒一次。 */
    private static final int CHECK_INTERVAL = 20;

    /** 每次判定的触发概率。很低——它必须像偶然。 */
    private static final float CHANCE = 1.0f / 90.0f;

    /** 两次渗出之间的最短间隔（tick）。3 分钟。 */
    private static final int MIN_GAP = 3600;

    /** 每玩家的冷却（游戏刻时间戳）。 */
    private static final Map<UUID, Long> LAST_BLEED = new ConcurrentHashMap<>();

    private ChoirBleed() {
    }

    /**
     * 每秒调用一次（由 {@code NameSteleObservation} 之外的独立 tick 驱动，
     * 见 {@code CorruptionEventHandler}）。
     *
     * <p>之所以挂在既有的每秒侵蚀 tick 上而不是新开一个监听器：
     * 那个 tick 已经在遍历在线玩家，复用它不会增加新的调度点。
     */
    public static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        Long last = LAST_BLEED.get(player.getUUID());
        if (last != null && now - last < MIN_GAP) {
            return;
        }
        if (now % CHECK_INTERVAL != 0) {
            return;
        }

        // 需要附近有另一名玩家。没有就不发生——不伪造来源。
        List<ServerPlayer> others = nearbyOthers(player, level);
        if (others.isEmpty()) {
            return;
        }

        RandomSource random = player.getRandom();
        if (random.nextFloat() >= CHANCE) {
            return;
        }

        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress mine = data.progressOf(player);

        // 收集「别人知道而我不知道」的记忆索引
        List<Integer> unknown = new ArrayList<>();
        for (ServerPlayer other : others) {
            PlayerProgress theirs = data.progressOf(other);
            for (int index : theirs.collected()) {
                if (!mine.has(index) && !unknown.contains(index)) {
                    unknown.add(index);
                }
            }
        }
        if (unknown.isEmpty()) {
            // 进度相同：什么都不会发生。这是核心条件，不是边界情况。
            return;
        }

        int chosen = unknown.get(random.nextInt(unknown.size()));
        MemoryEntry entry = MemoryRegistry.byIndex(chosen);
        if (entry == null) {
            return;
        }

        LAST_BLEED.put(player.getUUID(), now);
        player.displayClientMessage(
            Component.translatable("message.echoesofoblivion.choir.bleed", entry.title())
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    /** 半径内的其他玩家（不含自己）。 */
    private static List<ServerPlayer> nearbyOthers(ServerPlayer player, ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer other : level.players()) {
            if (other == player) {
                continue;
            }
            if (other.distanceToSqr(player) <= NEARBY_RADIUS * NEARBY_RADIUS) {
                result.add(other);
            }
        }
        return result;
    }

    /** 退出时清理冷却表。 */
    public static void forget(UUID playerId) {
        LAST_BLEED.remove(playerId);
    }

    /** 供测试：机制要求的判定半径与最短间隔都是正数且合理。 */
    public static boolean parametersAreSane() {
        return NEARBY_RADIUS > 0 && MIN_GAP > 0 && CHANCE > 0 && CHANCE < 0.5f;
    }
}
