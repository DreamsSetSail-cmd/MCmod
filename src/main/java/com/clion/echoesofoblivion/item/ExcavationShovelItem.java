package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 探查铲（v2.0.0）——**不摧毁，只取样**。
 *
 * <h2>为什么它不挖掉水晶</h2>
 *
 * <p>直觉做法是「铲子挖水晶掉材料」，但那会破坏整条链路：
 * 水晶是按坐标确定性分配记忆的载体，挖掉它等于把一段历史从世界里删了，
 * 而且玩家会开始**为了材料而破坏叙事资源**——这是最坏的一种玩法激励。
 *
 * <p>所以铲子做的是考古动作：从水晶表面刮下**水晶尘**（合成材料），
 * 有几率得到一枚**被封住的残片**，水晶本身留在原地、仍可阅读。
 * 取样的代价是镐级耐久消耗，因此它是有限次数的。
 *
 * <h2>被封住的残片怎么开</h2>
 *
 * <p>同样用这把铲子：手持被封住的残片右键，开启它。
 * 这个链路刻意保持在一件工具上——「你不能用别的办法得到它」，
 * 就像真正的考古一样，工具本身就是方法。
 */
public class ExcavationShovelItem extends Item {

    /** 一次取样得到的尘数量下限/上限。 */
    private static final int DUST_MIN = 1;
    private static final int DUST_MAX = 3;

    /** 一次取样得到被封残片的概率。 */
    private static final float SEALED_CHANCE = 0.22f;

    /** 每次取样消耗的耐久。 */
    private static final int DURABILITY_COST = 2;

    public ExcavationShovelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // 只有记忆水晶可以被取样
        if (!state.is(ModBlocks.MEMORY_CRYSTAL.get())) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();

        // 演出：刮下来的粉尘往下落，声音是石头而非玻璃——它是矿物，不是宝物
        serverLevel.sendParticles(ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            24, 0.3, 0.3, 0.3, 0.08);
        serverLevel.playSound(null, pos, ModSounds.CORRUPTION_PULSE.get(),
            SoundSource.BLOCKS, 0.5f, 2.0f);

        // 产出：水晶尘必得
        int dust = DUST_MIN + player.getRandom().nextInt(DUST_MAX - DUST_MIN + 1);
        player.getInventory().placeItemBackInInventory(
            new ItemStack(ModItems.CRYSTAL_DUST.get(), dust));

        // 有几率连带取出被封住的残片
        boolean sealed = player.getRandom().nextFloat() < SEALED_CHANCE;
        if (sealed) {
            player.getInventory().placeItemBackInInventory(
                new ItemStack(ModItems.SEALED_FRAGMENT.get(), 1));
            player.displayClientMessage(
                Component.translatable("item.echoesofoblivion.excavation_shovel.sealed")
                    .withStyle(ChatFormatting.GOLD), true);
        }

        // 耐久：创造模式不消耗
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(DURABILITY_COST, player, slotFor(context.getHand()));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * 手持被封住的残片右键：开启它。
     *
     * <p>这里复用 {@code use} 而不是 {@code useOn}，因为开启的动作不针对方块。
     * 判断的是**副手/主手的另一只手**里有没有被封残片，
     * 这样玩家可以「一手铲子一手残片」，是考古的标准姿势。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(tool);
        }

        // 在两只手里找被封住的残片（优先另一只手）
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND
            : InteractionHand.MAIN_HAND;
        ItemStack sealedStack = player.getItemInHand(otherHand);
        if (!sealedStack.is(ModItems.SEALED_FRAGMENT.get())) {
            sealedStack = tool.is(ModItems.SEALED_FRAGMENT.get()) ? tool : ItemStack.EMPTY;
        }
        if (sealedStack.isEmpty()) {
            // 没有任何被封住的残片可开：交回给原版处理（铲子因此仍能正常挖土）
            return InteractionResultHolder.fail(tool);
        }

        // 开启：给出一枚随机的残片
        var fragments = ModItems.fragments().values().stream().toList();
        if (fragments.isEmpty()) {
            return InteractionResultHolder.fail(tool);
        }
        var chosen = fragments.get(player.getRandom().nextInt(fragments.size()));

        if (!player.getAbilities().instabuild) {
            sealedStack.shrink(1);
            tool.hurtAndBreak(1, player, slotFor(hand));
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(chosen.get(), 1));

        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            player.getX(), player.getY() + 1.2, player.getZ(),
            30, 0.4, 0.5, 0.4, 0.03);
        serverLevel.playSound(null, player.blockPosition(), ModSounds.MEMORY_COLLECT.get(),
            SoundSource.PLAYERS, 0.8f, 1.2f);
        serverPlayer.displayClientMessage(
            Component.translatable("item.echoesofoblivion.excavation_shovel.opened")
                .withStyle(ChatFormatting.AQUA), false);

        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(tool);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.excavation_shovel.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.excavation_shovel.keep")
            .withStyle(ChatFormatting.GRAY));
    }

    /** 交互手 → 装备槽。{@code ItemStack#hurtAndBreak} 需要槽位而不是手。 */
    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
}
