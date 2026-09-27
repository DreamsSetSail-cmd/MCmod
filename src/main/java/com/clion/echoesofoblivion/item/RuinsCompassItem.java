package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.world.ModWorldGen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 遗迹罗盘（v2.0.0）——指向最近的记忆水晶。
 *
 * <h2>为什么需要它</h2>
 *
 * <p>走廊是超平坦且没有地标的，而记忆水晶的散布半径很大。没有罗盘时，
 * 玩家在雾里（侵蚀越高雾越近）纯靠运气找水晶，探索会退化成乱走。
 * 这件工具把「找」变成一个有方向的动作，同时**不给出距离精确值**——
 * 只说方向与远近的模糊词，保持走廊的不确定感。
 *
 * <h2>实现</h2>
 *
 * <p>以玩家为中心做**螺旋式区块扫描**（近处优先），找到第一个记忆水晶方块即停止。
 * 扫描上限 24 区块半径，避免一次查询卡住服务器 tick。
 * 结果在聊天栏给出一条方位提示，并播放一声很轻的水晶共鸣作为「回应」。
 */
public class RuinsCompassItem extends Item {

    /** 扫描的区块半径上限。24 区块 = 384 格。 */
    private static final int MAX_CHUNK_RADIUS = 24;

    /** 每个区块内检查的方块步长（越小越精确、越慢）。 */
    private static final int STEP = 4;

    public RuinsCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos target = findNearestCrystal(serverLevel, player.blockPosition());
        if (target == null) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.ruins_compass.none")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.displayClientMessage(bearingMessage(serverPlayer, target), false);
        // 一声很轻的共鸣——「它回应了你」，但没有任何东西真的说话
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS, 0.35f, 1.5f);

        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(stack);
    }

    /** 螺旋扫描：近处优先，找到即返回。 */
    private BlockPos findNearestCrystal(ServerLevel level, BlockPos origin) {
        int centerChunkX = origin.getX() >> 4;
        int centerChunkZ = origin.getZ() >> 4;

        // 环形逐层扫描，天然是「由近到远」的顺序
        for (int ring = 0; ring <= MAX_CHUNK_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // 只处理环上的区块，内部已在更小的 ring 里查过
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }
                    BlockPos found = scanChunk(level, chunkX, chunkZ);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /** 在单个区块里按步长找记忆水晶。 */
    private BlockPos scanChunk(ServerLevel level, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x += STEP) {
            for (int z = 0; z < 16; z += STEP) {
                for (int y = minY; y < maxY; y += STEP) {
                    cursor.set(baseX + x, y, baseZ + z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.MEMORY_CRYSTAL.get())) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 方位提示。
     *
     * <p>刻意只说八方位与模糊距离（近/中/远），不给精确坐标——
     * 精确数字会把走廊变成一张可计算的地图，而这与「不确定」的基调相冲突。
     */
    private Component bearingMessage(ServerPlayer player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        String direction = compassBearing(player, dx, dz);
        String proximity = distance < 48 ? "near" : (distance < 160 ? "mid" : "far");

        return Component.translatable("item.echoesofoblivion.ruins_compass.found",
            Component.translatable("item.echoesofoblivion.compass.direction." + direction),
            Component.translatable("item.echoesofoblivion.compass.distance." + proximity));
    }

    /** 相对玩家的八方位。 */
    private String compassBearing(ServerPlayer player, double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // 转到「以玩家朝向为正前方」的相对角
        double relative = angle - (player.getYRot() + 90.0);
        relative = ((relative % 360) + 360) % 360;

        // 8 个 45° 扇区，正前方为 north 位置（用相对方位命名，避免与真实方位混淆）
        String[] sectors = {"front", "front_right", "right", "back_right",
                           "back", "back_left", "left", "front_left"};
        int index = (int) Math.round(relative / 45.0) % 8;
        return sectors[index];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.ruins_compass.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** 判断某个坐标是否在走廊维度——罗盘只在走廊有意义。 */
    public static boolean isInCorridor(Level level) {
        return level.dimension().equals(ModWorldGen.SILENT_CORRIDOR);
    }

    /** 保留：扫描用的队列工具，供将来做「列出最近 N 个」时使用。 */
    @SuppressWarnings("unused")
    private static Deque<BlockPos> ringOrder(int radius) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        queue.add(new BlockPos(dx, 0, dz));
                    }
                }
            }
        }
        return queue;
    }
}
