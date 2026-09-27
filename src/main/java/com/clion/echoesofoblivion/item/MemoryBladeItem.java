package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 记忆之刃（v2.0.0）——**对回响有效，对聚合体无效**。
 *
 * <h2>这件武器存在的意义是它打不动 Boss</h2>
 *
 * <p>模组的核心设定是「武器无用」：聚合体不是生物，是一段收敛中的概念，
 * 它没有血条，只有稳定性，而稳定性只能用**记忆**去削。
 *
 * <p>如果加一把趁手的刀，玩家会自然地尝试用它砍 Boss——然后发现完全无效。
 * 这个「无效」本身就是叙事：你会先怀疑是伤害不够，再怀疑是附魔不对，
 * 最后才明白**这里根本没有可以被砍的东西**。这个认知过程比任何台词都有效。
 *
 * <p>所以这把刀是刻意做成「诱饵」的，但诱饵必须真的好用——
 * 它对亡魂追加伤害，是清场的最优解。玩家会依赖它，
 * 然后在 Boss 面前被迫放下它。这是设计上的必然，不是恶作剧。
 *
 * <h2>实现要点</h2>
 *
 * <ul>
 *   <li>对 {@code PhantomEntity} 追加伤害。</li>
 *   <li>对 {@code SilentAggregateEntity} 造成 <b>0</b> 伤害，并给出明确的失败反馈。</li>
 *   <li>耐久由 {@code ItemStack#hurtEnemy} 统一结算，这里<b>不</b>再调 hurtAndBreak，
 *       否则每次攻击会扣两点耐久。</li>
 * </ul>
 */
public class MemoryBladeItem extends Item {

    /** 对亡魂的额外伤害。 */
    private static final float PHANTOM_BONUS = 6.0f;

    public MemoryBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 对聚合体：完全无效。耐久仍会正常消耗（无效不等于免费）。
        if (target instanceof SilentAggregateEntity) {
            if (attacker instanceof Player player && !player.level().isClientSide) {
                player.displayClientMessage(
                    Component.translatable("item.echoesofoblivion.memory_blade.no_effect")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
            return true;
        }

        // 对亡魂追加伤害。这里直接改血量而不是再发一次伤害事件，
        // 是为了让「这一刀特别重」在数值上确定，不被无敌帧吞掉。
        if (target instanceof PhantomEntity && attacker.level() instanceof ServerLevel level) {
            Player owner = attacker instanceof Player p ? p : null;
            target.hurt(level.damageSources().playerAttack(owner), PHANTOM_BONUS);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1.0, target.getZ(),
                18, 0.3, 0.4, 0.3, 0.04);
            level.playSound(null, target.blockPosition(), ModSounds.BOSS_HIT.get(),
                SoundSource.PLAYERS, 0.6f, 1.7f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.memory_blade.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.memory_blade.useless")
            .withStyle(ChatFormatting.RED));
    }
}
