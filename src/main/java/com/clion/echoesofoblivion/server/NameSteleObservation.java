package com.clion.echoesofoblivion.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刻名的后果（v2.3.0）——**代价可见，收益为零**。
 *
 * <h2>两件后果</h2>
 *
 * <ol>
 *   <li><b>侵蚀增长加快 25%</b>。这不是惩罚性设计，是位置替换的物理表达：
 *       你把自己放进了那个文明的位置，而那个位置是**正在被消耗**的。</li>
 *   <li><b>偶尔出现关于你的记录式短句</b>。第三人称、档案语气、说一件你做过的事。
 *       它不是在对你说，它是在**记述你**——就像记述他们一样。</li>
 * </ol>
 *
 * <h2>为什么「不可撤回」要写在代码里</h2>
 *
 * <p>砸掉碑不会让你脱身。代价判定查的是**玩家自己的持久化标记**
 * （{@code persistentData["echoes_inscribed"]}），不是世界里还有没有那块碑。
 *
 * <p>这不是偷懒，是这一版的核心陈述：**你写上去的东西不在这里，在你身上。**
 * 如果砸碑能撤销代价，玩家会得到一个「试了再退」的安全阀，
 * 而整个机制就退化成一次可回滚的实验。
 *
 * <h2>为什么用 persistentData 而不是 PlayerProgress</h2>
 *
 * <p>它只需要回答一个是非问题，而且必须跨维度、跨死亡保留。
 * {@code persistentData} 正好满足，且不需要改动 {@code PlayerProgress} 的存档结构
 * （少一次存档兼容性风险）。碑上的方块实体仍然保存完整信息，
 * 用于追溯与调试。
 */
public final class NameSteleObservation {

    /** 玩家持久化标记：是否刻过名。 */
    public static final String INSCRIBED_FLAG = "echoes_inscribed";

    /** 刻名带来的侵蚀增长倍率。 */
    public static final float CORRUPTION_MULTIPLIER = 1.25f;

    /** 观测语句的送达间隔（tick）。约 6 分钟。 */
    private static final int OBSERVE_INTERVAL = 7200;

    /** 每次判定触发观测的概率。1/6 意味着实际约每 36 分钟一句。 */
    private static final float OBSERVE_CHANCE = 1.0f / 6.0f;

    /**
     * 观测语句的候选语言键。
     *
     * <p>全部是**第三人称、档案语气**，说的是玩家做过的事。
     * 刻意不解释、不安抚、不加感叹号——它们必须读起来像有人在记录你，
     * 而记录你的人并不觉得这有什么特别。
     */
    private static final String[] OBSERVATIONS = {
        "message.echoesofoblivion.inscription.observed.1",
        "message.echoesofoblivion.inscription.observed.2",
        "message.echoesofoblivion.inscription.observed.3",
        "message.echoesofoblivion.inscription.observed.4",
        "message.echoesofoblivion.inscription.observed.5",
    };

    /** 每玩家的观测计时。 */
    private static final Map<UUID, Integer> TIMERS = new ConcurrentHashMap<>();

    private NameSteleObservation() {
    }

    /** 玩家是否刻过名。 */
    public static boolean hasInscribed(ServerPlayer player) {
        return player.getPersistentData().getBoolean(INSCRIBED_FLAG);
    }

    /** 记下「此人刻过名」。由 {@code NameSteleBlock} 调用。 */
    public static void markInscribed(ServerPlayer player) {
        player.getPersistentData().putBoolean(INSCRIBED_FLAG, true);
    }

    /**
     * 每秒调用一次（由 {@code CorruptionEventHandler} 驱动）。
     *
     * <p>返回**侵蚀增长应当乘以的倍率**。没刻名时返回 1.0，
     * 因此未刻名的玩家完全不受影响。
     */
    public static float corruptionMultiplier(ServerPlayer player) {
        return hasInscribed(player) ? CORRUPTION_MULTIPLIER : 1.0f;
    }

    /**
     * 推进观测计时，可能送出一句「关于你的记述」。
     *
     * @param secondsElapsed 本次调用推进了多少秒（通常为 1）
     */
    public static void tickObservation(ServerPlayer player, int secondsElapsed) {
        if (!hasInscribed(player)) {
            return;
        }
        int ticks = TIMERS.merge(player.getUUID(), secondsElapsed, Integer::sum);
        if (ticks < OBSERVE_INTERVAL) {
            return;
        }
        TIMERS.put(player.getUUID(), 0);
        if (player.getRandom().nextFloat() >= OBSERVE_CHANCE) {
            return;
        }
        String key = OBSERVATIONS[player.getRandom().nextInt(OBSERVATIONS.length)];
        // 走聊天栏（false）而不是动作栏：这是一条**记录**，不是一条提示。
        player.displayClientMessage(Component.translatable(key), false);
    }

    /** 退出时清理计时表。 */
    public static void forget(UUID playerId) {
        TIMERS.remove(playerId);
    }

    /** 供测试：观测语句的候选数量。 */
    public static int observationCount() {
        return OBSERVATIONS.length;
    }

    /**
     * 供测试：把倍率应用到一份增长量上。
     *
     * <p>纯计算、不触碰世界，因此可以在 gameTest 里直接断言。
     * 让这里成为**唯一的**倍率应用点，是为了避免倍率在几处各写一遍后漂移。
     */
    public static int applyMultiplier(int baseGain, float multiplier) {
        return Math.round(baseGain * multiplier);
    }
}
