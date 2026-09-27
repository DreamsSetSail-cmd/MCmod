package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.MemoryCrystalUsePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 记忆水晶方块（阶段 1）。
 *
 * <p>关键修复：旧实现的客户端/服务端判断反了——{@code if (level.isClientSide) return SUCCESS;}
 * 让客户端直接返回、从不发包，于是 {@code MemoryCrystalUsePacket} 永远发不出去，
 * 整条记忆收集链路是断的。正确方向是<b>客户端发包、服务端处理</b>。
 */
public class MemoryCrystalBlock extends Block {

    public MemoryCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                              Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            // 客户端：请求服务端判定这枚水晶提供哪条记忆
            ModNetwork.sendToServer(new MemoryCrystalUsePacket(pos));
            return InteractionResult.SUCCESS;
        }
        // 服务端逻辑由 ServerPacketHandler 驱动，避免客户端直接改状态
        return InteractionResult.CONSUME;
    }
}
