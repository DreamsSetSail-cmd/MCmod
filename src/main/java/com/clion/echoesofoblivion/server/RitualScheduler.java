package com.clion.echoesofoblivion.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 延迟任务调度器（v2.0.0）。
 *
 * <p>为仪式提供「N tick 后执行一次」的能力，例如「寂静降临」需要在 30 秒后恢复静音。
 *
 * <h2>为什么不用别的方式</h2>
 *
 * <ul>
 *   <li><b>不存进存档</b>：这些效果都是短时的环境状态，服务器重启后本来就该重置。
 *       持久化反而会在重启后留下一个卡住的静音状态。</li>
 *   <li><b>不用 Scheduler API</b>：Forge 没有内置的延迟任务队列，而自己写一个
 *       基于 tick 的小调度器比引入依赖更可控。</li>
 *   <li><b>不用线程</b>：所有效果都会改动世界状态，必须在服务器主线程上跑。
 *       挂在 {@code ServerTickEvent.Post} 上天然满足这个约束。</li>
 * </ul>
 *
 * <p>任务与 {@link ServerLevel} 绑定，因此不同维度的同 tick 任务互不干扰。
 * 玩家在效果生效期间退出、维度卸载时，任务会在下一 tick 检测到失效并自行丢弃。
 */
public final class RitualScheduler {

    /** 一个待执行的延迟任务。 */
    private record Pending(ServerLevel level, long executeAtTick, Runnable action) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private RitualScheduler() {
    }

    /**
     * 调度一个延迟任务。
     *
     * @param delayTicks 延迟的 tick 数（20 tick = 1 秒）
     */
    public static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        long due = level.getGameTime() + delayTicks;
        PENDING.add(new Pending(level, due, action));
    }

    /**
     * 每 tick 推进一次队列。
     *
     * <p>使用 {@code ServerTickEvent.Post}：所有维度都 tick 完后统一处理，
     * 避免在某个维度的 tick 中途改动它的状态。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();

            // 维度已卸载：丢弃任务，避免引用一个失效的 ServerLevel
            if (pending.level().getServer() == null || pending.level().isClientSide()) {
                it.remove();
                continue;
            }
            if (pending.level().getGameTime() < pending.executeAtTick()) {
                continue;
            }
            it.remove();
            try {
                pending.action().run();
            } catch (RuntimeException ex) {
                // 单个任务失败不应该影响其他任务或服务器 tick
                com.clion.echoesofoblivion.EchoesOfOblivionMod.LOGGER
                    .warn("仪式延迟任务执行失败", ex);
            }
        }
    }

    /** 服务器停止时清空队列，避免下次启动时执行上一局的遗留任务。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    /** 供调试与测试查询当前排队任务数。 */
    public static int pendingCount() {
        return PENDING.size();
    }
}
