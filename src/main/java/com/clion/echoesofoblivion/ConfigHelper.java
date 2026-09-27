package com.clion.echoesofoblivion;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置读取的安全包装（修复「Cannot get config value before config is loaded」）。
 *
 * <p><b>问题</b>：Forge 的 {@code ConfigValue.get()} 在配置尚未加载时抛
 * {@code IllegalStateException}。配置是在服务器加载配置阶段才就绪的，而以下场景更早：
 * <ul>
 *   <li>{@code runGameTestServer} —— 测试世界 tick 时配置还没就绪</li>
 *   <li>部分事件早于 CONFIG_LOAD 触发</li>
 * </ul>
 * 之前 {@code CorruptionEventHandler} / {@code MirrorChunkHandler} 直接在 tick 里读配置，
 * 导致 gameTest 跑测试时整个世界 tick 崩溃（堆栈为
 * {@code Level.guardEntityTick -> ... -> ForgeConfigSpec$ConfigValue.get}）。
 *
 * <p><b>为什么不只用 isLoaded() 判断</b>：实测在 gameTest 环境下仅凭
 * {@code COMMON_SPEC.isLoaded()} 不足以拦住该异常（JIT 内联后堆栈只剩
 * {@code ConfigValue.get} 一帧，说明判断与取值之间存在竞态）。因此这里采取
 * 「先查状态、再兜异常」的双保险，确保任何加载时序下配置读取都不会把异常抛进游戏循环。
 *
 * <p>回退值一律显式写成与注册时相同的默认值，避免两处默认值漂移。
 */
public final class ConfigHelper {

    private ConfigHelper() {
    }

    public static int getInt(ForgeConfigSpec.IntValue value, int fallback) {
        try {
            if (!Config.COMMON_SPEC.isLoaded()) {
                return fallback;
            }
            return value.get();
        } catch (RuntimeException notReady) {
            return fallback;
        }
    }

    public static double getDouble(ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            if (!Config.CLIENT_SPEC.isLoaded()) {
                return fallback;
            }
            return value.get();
        } catch (RuntimeException notReady) {
            return fallback;
        }
    }

    public static boolean getBoolean(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            // COMMON 与 CLIENT 的配置项可能来自不同 spec，这里两个都查一次
            if (!Config.COMMON_SPEC.isLoaded() && !Config.CLIENT_SPEC.isLoaded()) {
                return fallback;
            }
            return value.get();
        } catch (RuntimeException notReady) {
            return fallback;
        }
    }
}
