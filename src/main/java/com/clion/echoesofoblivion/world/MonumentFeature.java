package com.clion.echoesofoblivion.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 可读的遗迹（v1.1.0 环境叙事）。
 *
 * <p>旧的 {@link RuinPileFeature} 只生成「随机碎石」，看起来像地质现象。
 * 有轴线的残骸看起来才像**曾经的意图**。这个地物生成三种能被辨认出用途的结构：
 *
 * <ul>
 *   <li>{@code BRIDGE} —— 两根立柱 + 之间缺失的横梁。读者会自己补上那根梁，
 *       而「缺失」正是叙事本身：他们的桥是靠交出安静换来的。</li>
 *   <li>{@code APPARATUS} —— 圆台基座 + 中心空槽。那台仪器的位置。
 *       空槽是刻意的：仪器已经不在了，或者说它完成了工作。</li>
 *   <li>{@code MONUMENT} —— 一排竖直方块，**上面什么都不写**。
 *       这是全篇最重要的道具：一个连名字都被替换掉的文明，
 *       用写满名字的碑来纪念它是没有意义的。空白碑比任何铭文都更符合设定。</li>
 * </ul>
 *
 * <p>三种遗迹共用一套材质与生成框架，因此做成一个可配置的 Feature，
 * 由数据包 JSON 决定生成哪一种，而不是写三个类。
 */
public class MonumentFeature extends Feature<MonumentFeature.Config> {

    /** 遗迹的形态。 */
    public enum Kind {
        BRIDGE,
        APPARATUS,
        MONUMENT
    }

