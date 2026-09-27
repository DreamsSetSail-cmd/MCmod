package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.BossAuraPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 聚合体的环境级技能（v1.2.0）。
 *
 * <h2>为什么是「环境级」</h2>
 *
 * <p>设计上有一条硬线：**聚合体不能开始追打玩家**。一旦它有了伤害技能，
 * 它就从一个「现象」退化成一只「怪物」——而整个模组的恐怖前提是
 * 它是一种无法被战斗逻辑理解的东西。
 *
 * <p>因此它的技能不是伤害，而是**改变玩家所处的环境**：声音消失、雾变浓、
 * 亡魂成形。玩家感受到压力，但找不到「它在攻击我」的确切证据。
 *
 * <h2>三档技能</h2>
 *
 * <table>
 *   <tr><th>阶段</th><th>技能</th><th>表现</th></tr>
 *   <tr><td>1（稳定性 ≤60）</td><td>回响成形</td><td>半径 20 内生成 2~3 个亡魂，
 *       随它一起存在——不是它召唤的援军，而是它周围开始出现东西</td></tr>
 *   <tr><td>2（稳定性 ≤30）</td><td>静默领域</td><td>光环内的**所有声音被压掉**
 *       （通过 {@code BossAuraPacket} 让客户端抑制环境音）</td></tr>
 *   <tr><td>濒死</td><td>收敛</td><td>粒子向中心塌缩，配合终结演出</td></tr>
 * </table>
 */
public final class BossAura {

    /** 光环半径：玩家在此距离内开始失去环境音。 */
    public static final double AURA_RADIUS = 24.0;

    /** 光环强度的同步间隔（tick）。 */
    private static final int SYNC_INTERVAL = 20;

    /** 回响成形的最大亡魂数（同一 Boss 周围）。 */
    private static final int MAX_ECHOES = 3;

    private BossAura() {
    }

    /**
     * 每 tick 由 {@code SilentAggregateEntity} 调用。
     *
     * @param phase 当前阶段（0/1/2）
     */
    public static void tick(SilentAggregateEntity boss, int phase) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }

        // 光环强度同步：距离越近越静。阶段 0 时光环很弱（只有一点压抑感）。
        if (boss.tickCount % SYNC_INTERVAL == 0) {
            syncAura(boss, level, phase);
        }

        // 阶段 1 起：亡魂在它周围成形（不是它召唤的，是「周围开始出现东西」）
        if (phase >= 1 && boss.tickCount % (SYNC_INTERVAL * 10) == 0) {
            formEchoes(boss, level);
        }

        // 阶段 2：静默领域的视觉部分——反向粒子，暗示「这一带在往回吸」
        if (phase >= 2 && boss.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                boss.getX(), boss.getY() + 2.0, boss.getZ(),
                12, AURA_RADIUS * 0.6, 2.0, AURA_RADIUS * 0.6, 0.01);
        }
    }

    /** 把光环强度推给范围内的玩家（范围外的推 0，让他们恢复声音）。 */
    private static void syncAura(SilentAggregateEntity boss, ServerLevel level, int phase) {
        // 阶段越高，静默越强；阶段 0 只给一点点「不对劲」
        float base = switch (phase) {
            case 2 -> 1.0f;
            case 1 -> 0.55f;
            default -> 0.15f;
        };

        for (ServerPlayer player : level.players()) {
            double distance = Math.sqrt(player.distanceToSqr(boss));
            float intensity;
            if (distance >= AURA_RADIUS) {
                intensity = 0.0f;
            } else {
                // 从边缘到中心线性增强
                float proximity = (float) (1.0 - distance / AURA_RADIUS);
                intensity = base * proximity;
            }
            ModNetwork.sendToPlayer(new BossAuraPacket(intensity), player);
        }
    }

    /**
     * 在聚合体周围让亡魂成形。
     *
     * <p>它们不走过来——生成后由各自的 AI 决定行为。这里只负责「让它们出现」，
     * 而出现本身就是压力：玩家会看到聚合体周围的东西变多了。
     */
    private static void formEchoes(SilentAggregateEntity boss, ServerLevel level) {
        int existing = level.getEntitiesOfClass(PhantomEntity.class,
            boss.getBoundingBox().inflate(AURA_RADIUS)).size();
        if (existing >= MAX_ECHOES) {
            return;
        }

        var random = boss.getRandom();
        int toSpawn = 1 + random.nextInt(2);
        for (int i = 0; i < toSpawn && existing + i < MAX_ECHOES; i++) {
            PhantomEntity phantom = ModEntities.PHANTOM.get().create(level);
            if (phantom == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 8.0 + random.nextDouble() * (AURA_RADIUS - 10.0);
            int x = (int) (boss.getX() + Math.cos(angle) * dist);
            int z = (int) (boss.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            phantom.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0f, 0.0f);
            level.addFreshEntity(phantom);

            level.sendParticles(ParticleTypes.SOUL,
                x + 0.5, y + 1.0, z + 0.5, 20, 0.4, 0.8, 0.4, 0.01);
        }
        // 一声很轻的、位置不对的低语
        level.playSound(null, BlockPos.containing(boss.getX(), boss.getY(), boss.getZ()),
            ModSounds.WHISPER_AMBIENT.get(), SoundSource.HOSTILE, 0.5f, 0.6f);
    }

    /**
     * 终结演出（由 {@code BossCombat} 调用）。
     *
     * <p>收敛：粒子向中心塌缩，配合音效。它不是「爆炸」，
     * 而是**所有东西同时收回去了**——与它「一切的不在」的设定一致。
     */
    public static void collapse(SilentAggregateEntity boss, ServerLevel level) {
        level.sendParticles(ParticleTypes.SQUID_INK,
            boss.getX(), boss.getY() + 2.0, boss.getZ(),
            80, 3.0, 3.0, 3.0, -0.15);
        level.playSound(null, boss.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
            SoundSource.HOSTILE, 1.0f, 0.5f);
    }

    /** 玩家离开世界或 Boss 消失时，把光环强度清零，避免声音一直被压着。 */
    public static void clearAura(ServerPlayer player) {
        ModNetwork.sendToPlayer(new BossAuraPacket(0.0f), player);
    }

    /** 供外部查询：某玩家是否身处光环内。 */
    public static boolean isInAura(SilentAggregateEntity boss, Player player) {
        return player.distanceToSqr(boss) < AURA_RADIUS * AURA_RADIUS;
    }
}
