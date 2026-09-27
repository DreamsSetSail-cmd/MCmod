package com.clion.echoesofoblivion.entity;

import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 镜中的你（v1.2.0）。
 *
 * <p>心理恐怖最有效的形态：**它什么都不做。**
 *
 * <p>行为规则刻意压到最少：
 * <ul>
 *   <li><b>不攻击</b>——没有攻击目标、没有伤害、{@code isInvulnerable} 为真。</li>
 *   <li><b>不移动</b>——没有寻路目标，位置固定。它会转身看你，但不会走过来。</li>
 *   <li><b>靠近就消失</b>——玩家进入 6 格内时它消散（带一小片末地烛粒子）。
 *       这条是核心：你永远无法确认它是什么，因为你一想看清楚它就没了。</li>
 *   <li><b>会自行消散</b>——60 秒后消失，避免在世界上留下永久实体。</li>
 * </ul>
 *
 * <p>与「镜像区块」这个机制名的呼应是有意的：被感染的不是地形，是**你**。
 * 所以它长得和你一样。
 *
 * <p>它不发出任何声音。出现与消失都只有粒子，没有音效——
 * 一旦有声音，它就变成了「一个怪物」，而不是「你看错了吗」。
 */
public class MirrorEntity extends Mob {

    /** 玩家进入该距离内即消散（格）。 */
    private static final double VANISH_DISTANCE = 6.0;

    /** 自行消散的时间（tick）。60 秒。 */
    private static final int LIFETIME = 1200;

    /** 只在玩家视野之外转身：该距离内它保持静止，避免被看到「动」。 */
    private static final double TURN_DISTANCE = 16.0;

    public MirrorEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        // 完全不参与战斗
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)     // 它不走
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // 故意留空：任何寻路目标都会让它「动起来」，那就不再是同一个东西了。
        // 转身逻辑在 tick() 里手写，因为它需要条件判断（玩家是否可能看到）。
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        // 自行消散
        if (this.tickCount >= LIFETIME) {
            vanish(false);
            return;
        }

        Player nearest = this.level().getNearestPlayer(this, 64.0);
        if (nearest == null) {
            return;
        }

        // 玩家太近：消散。
        // 这是它唯一「回应」玩家的方式——它选择消失，而不是逃跑或攻击。
        if (this.distanceTo(nearest) < VANISH_DISTANCE) {
            vanish(true);
            return;
        }

        // 只在较远处转身面向玩家。近距离不转身，避免出现「它刚动了」的确证。
        if (this.distanceTo(nearest) > TURN_DISTANCE) {
            faceTowards(nearest);
        }
    }

    /** 平缓地把视线转向目标（不改位置）。 */
    private void faceTowards(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0f;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    /**
     * 消散。
     *
     * @param watchedByPlayer 是否因为玩家靠近而消散。区别只在于粒子量——
     *                        被看着消失时给得更明显一点，让玩家「确实看见了什么」。
     */
    private void vanish(boolean watchedByPlayer) {
        if (this.level() instanceof ServerLevel serverLevel) {
            int count = watchedByPlayer ? 40 : 12;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.0, this.getZ(),
                count, 0.4, 0.9, 0.4, -0.02);
            // 消散时给一声极轻的、被掐断的低语——不是尖叫，是话说到一半停住
            if (watchedByPlayer) {
                serverLevel.playSound(null, this.blockPosition(), ModSounds.WHISPER_AMBIENT.get(),
                    SoundSource.AMBIENT, 0.25f, 0.6f);
            }
        }
        this.discard();
    }

    /** 不参与任何战斗判定。 */
    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 打不到它。攻击只会让它消失，给出「刚才真的有东西」的确认。
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            vanish(true);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;  // 由 LIFETIME 控制，不因为距离被卸载
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;  // 不可推动、不可挤压
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
