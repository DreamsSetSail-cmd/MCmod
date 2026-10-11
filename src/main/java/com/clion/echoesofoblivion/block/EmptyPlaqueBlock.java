package com.clion.echoesofoblivion.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 空碑（v2.5.0）——**「缺席优于出现」的最终形态**。
 *
 * <h2>它是什么</h2>
 *
 * <p>击败聚合体、走完清点之后，世界中会出现一块**比原来更多**的碑——
 * 而它是空的。
 *
 * <p>不解释。这是全篇最后一次反转的收尾：
 * 你以为你赢了，而有些东西比你赢之前**更少**。
 *
 * <h2>为什么它不给任何东西</h2>
 *
 * <p>右键它只会得到一句平淡的反馈。**没有战利品、没有成就、没有提示。**
 * 如果它给任何东西，它就变成了一个「通关奖励」，而它要传达的恰恰是
 * **什么都没有多出来，反而少了一块**。
 *
 * <h2>它为什么叫「空碑」</h2>
 *
 * <p>名字碑是**可以被写上去的**（v2.3.0）。空碑**不接受任何铭刻**——
 * 手持空白铭牌右键它，它只是继续空着。
 *
 * <p>玩家会试。那一下「试了但不行」比任何文字都有效。
 */
public class EmptyPlaqueBlock extends Block {

    public EmptyPlaqueBlock(Properties properties) {
        super(properties);
    }

    /**
     * 手持物品右键：刻意**拒绝**铭刻。
     *
     * <p>注意这里返回 {@code CONSUME} 而不是把物品用掉——
     * 玩家拿着空白铭牌来刻，得到的是一句「这里不接受」，
     * 而铭牌**不会**被消耗。他还可以拿去刻别的地方。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.SUCCESS;
        }
        serverPlayer.displayClientMessage(
            Component.translatable("block.echoesofoblivion.empty_plaque.refused")
                .withStyle(ChatFormatting.DARK_GRAY), true);
        return ItemInteractionResult.CONSUME;
    }

    /** 空手右键：一句平淡的观察，没有解释。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                              Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                Component.translatable("block.echoesofoblivion.empty_plaque.look")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return InteractionResult.SUCCESS;
    }
}
