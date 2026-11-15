package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.SilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 仪器核心（Apparatus Core，v2.6.0）——**那台仪器，由你重新造出来**。
 *
 * <h2>它为什么必须存在</h2>
 *
 * <p>从 v1.x 起，走廊里就散布着 {@code APPARATUS} 类型的遗迹：一座圆台，
 * 中心是一个**被挖空的槽**。文档一直写着「仪器已经不在了，或者说它完成了工作」。
 * 但它从来没有真正落地为一件玩家能做的东西——它一直是布景。
 *
 * <p>这一版把它做出来：玩家可以合成一枚核心，把它放进世界里，然后**启动它**。
 *
 * <h2>它做什么</h2>
 *
 * <p>启动之后，半径 {@link SilenceData#ACTIVATION_RADIUS} 区块内的世界**永久静默**：
 *
 * <ul>
 *   <li>这些区块**不可能被感染**，而且激活时会**清除**已有的感染。</li>
 *   <li>静默区里不再有错位的回响（介质已经没有东西可以腐坏了）。</li>
 *   <li>静默区里侵蚀**停止增长**。</li>
 *   <li>静默区会**缓慢扩散**，吃掉周围的区块。</li>
 *   <li>静默区里的残片龛**死掉**——它们不再交出任何东西。</li>
 * </ul>
 *
 * <p>也就是：**它真的有用。** 那个文明造它就是为了阻止扩散，而它有效。
 * 恐怖之处从来不是它没用。
 *
 * <h2>为什么不可撤回</h2>
 *
 * <p>没有任何物品能消除静默。净化剂能清除**感染**，但静默不是感染，
 * 它是**缺席**——你不能把「没有」清理掉。
 *
 * <p>这是全篇唯一一次真正不可逆的**世界**改变（记忆焚烧不可逆，但只影响一个人）。
 *
 * <h2>启动的演出</h2>
 *
 * <p>刻意**没有**爆炸、没有闪光、没有胜利音效。只有一声很低的闷响，
 * 然后世界安静下来。玩家要能在那一刻**听出**声音消失了——
 * 如果配上一段华丽的音效，他反而听不到那份安静。
 */
public class ApparatusCoreBlock extends Block {

    public ApparatusCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        SilenceData silence = SilenceData.get(serverLevel);
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // 已经静默的地方不需要第二台——但玩家可能就想再放一台。
        // 这里允许叠加（静默区本来就重叠），只是不重复计数。
        List<Long> added = silence.silenceArea(serverLevel, chunkX, chunkZ,
            SilenceData.ACTIVATION_RADIUS);

        // 核心本身被烧掉：它不是可重复使用的机器，它是一次性的完成。
        serverLevel.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

        // 一声很低的闷响，然后安静。
        // 只播这一次，且刻意用低音量——它应该像某个东西**停止**了，
        // 而不是像某个东西启动了。
        serverLevel.playSound(null, pos, ModSounds.BOSS_DEATH.get(),
            SoundSource.BLOCKS, 0.55f, 0.45f);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
            24, 0.8, 0.6, 0.8, 0.01);

        // 反馈只说结果，不评价。不说「你成功了」，也不说「你做了什么」。
        //
        // added 为空意味着这片地面本来就静着（玩家在旧静默区里又放了一台）。
        // 那不能报「0 个区块安静了」——那读起来像失败，而它其实什么都没发生。
        serverPlayer.displayClientMessage(
            Component.translatable(added.isEmpty()
                    ? "block.echoesofoblivion.apparatus_core.already"
                    : "block.echoesofoblivion.apparatus_core.activated",
                added.size())
                .withStyle(ChatFormatting.DARK_GRAY), false);
        // 第二句才是重点，但它仍然**不解释**。玩家自己会想到那个文明。
        serverPlayer.displayClientMessage(
            Component.translatable("block.echoesofoblivion.apparatus_core.done")
                .withStyle(ChatFormatting.GRAY), false);

        silence.broadcastIncrement(serverLevel, added);

        return InteractionResult.CONSUME;
    }
}
