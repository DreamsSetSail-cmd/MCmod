package com.clion.echoesofoblivion;

import com.clion.echoesofoblivion.entity.MirrorEntity;
import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.ShadowSyncPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.RealityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 镜像区块的运行时行为（阶段 5）。
 *
 * <p>职责划分：{@link RealityData} 只管「哪些区块被感染」与持久化；
 * 本类负责扩散节奏、重力闪烁、粒子与亡魂生成这些运行时效果。
 *
 * <p>旧实现直接在 {@code PlayerTickEvent} 里嵌套三层随机撒实体，既没有持久化、
 * 也没有边界，属于必须替换的临时逻辑。
 */
public class MirrorChunkHandler {

    /** 重力闪烁的检查间隔（tick）。 */
    private static final int FLICKER_INTERVAL = 10;

    /** 感染区内每个玩家每次检查触发重力的概率。 */
    private static final float GRAVITY_FLICKER_CHANCE = 0.06f;

    /**
     * 「镜中的你」的出现概率（v1.2.0）。
     *
     * <p>检查间隔是 10 tick，因此 0.0006 大约等于每 10000 次检查出现一次，
     * 在感染区持续活动时约每 10~20 分钟一次。这个频率是刻意的：
     * 它必须罕见到你无法确认它是机制还是错觉。
     */
    private static final float MIRROR_CHANCE = 0.0006f;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (!ConfigHelper.getBoolean(Config.enableMirrorChunks, true)) {
            return;
        }

        int intervalTicks = ConfigHelper.getInt(Config.mirrorExpandInterval, 300) * 20;
        if (level.getGameTime() % intervalTicks != 0) {
            return;
        }

        RealityData data = RealityData.get(level);
        for (ServerPlayer player : level.players()) {
            List<Long> added = data.spreadFrom(player);
            if (!added.isEmpty()) {
                data.broadcastIncrement(level, added);
                level.playSound(null, player.blockPosition(), ModSounds.CORRUPTION_PULSE.get(),
                    SoundSource.AMBIENT, 0.5f, 0.7f);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (!ConfigHelper.getBoolean(Config.enableMirrorChunks, true)) {
            return;
        }
        if (player.tickCount % FLICKER_INTERVAL != 0) {
            return;
        }

        RealityData data = RealityData.get(level);
        if (!data.isInfected(player.getX(), player.getZ())) {
            return;
        }

        applyInfectedEffects(level, player, data);
    }

    /** 玩家身处感染区块内时的表现：重力闪烁 + 粒子 + 亡魂。 */
    private void applyInfectedEffects(ServerLevel level, ServerPlayer player, RealityData data) {
        var random = player.getRandom();

        // 1. 重力闪烁：短暂地把玩家向上（或向下）推一下，制造「空间不稳」的错觉
        if (random.nextFloat() < GRAVITY_FLICKER_CHANCE) {
            Vec3 movement = player.getDeltaMovement();
            double vertical = movement.y;
            if (random.nextBoolean()) {
                // 反重力
                player.setDeltaMovement(movement.x, Math.min(0.6, vertical + 0.45), movement.z);
            } else {
                // 突然加重
                player.setDeltaMovement(movement.x, vertical - 0.35, movement.z);
            }
            player.hurtMarked = true;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.5, 0.8, 0.5, 0.02);
        }

        // 2. 时间倒流的廉价表现：反向运动的末地烛粒子
        if (random.nextFloat() < 0.25f) {
            level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 2.0, player.getZ(),
                6, 1.0, 1.0, 1.0, -0.05);
        }

        // 3. 亡魂在感染区内成型（有总量上限，避免刷爆）
        if (random.nextFloat() < 0.02f && countNearbyPhantoms(level, player) < 6) {
            spawnPhantomNear(level, player);
        }

        // 4. 镜中的你（v1.2.0）：极低概率，且全球同时只允许一个。
        //    概率刻意压到远低于亡魂——它必须罕见，否则会变成一种「怪物」，
        //    而它应当始终是「我是不是看错了」。
        if (random.nextFloat() < MIRROR_CHANCE && countNearbyMirrors(level, player) == 0) {
            spawnMirrorNear(level, player);
        }

        // 5. 感染区的影子状态强制拉高
        ModNetwork.sendToPlayer(new ShadowSyncPacket(4), player);
    }

    private int countNearbyPhantoms(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(PhantomEntity.class, player.getBoundingBox().inflate(48)).size();
    }

    private int countNearbyMirrors(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(MirrorEntity.class, player.getBoundingBox().inflate(96)).size();
    }

    /**
     * 在玩家视野**之外**放置一个镜中的你。
     *
     * <p>生成距离 24~40 格：太近会立刻触发它的消散（6 格规则），太远则在雾里看不见。
     * 这个距离让玩家有机会先瞥见一个静止的人形，然后在靠近时失去它。
     */
    private void spawnMirrorNear(ServerLevel level, ServerPlayer player) {
        MirrorEntity mirror = ModEntities.MIRROR.get().create(level);
        if (mirror == null) {
            return;
        }
        var random = player.getRandom();

        // 生成在玩家的侧后方，避免正前方「凭空出现」
        double angle = Math.toRadians(player.getYRot() + 120.0 + random.nextDouble() * 120.0);
        double distance = 24.0 + random.nextDouble() * 16.0;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;

        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (int) x, (int) z);
        mirror.moveTo(x, y, z, random.nextFloat() * 360.0f, 0.0f);
        level.addFreshEntity(mirror);
        // 刻意不播放任何音效：有声音它就成了「一个怪物」
    }

    private void spawnPhantomNear(ServerLevel level, ServerPlayer player) {
        PhantomEntity phantom = ModEntities.PHANTOM.get().create(level);
        if (phantom == null) {
            return;
        }
        var random = player.getRandom();
        double x = player.getX() + random.nextInt(-24, 25);
        double z = player.getZ() + random.nextInt(-24, 25);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (int) x, (int) z);
        phantom.moveTo(x, y, z, random.nextFloat() * 360.0f, 0.0f);
        level.addFreshEntity(phantom);
        level.playSound(null, phantom.blockPosition(), ModSounds.WHISPER_AMBIENT.get(),
            SoundSource.HOSTILE, 0.4f, 0.7f);
    }

    /** 玩家进入主世界时把当前感染状态全量推给他。 */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
            && player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            RealityData.get(player.serverLevel()).syncFull(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
            && player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            RealityData.get(player.serverLevel()).syncFull(player);
        }
    }

    /** 供其他系统查询：玩家是否正处在感染区内。 */
    public static boolean isPlayerInfected(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        return RealityData.get(level).isInfected(player.getX(), player.getZ());
    }
}
