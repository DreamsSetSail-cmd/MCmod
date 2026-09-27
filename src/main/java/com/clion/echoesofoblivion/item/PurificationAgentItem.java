package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket;
import com.clion.echoesofoblivion.network.packets.RealityShiftPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.RealityData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 净化剂（v2.0.0）——**真的能清除感染的消耗品**。
 *
 * <h2>为什么需要它</h2>
 *
 * <p>v1.x 里镜像区块只会扩散，玩家对它毫无办法。这让「现实坍缩」从一种威胁
 * 变成了一种天气——你只能等它长大。有了净化剂，玩家第一次拥有了**对抗手段**，
 * 而且这个手段是有限、有代价的：
 *
 * <ul>
 *   <li>它清除玩家**所在区块**以及紧邻的 3x3 区块，不是整片感染区。</li>
 *   <li>它**不降低侵蚀值**——污染已经在你身上了，清掉地面上的没有用。</li>
 *   <li>它需要真材实料合成（虚空余烬 + 真理碎片 + 腐蚀精粹），因此不可能刷。</li>
 * </ul>
 *
 * <p>设计意图：让玩家在「我清得完吗」和「清这个值不值」之间做选择。
 * 感染区上限是 4096 区块，一瓶净化 9 个区块——算术本身就是答案。
 */
public class PurificationAgentItem extends Item {

    /** 一次净化的区块半径（0 表示只清当前区块，1 表示 3x3）。 */
    private static final int PURIFY_RADIUS = 1;

    public PurificationAgentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        RealityData reality = RealityData.get(serverLevel);
        ChunkPos center = new ChunkPos(player.blockPosition());

        // 收集本次要清除的区块
        List<Long> removed = new ArrayList<>();
        for (int dx = -PURIFY_RADIUS; dx <= PURIFY_RADIUS; dx++) {
            for (int dz = -PURIFY_RADIUS; dz <= PURIFY_RADIUS; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                if (reality.isInfected(cx, cz)) {
                    removed.add(ChunkPos.asLong(cx, cz));
                }
            }
        }

        if (removed.isEmpty()) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.purification_agent.clean")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        // 真正把它们从感染集合里移除
        int cleared = reality.removeChunks(removed);

        // 视觉与音效：一圈向外扩散的净化粒子
        for (long packed : removed) {
            BlockPos p = new BlockPos(ChunkPos.getX(packed) << 4, player.blockPosition().getY(),
                ChunkPos.getZ(packed) << 4);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                p.getX() + 8, p.getY() + 1.0, p.getZ() + 8, 30, 7.0, 2.0, 7.0, 0.02);
        }
        serverLevel.playSound(null, player.blockPosition(), ModSounds.PORTAL_OPEN.get(),
            SoundSource.PLAYERS, 0.6f, 1.6f);

        // 全量重推感染状态，让客户端立刻看到区块被清掉
        reality.syncFull(serverPlayer);

        serverPlayer.displayClientMessage(
            Component.translatable("item.echoesofoblivion.purification_agent.cleansed", cleared)
                .withStyle(ChatFormatting.AQUA), false);

        // 消耗品：非创造模式消耗一个
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.purification_agent.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.purification_agent.warning")
            .withStyle(ChatFormatting.RED));
    }

    /** 保留：连接 PlayerProgress 以便将来加「净化时顺带降低一点侵蚀」的变体。 */
    @SuppressWarnings("unused")
    private static void reportCorruption(ServerPlayer player) {
        PlayerProgress progress = PlayerMemoryData.get(player.serverLevel()).progressOf(player);
        ModNetwork.sendToPlayer(
            new CorruptionUpdatePacket(progress.corruption(), progress.corruptionRatio()), player);
    }
}
