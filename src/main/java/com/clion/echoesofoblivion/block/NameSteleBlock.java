package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.block.entity.NameSteleBlockEntity;
import com.clion.echoesofoblivion.item.ModItems;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 名字碑（v2.3.0）——**让玩家自己决定要不要把自己写上去，然后承担结果**。
 *
 * <h2>为什么不推翻「碑必须空白」</h2>
 *
 * <p>{@code docs/ideas-backlog.md} 明确拒绝「给文明或个人命名」，理由是
 * 名字会削弱「被替换」的恐怖。这一版**不推翻它**，正好相反：
 *
 * <ul>
 *   <li>碑本身是空白的（世界生成放下的就是空碑）。</li>
 *   <li>玩家刻上的是**自己的名字**，不是这个文明的名字。</li>
 *   <li>刻了之后**看不到**——任何渲染路径都不显示它。</li>
 * </ul>
 *
 * <p>所以「名字消失」这件事没有被削弱，它被**转移到了玩家身上**。
 * 原来的恐怖是「他们的名字没了」，现在是「你的名字也进去了」。
 *
 * <h2>为什么需要一块空白铭牌</h2>
 *
 * <p>{@code blank_plaque} 在此之前是可合成但无功能的物品（技术债 §9.x）。
 * 这一版给它功能：手持空白铭牌右键空碑即可刻名，铭牌被消耗。
 *
 * <p>这个材料选择不是随意的——铭牌本来就「什么都没有写」，
 * 而玩家用一块空白的牌子换来了一个看不见的名字。
 */
public class NameSteleBlock extends Block implements EntityBlock {

    public NameSteleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NameSteleBlockEntity(pos, state);
    }

    /**
     * 手持空白铭牌右键空碑 → 刻名。
     *
     * <p>返回类型是 {@code ItemInteractionResult} 而不是 {@code InteractionResult}——
     * 1.20.5+ 把「带着物品右键方块」拆成了独立的方法与返回值类型。
     * 用错类型会编译失败（这正是 v2.3.0 踩到的一次）。
     *
     * <p>{@code PASS_TO_DEFAULT_BLOCK_INTERACTION} 表示「我不管，交给默认处理」——
     * 拿着别的东西右键时不该拦住玩家。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModItems.BLANK_PLAQUE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof NameSteleBlockEntity stele)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stele.isInscribed()) {
            // 碑不接受改写，也不告诉你原来是谁——因为「原来是谁」已经不重要了
            serverPlayer.displayClientMessage(
                Component.translatable("block.echoesofoblivion.name_stele.taken")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            return ItemInteractionResult.CONSUME;
        }

        if (!stele.inscribe(serverPlayer.getUUID(), serverPlayer.getName().getString(),
                serverLevel.getGameTime())) {
            return ItemInteractionResult.CONSUME;
        }

        // 代价从这里开始生效。标记在**玩家**身上而不是方块上——
        // 因此砸掉碑不会撤销代价（见类注释与 NameSteleObservation）。
        com.clion.echoesofoblivion.server.NameSteleObservation.markInscribed(serverPlayer);

        // 消耗一块空白铭牌
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // 演出：刻刀落下的声音，以及一点石粉。
        // 刻意**没有**任何「成就达成」式的音效或粒子爆发——
        // 这件事不该有正反馈。玩家付出了一块铭牌，得到的只是一声凿击。
        serverLevel.playSound(null, pos, ModSounds.BOSS_HIT.get(),
            SoundSource.BLOCKS, 0.7f, 1.8f);
        serverLevel.sendParticles(ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            18, 0.25, 0.35, 0.25, 0.05);

        // 反馈只说「刻好了」，不说刻了什么、也不说会有什么后果。
        // 后果必须由玩家自己发现（见 NameSteleObservation）。
        serverPlayer.displayClientMessage(
            Component.translatable("block.echoesofoblivion.name_stele.inscribed")
                .withStyle(ChatFormatting.GRAY), false);

        return ItemInteractionResult.CONSUME;
    }

    /**
     * 破坏名字碑：**刻名不可撤回**。
     *
     * <p>碑可以被打掉，但 {@code NameSteleObservation} 的代价判定查的是
     * **玩家自己的持久化标记**，不是世界里还有没有那块碑。
     * 所以砸掉碑不会让你脱身——你写上去的东西不在这里，在你身上。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
    }
}