    /**
     * 配置。
     *
     * <p>只有 {@code kind} 是必填的；材质留空时使用默认的深板岩/黑石组合，
     * 这样数据包只需要写一行 {@code {"kind": "monument"}} 就能用。
     */
    public record Config(Kind kind) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(
                name -> {
                    for (Kind k : Kind.values()) {
                        if (k.name().equalsIgnoreCase(name)) {
                            return k;
                        }
                    }
                    // 未知名称退化到 MONUMENT：它是三种里最低调的一种，
                    // 出错时不会在世界上留下突兀的结构。
                    return Kind.MONUMENT;
                },
                Kind::name
            ).fieldOf("kind").forGetter(Config::kind)
        ).apply(instance, Config::new));
    }

    // ---------------------------------------------------------------- 材质

    private static BlockState structure() {
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static BlockState structureCracked() {
        return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static BlockState accent() {
        return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    private static BlockState base() {
        return Blocks.BLACKSTONE.defaultBlockState();
    }

    public MonumentFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, context.origin());

        return switch (context.config().kind()) {
            case BRIDGE -> placeBridge(level, random, surface);
            case APPARATUS -> placeApparatus(level, random, surface);
            case MONUMENT -> placeMonument(level, random, surface);
        };
    }

    // ---------------------------------------------------------------- 桥墩

    /**
     * 两根立柱，间距 5~7 格，顶端**没有横梁**。
     *
     * <p>柱高刻意不一致（一端更高），让「断掉」这件事看起来发生过。
     */
    private boolean placeBridge(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int span = 5 + random.nextInt(3);
        int leftHeight = 4 + random.nextInt(3);
        int rightHeight = Math.max(2, leftHeight - 1 - random.nextInt(3));
        boolean placed = false;

        placed |= pillar(level, origin, leftHeight, true);
        placed |= pillar(level, origin.offset(span, 0, 0), rightHeight, false);

        // 桥墩基座：各铺一块 3x3 的石台，暗示这里曾有承重结构
        placed |= pad(level, origin.below(), 1);
        placed |= pad(level, origin.offset(span, -1, 0), 1);

        return placed;
    }

    /** 一根立柱：3x3 的方形柱，顶端可能缺一块。 */
    private boolean pillar(WorldGenLevel level, BlockPos basePos, int height, boolean topped) {
        boolean placed = false;
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos pos = basePos.offset(dx, dy, dz);
                    if (!isReplaceable(level, pos)) {
                        continue;
                    }
                    // 表面用裂纹砖，内部用完整砖，做出风化的层次
                    boolean outer = dy == height - 1 || dx == 0 || dz == 0;
                    level.setBlock(pos, outer ? structureCracked() : structure(), 2);
                    placed = true;
                }
            }
        }
        if (topped) {
            // 完整的一端放一块较亮的石材，作为「曾有横梁搭在这里」的接口
            BlockPos cap = basePos.above(height).offset(0, 0, 0);
            if (isReplaceable(level, cap)) {
                level.setBlock(cap, accent(), 2);
                placed = true;
            }
        }
        return placed;
    }

    // ---------------------------------------------------------------- 仪器基座

    /**
     * 圆台基座 + 中心空槽。
     *
     * <p>空槽用破坏方块（空气）明确表达「这里曾经放着东西」。
     * 半径 2 的圆台，中心挖空一格。
     */
    private boolean placeApparatus(WorldGenLevel level, RandomSource random, BlockPos origin) {
        boolean placed = false;
        int radius = 2;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 0.4) {
                    continue;
                }
                BlockPos pos = origin.offset(dx, 0, dz);

                // 中心：挖空，露出下面的地层，形成「槽」
                if (distance < 0.6) {
                    BlockPos below = pos.below();
                    if (!level.getBlockState(below).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        level.setBlock(below, accent(), 2);
                        placed = true;
                    }
                    continue;
                }

                if (!isReplaceable(level, pos)) {
                    continue;
                }
                // 外圈用完整砖，内圈用裂纹砖
                level.setBlock(pos, distance > radius - 0.8 ? base() : structureCracked(), 2);
                placed = true;

                // 圆台抬高一层：边缘加一圈矮墙
                if (distance > radius - 0.8 && random.nextFloat() < 0.6f) {
                    BlockPos rim = pos.above();
                    if (isReplaceable(level, rim)) {
                        level.setBlock(rim, structure(), 2);
                        placed = true;
                    }
                }
            }
        }

        // 三根断裂的支撑柱，随机高度 0~2，暗示上方曾有东西
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.round(Math.cos(angle) * (radius + 1));
            int z = (int) Math.round(Math.sin(angle) * (radius + 1));
            int h = random.nextInt(3);
            for (int dy = 0; dy < h; dy++) {
                BlockPos pos = origin.offset(x, dy + 1, z);
                if (isReplaceable(level, pos)) {
                    level.setBlock(pos, structureCracked(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    // ---------------------------------------------------------------- 名字碑

    /**
     * 一排竖直方块，**上面什么都不写**。
     *
     * <p>这是全篇最重要的道具。设计上刻意做成「读不出内容但明显是给人看的」：
     * 竖立的碑面、整齐的排列、统一的材质——唯独没有信息。
     *
     * <p>技术上是 Minecraft 的限制：方块无法承载任意文字。
     * 但这个限制恰好与设定完美吻合，所以不做告示牌、不做成书，
     * **就让它是空白的**。
     */
    private boolean placeMonument(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int count = 3 + random.nextInt(3);
        boolean placed = false;

        for (int i = 0; i < count; i++) {
            BlockPos pos = origin.offset(i * 2, 0, 0);
            int height = 2 + random.nextInt(2);

            // 基座
            if (isReplaceable(level, pos)) {
                level.setBlock(pos, base(), 2);
                placed = true;
            }
            // 碑身：正面用较亮的抛光砖，模拟被打磨过的碑面
            for (int dy = 1; dy <= height; dy++) {
                BlockPos body = pos.above(dy);
                if (!isReplaceable(level, body)) {
                    break;
                }
                level.setBlock(body, accent(), 2);
                placed = true;
            }
            // 少数碑立得更高，但仍然空的——高度不带来内容
            if (random.nextFloat() < 0.3f) {
                BlockPos cap = pos.above(height + 1);
                if (isReplaceable(level, cap)) {
                    level.setBlock(cap, structure(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    // ---------------------------------------------------------------- 工具

    /** 只替换空气与可替换方块，避免削掉地形。 */
    private static boolean isReplaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** 在中心周围铺一块正方形石台。 */
    private static boolean pad(WorldGenLevel level, BlockPos center, int radius) {
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (isReplaceable(level, pos)) {
                    level.setBlock(pos, base(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
