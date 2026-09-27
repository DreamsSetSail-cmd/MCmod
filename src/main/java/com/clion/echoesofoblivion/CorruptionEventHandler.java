package com.clion.echoesofoblivion;

import com.clion.echoesofoblivion.item.ResearcherLanternItem;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket;
import com.clion.echoesofoblivion.network.packets.ShadowSyncPacket;
import com.clion.echoesofoblivion.server.ServerPacketHandler;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.ModWorldGen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 侵蚀值与状态同步（阶段 4 的服务端骨架，在阶段 1 就先把「同步」这条链路打通）。
 *
 * <p>修复的三个问题：
 * <ol>
 *   <li>旧实现同时维护 {@code persistentData.echoes_corruption} 与 {@code PlayerMemoryData.corruptionLevel}
 *       两套侵蚀值，而客户端显示用的数值没有任何来源——现在唯一定义在 {@link PlayerProgress}。</li>
 *   <li>旧实现用「维度名字符串里是否含 silent」判断所处维度，脆弱且不符合规范——改用维度 {@code ResourceKey} 比较。</li>
 *   <li>旧实现登录时只同步侵蚀值，不同步「已收集记忆」——现在两者都同步。</li>
 * </ol>
 *
 * <p>镜像区块的扩散与亡魂生成属于阶段 5，届时会迁入 {@code RealityData}，
 * 这里不再出现「用随机数在玩家周围直接生成实体」的临时逻辑。
 */
public class CorruptionEventHandler {

    /** 同步间隔（tick）。 */
    private static final int SYNC_INTERVAL = 20;

    /** 「它叫你的名字」的侵蚀阈值。 */
    private static final int NAME_CALL_CORRUPTION = 75;

    /** 两次叫名之间的最短间隔（秒）。 */
    private static final int NAME_CALL_MIN_INTERVAL = 240;

    /**
     * 每秒触发叫名的概率。
     *
     * <p>{@code 1/480} 意味着在侵蚀持续高于阈值时，中位等待约 8 分钟。
     * 配合 4 分钟的最短间隔，实际节奏大约每 8~15 分钟一次。
     */
    private static final float NAME_CALL_CHANCE = 1.0f / 480.0f;

    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();

        UUID playerId = player.getUUID();
        int ticks = playerTickCounters.merge(playerId, 1, Integer::sum);
        if (ticks % SYNC_INTERVAL != 0) {
            return;
        }

        PlayerMemoryData data = PlayerMemoryData.get(level);
        PlayerProgress progress = data.progressOf(player);

        // 记录增长前的值，供提灯的净变化折扣使用
        int beforeGain = progress.corruption();

        // 每秒推进一次侵蚀值
        int gain = ConfigHelper.getInt(Config.corruptionGainRate, 1);
        if (gain > 0) {
            progress.addCorruption(gain);
        }
        // 身处寂静走廊时侵蚀显著加速——「真相在污染你」
        if (level.dimension().equals(ModWorldGen.SILENT_CORRIDOR)) {
            progress.addCorruption(2);
        }

        // v2.0.0：研究者提灯让污染长得慢一半。它不是治疗，只是把终点推远。
        // 用「净变化」而不是逐个来源打折，是为了保证它在走廊（+2）里同样有效——
        // 只对 gain 打折会让这件物品在最需要它的地方失效。
        int delta = progress.corruption() - beforeGain;
        if (delta > 0 && ResearcherLanternItem.isCarried(player)) {
            int reduction = Math.max(1, Math.round(delta * (1.0f - ResearcherLanternItem.GROWTH_MULTIPLIER)));
            progress.addCorruption(-reduction);
        }

        int before = progress.corruption();
        data.markDirty();

        ModNetwork.sendToPlayer(new CorruptionUpdatePacket(before, progress.corruptionRatio()), player);
        ModNetwork.sendToPlayer(new ShadowSyncPacket(shadowStateFor(before)), player);

        // 高侵蚀：低语与心跳（音量随侵蚀升高）
        if (before >= 50 && ticks % (SYNC_INTERVAL * 5) == 0) {
            float volume = 0.25f * progress.corruptionRatio();
            player.playNotifySound(ModSounds.WHISPER_AMBIENT.get(), SoundSource.AMBIENT, volume, 0.8f);
        }
        if (before >= 75 && ticks % (SYNC_INTERVAL * 3) == 0) {
            float volume = 0.35f * progress.corruptionRatio();
            player.playNotifySound(ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT, volume, 1.0f);
        }

        // 「它叫你的名字」（v1.2.0）
        maybeCallPlayerName(player, progress, before);
    }

    /**
     * 侵蚀很高时，极低概率在聊天栏出现「（你的名字）。」——没有别的内容。
     *
     * <p>这是「语言被污染」这一核心设定最直接的用法：它证明那个东西**认识你**，
     * 而它认识你的方式，是把你变成它的词汇之一。
     *
     * <p>频率刻意压得极低（中位约 8 分钟一次，且至少间隔 4 分钟）。
     * 一次就足够恐怖；如果每几分钟来一次，它就会退化成骚扰而不是恐怖。
     * 计时器存在存档里，因此重连无法影响它。
     */
    private static void maybeCallPlayerName(ServerPlayer player, PlayerProgress progress, int corruption) {
        progress.tickNameCallTimer();
        if (corruption < NAME_CALL_CORRUPTION) {
            return;
        }
        if (progress.secondsSinceNameCall() < NAME_CALL_MIN_INTERVAL) {
            return;
        }
        if (player.getRandom().nextFloat() >= NAME_CALL_CHANCE) {
            return;
        }

        progress.resetNameCallTimer();
        // 用玩家自己的名字作为参数，翻译文件只负责括号与标点
        player.displayClientMessage(
            Component.translatable("message.echoesofoblivion.name_call", player.getName()), false);
    }

    /**
     * 侵蚀值 → 影子状态码。服务端决定，客户端只负责表现。
     */
    public static int shadowStateFor(int corruption) {
        if (corruption < 20) {
            return 0;
        }
        if (corruption < 40) {
            return 1;
        }
        if (corruption < 60) {
            return 2;
        }
        if (corruption < 80) {
            return 3;
        }
        return 4;
    }

    /** 登录 / 重生 / 跨维度时全量同步进度，保证客户端缓存与存档一致。 */
    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 保险：上一次会话若在聚合体光环内退出，客户端会残留一个被压制的雾效。
            // 登录时先清零，让服务端在下一 tick 重新按实际距离计算。
            com.clion.echoesofoblivion.server.BossAura.clearAura(player);
            syncAll(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAll(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAll(player);
        }
    }

    private static void syncAll(ServerPlayer player) {
        PlayerMemoryData data = PlayerMemoryData.get(player.serverLevel());
        PlayerProgress progress = data.progressOf(player);

        // 位掩码 + 线索 + 侵蚀值统一由 ServerPacketHandler 拼装，避免两处实现漂移
        ServerPacketHandler.syncProgress(player, progress);
        ModNetwork.sendToPlayer(new ShadowSyncPacket(shadowStateFor(progress.corruption())), player);
    }
}
