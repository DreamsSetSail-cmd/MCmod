package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.block.UnstablePortalBlock;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 走廊之钥（阶段 2）。
 *
 * <p>用法：对着「黑曜石 / 哭泣的黑曜石」搭建的 4 宽 5 高门框内侧的空位右键，
 * 门框校验通过后填充不稳定传送门方块。校验是必须的——否则这把钥匙就变成了
 * 「在任何地方凭空造门」的作弊道具。
 */
public class CorridorKeyItem extends Item {

    /** 门框内部门洞尺寸（宽 2、高 3），与原版下界传送门一致。 */
    private static final int PORTAL_WIDTH = 2;
    private static final int PORTAL_HEIGHT = 3;

    public CorridorKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        List<BlockPos> interior = findPortalInterior(serverLevel, clicked);
        if (interior.isEmpty()) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                    Component.translatable("item.echoesofoblivion.corridor_key.invalid_frame"), true);
            }
            return InteractionResult.FAIL;
        }

        for (BlockPos pos : interior) {
            serverLevel.setBlockAndUpdate(pos, ModBlocks.UNSTABLE_PORTAL.get().defaultBlockState()
                .setValue(UnstablePortalBlock.FACING, Direction.NORTH));
        }
        serverLevel.playSound(null, clicked, ModSounds.PORTAL_OPEN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * 在点击位置附近寻找可用的门洞。
     *
     * <p>以点击点为中心，在三个轴向上尝试所有可能的门洞原点，只要内部门洞全为空气、
     * 且四周框架全部为黑曜石/哭泣的黑曜石，就认为门框合法。
     */
    private static List<BlockPos> findPortalInterior(ServerLevel level, BlockPos clicked) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            for (int offsetX = -PORTAL_WIDTH; offsetX <= 1; offsetX++) {
                for (int offsetY = -PORTAL_HEIGHT + 1; offsetY <= 1; offsetY++) {
                    BlockPos origin = clicked.offset(
                        axis == Direction.Axis.X ? offsetX : 0,
                        offsetY,
                        axis == Direction.Axis.Z ? offsetX : 0);
                    List<BlockPos> interior = validateFrame(level, origin, axis);
                    if (!interior.isEmpty()) {
                        return interior;
                    }
                }
            }
        }
        return List.of();
    }

    /** 校验以 {@code origin} 为左下角、沿 {@code axis} 展开的门洞与其框架。 */
    private static List<BlockPos> validateFrame(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        List<BlockPos> interior = new ArrayList<>(PORTAL_WIDTH * PORTAL_HEIGHT);

        for (int w = 0; w < PORTAL_WIDTH; w++) {
            for (int h = 0; h < PORTAL_HEIGHT; h++) {
                BlockPos pos = step(origin, axis, w).above(h);
                BlockState state = level.getBlockState(pos);
                // 门洞内部必须可替换
                if (!state.isAir() && !state.canBeReplaced()) {
                    return List.of();
                }
                interior.add(pos);
            }
        }

        // 框架：门洞四周一圈
        for (int w = -1; w <= PORTAL_WIDTH; w++) {
            for (int h = -1; h <= PORTAL_HEIGHT; h++) {
                boolean onFrameEdge = w == -1 || w == PORTAL_WIDTH || h == -1 || h == PORTAL_HEIGHT;
                if (!onFrameEdge) {
                    continue;
                }
                BlockPos frame = step(origin, axis, w).above(h);
                if (!isValidFrameBlock(level.getBlockState(frame))) {
                    return List.of();
                }
            }
        }
        return interior;
    }

    private static BlockPos step(BlockPos origin, Direction.Axis axis, int amount) {
        return axis == Direction.Axis.X ? origin.east(amount) : origin.south(amount);
    }

    private static boolean isValidFrameBlock(BlockState state) {
        return state.is(Blocks.OBSIDIAN)
            || state.is(Blocks.CRYING_OBSIDIAN)
            || state.is(Blocks.AMETHYST_BLOCK);
    }
}
