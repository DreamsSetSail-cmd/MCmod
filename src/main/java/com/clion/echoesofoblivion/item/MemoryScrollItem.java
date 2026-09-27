package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.client.ClientPacketHandler;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.CodexRequestPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/**
 * 记忆卷轴（v1.1.0 实现为图鉴入口）。
 *
 * <p>旧版只是普通物品。现在右键它会：
 * <ol>
 *   <li>客户端发一次 {@link CodexRequestPacket}，让服务端把最新进度同步过来</li>
 *   <li>客户端立即打开 {@link com.clion.echoesofoblivion.client.screen.MemoryCodexScreen}</li>
 * </ol>
 *
 * <p>顺序上先请求再打开：同步是幂等的完整状态，晚到几百毫秒只会让图鉴自己刷新一下，
 * 比「等同步回来再开界面」的手感更好。
 */
public class MemoryScrollItem extends Item {

    public MemoryScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // 请求刷新进度，然后打开图鉴。
            // DistExecutor 的工厂只在客户端求值，因此 ClientPacketHandler
            // 在服务端永远不会被解析到（与各网络包的处理方式一致）。
            ModNetwork.sendToServer(CodexRequestPacket.INSTANCE);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandler::openCodex);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.memory_scroll.desc"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
