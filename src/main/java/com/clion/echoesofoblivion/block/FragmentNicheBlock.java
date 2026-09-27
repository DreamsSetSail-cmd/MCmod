package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.item.ModItems;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 残片龛（v2.0.0）——**12 段残片在世界里的唯一来源**。
 *
 * <h2>为什么必须有这个方块</h2>
 *
 * <p>残片刻意没有合成配方（见 {@code LoreFragmentItem} 的类注释：
 * 给配方会把考古变成合成）。但「没有配方」必须同时给出**另一条获取途径**，
 * 否则这 12 件物品在生存模式下完全不可得，整套叙事就是死的。
 *
 * <p>所以世界生成会在遗迹与纪念碑里放一个残片龛。它是一个**空的凹槽**——
 * 不是箱子、不是书架，就是一个明显「本来是放东西的地方」。
 * 玩家右键它，取出里面那一段。
 *
 * <h2>它给的是哪一段：确定性，而不是随机</h2>
 *
 * <p>如果用随机，同一个玩家会反复拿到已经读过的段落，而永远缺某一段——
 * 这在有 12 段的集合上是必然的（赠券收集者问题），体验会很糟。
 *
 * <p>因此残片龛按**玩家自己还没拿到的索引顺序**发放：
 * 第一个龛给 0，第二个给 1……直到 12 段都拿齐。
 * 这样「顺序不可控」依然成立——**找到哪个龛是不可控的**，
 * 而玩家不会因为运气差而永远读不完。
 *
 * <h2>它不可被破坏回收</h2>
 *
 * <p>破坏它只掉一个空白的铭牌。龛本身是一次性的，
 * 与「这些东西的位置是固定的历史事实」一致。
 */
public class FragmentNicheBlock extends Block {

    public FragmentNicheBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                              Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack fragment = takeNextFragment(serverPlayer);
        if (fragment.isEmpty()) {
            // 12 段都拿过了——这个龛对你已经没有东西可给
            serverPlayer.displayClientMessage(
                Component.translatable("block.echoesofoblivion.fragment_niche.empty")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            return InteractionResult.CONSUME;
        }

        player.getInventory().placeItemBackInInventory(fragment);

        // 演出：从凹槽里升起来的粒子，以及一声很轻的共鸣
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            30, 0.3, 0.4, 0.3, 0.02);
        serverLevel.playSound(null, pos, ModSounds.MEMORY_COLLECT.get(),
            SoundSource.BLOCKS, 0.9f, 1.3f);

        serverPlayer.displayClientMessage(
            Component.translatable("block.echoesofoblivion.fragment_niche.taken")
                .withStyle(ChatFormatting.AQUA), false);

        // 一次性：取走后变成空槽（用空气表示「这里本来有东西」）
        serverLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            12, 0.2, 0.2, 0.2, 0.01);

        return InteractionResult.CONSUME;
    }

    /**
     * 按玩家尚未持有的索引顺序发放下一段残片。
     *
     * <p>「尚未持有」用**背包实际内容**判定，而不是进度存档：
     * 玩家可能把残片丢掉了，那他就应该能再拿到一段。
     * 用进度判定会让丢掉的段落永久不可恢复。
     *
     * @return 一段残片；全部拿齐时返回空
     */
    private static ItemStack takeNextFragment(ServerPlayer player) {
        List<ItemStack> carried = player.getInventory().items;
        for (var entry : ModItems.fragments().entrySet()) {
            var item = entry.getValue().get();
            boolean has = carried.stream().anyMatch(s -> s.is(item))
                || player.getOffhandItem().is(item);
            if (!has) {
                return new ItemStack(item, 1);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 破坏它不给任何掉落——{@code noLootTable()} 在 ModBlocks 里声明。 */
}
