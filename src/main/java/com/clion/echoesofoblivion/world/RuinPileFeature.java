package com.clion.echoesofoblivion.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 走廊废墟（阶段 2 第 4 条）。
 *
 * <p>在寂静走廊里生成一个「倒塌建筑残骸」：一圈不完整的低矮墙基、几块散落的碎砖，
 * 以及偶尔残留的一根立柱。选择用地物（Feature）而不是 {@code Structure} 实现，
 * 是因为废墟只需要「随机散布的破碎感」，不需要结构模板（NBT）与结构集，
 * 这样既符合视觉目标，也避免了结构生成那一整套高风险的配置。
 */
public class RuinPileFeature extends Feature<NoneFeatureConfiguration> {

    /** 废墟最大半径。 */
    private static final int MAX_RADIUS = 5;

    public RuinPileFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // 用高度图重新定位到地表，避免悬空或埋进地里
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin);

        int radius = 2 + random.nextInt(MAX_RADIUS - 1);
        boolean placedAny = false;

        // 1. 墙基：周长上按概率保留方块，形成「断墙」
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                boolean onWall = distance > radius - 1.2 && distance <= radius + 0.2;
                if (!onWall) {
                    continue;
                }
                // 墙体本应连续，这里刻意制造缺口
                if (random.nextFloat() < 0.35f) {
                    continue;
                }
                int height = 1 + random.nextInt(3);
                for (int dy = 0; dy < height; dy++) {
                    if (random.nextFloat() < 0.25f) {
                        continue;
                    }
                    BlockPos pos = surface.offset(dx, dy, dz);
                    if (isReplaceable(level, pos)) {
                        setBlock(level, pos, wallBlock(random));
                        placedAny = true;
                    }
                }
            }
        }

        // 2. 内部散落碎砖
        int debris = 4 + random.nextInt(10);
        for (int i = 0; i < debris; i++) {
            BlockPos pos = surface.offset(
                random.nextInt(-radius, radius + 1), 0, random.nextInt(-radius, radius + 1));
            if (isReplaceable(level, pos)) {
                setBlock(level, pos, debrisBlock(random));
                placedAny = true;
            }
        }

        // 3. 偶尔保留一根立柱：一根直立的方块柱，顶端可能已经断了
        if (random.nextFloat() < 0.4f) {
            int pillarHeight = 2 + random.nextInt(4);
            BlockPos pillarBase = surface.offset(
                random.nextInt(-radius, radius + 1), 0, random.nextInt(-radius, radius + 1));
            for (int dy = 0; dy < pillarHeight; dy++) {
                BlockPos pos = pillarBase.above(dy);
                if (!isReplaceable(level, pos)) {
                    break;
                }
                setBlock(level, pos, wallBlock(random));
                placedAny = true;
            }
        }

        return placedAny;
    }

    /** 只替换空气与可替换方块，避免削掉地形。 */
    private static boolean isReplaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** 墙体材料：以深板岩与黑石为主，偶尔混入裂纹砖。 */
    private static BlockState wallBlock(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.45f) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 0.70f) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 0.90f) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    /** 碎砖材料。 */
    private static BlockState debrisBlock(RandomSource random) {
        return random.nextFloat() < 0.5f
            ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
            : Blocks.BLACKSTONE.defaultBlockState();
    }
}
