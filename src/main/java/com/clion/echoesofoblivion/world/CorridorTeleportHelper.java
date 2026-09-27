package com.clion.echoesofoblivion.world;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.PortalTravelPacket;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 双维度传送逻辑（阶段 2）。
 *
 * <p>要点：
 * <ul>
 *   <li>主世界 ↔ 寂静走廊双向传送，走廊的 {@code coordinate_scale} 为 2.0，
 *       因此坐标需要按比例换算，而不是直接用同一个 x/z。</li>
 *   <li>落点带随机偏移（不稳定感），并保证脚下有实体方块，避免掉进虚空。</li>
 *   <li>返回主世界时会在落点周围补一小块黑石平台，防止出现在半空或方块里。</li>
 * </ul>
 */
public final class CorridorTeleportHelper {

    /** 落点随机偏移范围（格）。 */
    private static final int SCATTER = 12;

    private CorridorTeleportHelper() {
    }

    /** 供「走廊之钥」点火使用：在黑曜石框架内填充传送门方块。 */
    public static boolean fillPortal(ServerLevel level, BlockPos origin, Direction facing) {
        boolean placedAny = false;
        // 在 5x5（水平）x 4（垂直）范围内填充，形成门面
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos target = origin.offset(dx, dy, dz);
                    BlockState existing = level.getBlockState(target);
                    if (!existing.isAir()) {
                        continue;
                    }
                    level.setBlockAndUpdate(target, ModBlocks.UNSTABLE_PORTAL.get()
                        .defaultBlockState()
                        .setValue(com.clion.echoesofoblivion.block.UnstablePortalBlock.FACING, facing));
                    placedAny = true;
                }
            }
        }
        if (placedAny) {
            level.playSound(null, origin, ModSounds.PORTAL_OPEN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return placedAny;
    }

    /** 执行传送。{@code entering} 为 true 表示主世界 → 走廊。 */
    public static void travel(ServerPlayer player, boolean entering) {
        ResourceKey<Level> destination = entering
            ? ModWorldGen.SILENT_CORRIDOR
            : Level.OVERWORLD;

        ServerLevel target = player.getServer().getLevel(destination);
        if (target == null) {
            EchoesOfOblivionMod.LOGGER.warn("无法找到目标维度 {}，传送取消", destination.location());
            return;
        }

        BlockPos landing = entering
            ? findCorridorLanding(player, target)
            : findOverworldLanding(player, target);

        player.teleportTo(target, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
            player.getYRot(), player.getXRot());

        // 通知客户端做视野压制
        ModNetwork.sendToPlayer(new PortalTravelPacket(entering, true), player);
        target.playSound(null, landing, ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    /** 走廊落点：走廊是超平坦地形，直接把玩家放在地表之上并铺落脚点。 */
    private static BlockPos findCorridorLanding(ServerPlayer player, ServerLevel corridor) {
        BlockPos base = scaleForCorridor(player.blockPosition());
        int x = base.getX() + scatter(player);
        int z = base.getZ() + scatter(player);

        int surface = corridor.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos landing = new BlockPos(x, Math.max(corridor.getMinBuildHeight() + 2, surface), z);
        buildLandingPad(corridor, landing);
        return landing;
    }

    /** 主世界落点：按比例还原坐标，并确保落点上方有两格空间。 */
    private static BlockPos findOverworldLanding(ServerPlayer player, ServerLevel overworld) {
        BlockPos current = player.blockPosition();
        int x = current.getX() * 2 + scatter(player) * 2;
        int z = current.getZ() * 2 + scatter(player) * 2;

        // 主世界用世界表面定位
        BlockPos surface = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos(x, overworld.getMinBuildHeight(), z));
        BlockPos landing = surface.above();
        buildLandingPad(overworld, landing);
        return landing;
    }

    /** 走廊的 coordinate_scale = 2.0，主世界坐标除以 2 得到走廊坐标。 */
    private static BlockPos scaleForCorridor(BlockPos overworldPos) {
        return new BlockPos(overworldPos.getX() / 2, overworldPos.getY(), overworldPos.getZ() / 2);
    }

    private static int scatter(ServerPlayer player) {
        return player.getRandom().nextInt(-SCATTER, SCATTER + 1);
    }

    /** 在落点下方与四周补一小块平台，避免玩家悬空或被埋。 */
    private static void buildLandingPad(ServerLevel level, BlockPos landing) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = landing.offset(dx, -1, dz);
                if (level.getBlockState(floor).isAir()) {
                    level.setBlockAndUpdate(floor, Blocks.BLACKSTONE.defaultBlockState());
                }
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos clear = landing.offset(dx, dy, dz);
                    if (!level.getBlockState(clear).isAir()) {
                        level.setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
}
