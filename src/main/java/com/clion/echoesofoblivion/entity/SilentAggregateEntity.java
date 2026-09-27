package com.clion.echoesofoblivion.entity;

import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.RenderStatePacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 寂静聚合体（阶段 6）——概念型 Boss。
 *
 * <p>设计（对应技术大纲 §8.1 与 README 的「概念 Boss 战」）：
 * <ul>
 *   <li>对一切常规武器免疫，{@code hurt} 只放行 {@code BYPASSES_INVULNERABILITY}
 *       （{@code /kill}、虚空伤害），否则播放「无效化」反馈。</li>
 *   <li>真正的伤害来自「稳定性（stability）」：玩家用已收集的记忆作为武器，
 *       由 {@code BossCombat} 判定并扣减。</li>
 *   <li>恐惧光环：周期性把附近玩家的侵蚀值拉高，距离越近越快。</li>
 *   <li>阶段 0/1/2 由稳定性决定，同步给客户端做渲染变化。</li>
 * </ul>
 */
public class SilentAggregateEntity extends Monster {

    public static final int MAX_STABILITY = 100;

    /** 恐惧光环半径。 */
    private static final double AURA_RADIUS = 16.0;

    /**
     * 台词的送达半径。比光环大得多——那几句话不是「对你喊」，
     * 而是整片区域都听见了，包括你。
     */
    private static final double ANNOUNCE_RADIUS = 48.0;

    private int stability = MAX_STABILITY;
    private int phase;

    public SilentAggregateEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        // Boss 不会被推动
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // 刻意缓慢：它不是来追杀你的，它是来等你明白的
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 48.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------ 免疫

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            // 无效化反馈：武器打上去只会让空气变形
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                18, 1.0, 1.2, 1.0, 0.02);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.BOSS_HIT.get(),
                SoundSource.HOSTILE, 0.6f, 0.6f);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // 火焰、溺水、摔落等环境伤害一律无效
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    // ------------------------------------------------------------ 每 tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        // 恐惧光环：每秒一次，离得越近侵蚀越快
        if (this.tickCount % 20 == 0) {
            applyFearAura();
        }

        // 阶段推进（供渲染与技能使用）
        int nextPhase = stability <= 30 ? 2 : (stability <= 60 ? 1 : 0);
        if (nextPhase != phase) {
            phase = nextPhase;
            broadcastPhase();
        }
    }

    private void applyFearAura() {
        for (Player player : this.level().getEntitiesOfClass(Player.class,
            this.getBoundingBox().inflate(AURA_RADIUS))) {
            double distance = Math.sqrt(player.distanceToSqr(this));
            int gain = distance < AURA_RADIUS * 0.4 ? 4 : 2;
            com.clion.echoesofoblivion.server.BossCombat.addAuraCorruption(player, gain);
        }
    }

    private void broadcastPhase() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        RenderStatePacket packet = RenderStatePacket.bossPhase(phase, stabilityRatio());
        for (Player player : serverLevel.players()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                ModNetwork.sendToPlayer(packet, serverPlayer);
            }
        }
        announcePhase();
    }

    /**
     * 阶段切换时说一句被「改写」过的话。
     *
     * <p>这是全篇叙事「语言被污染」的唯一直接证据：句子语法完全正确，
     * 但主语可疑、意思不对——它不是在对玩家说话，它是在**用玩家的话**对玩家说话。
     *
     * <p>刻意极短、极轻（{@code false} 参数走聊天栏而非头顶），
     * 让它像从环境音里渗出来的一样，而不是一句正式的 Boss 台词。
     */
    private void announcePhase() {
        String key = switch (phase) {
            case 1 -> "message.echoesofoblivion.boss.phase1";
            case 2 -> "message.echoesofoblivion.boss.phase2";
            default -> null;
        };
        if (key == null) {
            return;
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class,
            this.getBoundingBox().inflate(ANNOUNCE_RADIUS))) {
            player.displayClientMessage(Component.translatable(key), false);
        }
    }

    /** 濒死时的最后一句。由 {@code BossCombat} 在结算前调用。 */
    public void announceDying() {
        for (Player player : this.level().getEntitiesOfClass(Player.class,
            this.getBoundingBox().inflate(ANNOUNCE_RADIUS))) {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.boss.dying"), false);
        }
    }

    // ------------------------------------------------------------ 稳定性

    public int getStability() {
        return stability;
    }

    public float stabilityRatio() {
        return stability / (float) MAX_STABILITY;
    }

    public int getPhase() {
        return phase;
    }

    /**
     * 扣减稳定性。
     *
     * @return 是否因此被击败
     */
    public boolean reduceStability(int amount) {
        this.stability = Math.max(0, this.stability - amount);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                24, 1.4, 1.8, 1.4, 0.05);
        }
        int nextPhase = stability <= 30 ? 2 : (stability <= 60 ? 1 : 0);
        if (nextPhase != phase) {
            phase = nextPhase;
            broadcastPhase();
        }
        return this.stability <= 0;
    }
}
