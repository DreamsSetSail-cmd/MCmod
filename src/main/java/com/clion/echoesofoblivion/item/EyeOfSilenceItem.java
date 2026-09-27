package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.server.BossCombat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 寂静之眼（阶段 6）——Boss 召唤凭证。
 *
 * <p>召唤条件（由 {@link BossCombat#trySummon} 判定）：
 * 已见证全部 5 条记忆 + 身处镜像区块（感染区）内 + 附近没有其他 Boss。
 * 三个条件缺一不可，因此它不能靠运气蒙出来。
 */
public class EyeOfSilenceItem extends Item {

    public EyeOfSilenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        boolean summoned = BossCombat.trySummon(serverPlayer);
        if (summoned) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        // 失败原因已经通过消息反馈给玩家
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 附魔光效：提示「这件物品不是普通道具」
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.echoesofoblivion.eye_of_silence");
    }
}
