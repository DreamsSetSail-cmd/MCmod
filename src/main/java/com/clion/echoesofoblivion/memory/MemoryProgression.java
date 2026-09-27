package com.clion.echoesofoblivion.memory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import com.clion.echoesofoblivion.sound.ModSounds;

/**
 * 记忆进度驱动的世界联动（阶段 3 第 5 条）。
 *
 * <p>收集进度跨过阈值时触发世界级反馈：叙事提示、声音、以及后续阶段要挂接的
 * 世界事件（水晶光芒变化、走廊深处解锁）。阈值集中在这里，避免散落在各处。
 */
public final class MemoryProgression {

    /** 收集 N 条记忆时触发的里程碑。 */
    public static final int MILESTONE_WHISPER = 2;
    public static final int MILESTONE_WATCHED = 3;
    public static final int MILESTONE_COMPLETE = 5;

    private MemoryProgression() {
    }

    /**
     * 在收集一条新记忆后调用。
     *
     * @param previousCount 收集前的数量
     * @param newCount      收集后的数量
     */
    public static void onMemoryCollected(ServerPlayer player, ServerLevel level,
                                         int previousCount, int newCount) {
        if (newCount <= previousCount) {
            return;
        }
        if (newCount == MILESTONE_WHISPER) {
            notifyPlayer(player, "message.echoesofoblivion.milestone.whisper");
            level.playSound(null, player.blockPosition(), ModSounds.WHISPER_AMBIENT.get(),
                SoundSource.AMBIENT, 0.7f, 0.8f);
        } else if (newCount == MILESTONE_WATCHED) {
            notifyPlayer(player, "message.echoesofoblivion.milestone.watched");
            level.playSound(null, player.blockPosition(), ModSounds.HEARTBEAT.get(),
                SoundSource.AMBIENT, 0.8f, 0.9f);
        } else if (newCount >= MILESTONE_COMPLETE) {
            notifyPlayer(player, "message.echoesofoblivion.milestone.complete");
            level.playSound(null, player.blockPosition(), ModSounds.CORRUPTION_PULSE.get(),
                SoundSource.AMBIENT, 1.0f, 0.7f);
        }
    }

    /** 用于判断「是否已完整见证」——Boss 阶段的解锁条件之一。 */
    public static boolean isComplete(PlayerProgress progress) {
        return progress.collectedCount() >= MemoryRegistry.size();
    }

    /** 侵蚀倍率：见证得越多，被污染得越快。 */
    public static int corruptionMultiplier(PlayerProgress progress) {
        return 1 + progress.collectedCount();
    }

    private static void notifyPlayer(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), false);
    }
}
