package com.clion.echoesofoblivion.world;

import com.clion.echoesofoblivion.block.ModBlocks;
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
 * 走廊废墟（v2.1.0 重写：从「随机碎石」到「布局语法」）。
 *
 * <h2>为什么要重写</h2>
 *
 * <p>旧实现是噪点式的：周长上按概率保留方块形成断墙、内部均匀撒碎砖。
 * 它看起来像**地质现象**——岩石碎了，仅此而已。
 *
 * <p>新实现让废墟有**轴线**：墙基沿一条直线延伸、柱距相等、
 * 碎砖沿一条倒塌的线分布。能被辨认出「这里曾经有秩序」的废墟，
 * 比纯随机碎石更悲伤——因为**是秩序坏掉了，不是从来没有秩序**。
 * 这与本版的核心机制（介质腐坏）共享同一句话。
 *
 * <h2>布局语法的四条规则</h2>
 *
 * <ol>
 *   <li><b>主墙沿轴</b>：一条沿 X 或 Z 的墙，每格高 1~3。缺口不是逐格随机的，
 *       而是**成段**塌掉——真实倒塌会带走一整段，不会只掉一块。</li>
 *   <li><b>柱距相等</b>：主墙前每隔 {@value #PILLAR_SPACING} 格一根立柱，高 2~5。
 *       柱列会**突然中断**，中断处不放缓冲、不放残桩——断就是断。</li>
 *   <li><b>碎砖成线</b>：倒塌的横梁落在垂直于主墙的一条线上，离墙越远越稀。
 *       这替代了旧版「均匀撒开」——沿一条线撒会读成**一次事件**。</li>
 *   <li><b>残片龛在轴线端点</b>：它是这个建筑最后的遗物，
 *       因此放在主墙的一端，而不是废墟的几何中心。</li>
 * </ol>
 *
 * <h2>为什么用 Feature 而不是 Structure</h2>
 *
 * <p>沿用旧实现的判断：废墟只需要一段被生成出来的残骸，不需要结构模板（NBT）
 * 与结构集。轴向布局用几十行代码就能表达，而引入 Structure 会带来模板文件、
 * 结构集配置、以及一整套高风险的地形适配。
 */
public class RuinPileFeature extends Feature<NoneFeatureConfiguration> {

    /** 主墙长度范围（格）。 */
    private static final int WALL_MIN = 7;
    private static final int WALL_MAX = 15;

    /** 立柱间距。**相等是刻意的**——「间距相等」本身就是秩序的痕迹。 */
    private static final int PILLAR_SPACING = 3;

    /** 柱列中断前的最大柱数。 */
    private static final int PILLAR_LIMIT = 6;

    /** 立柱相对主墙的水平进深（格）。 */
    private static final int COLONNADE_DEPTH = 2;

    /** 碎砖带相对主墙的起始距离（格）。 */
    private static final int DEBRIS_OFFSET = 3;

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

        // 轴线：随机选 X 或 Z。整座建筑的秩序都建立在这条轴上。
        boolean alongX = random.nextBoolean();
        int wallLength = WALL_MIN + random.nextInt(WALL_MAX - WALL_MIN + 1);

        boolean placedAny = false;
        placedAny |= placeMainWall(level, random, surface, alongX, wallLength);
        placedAny |= placeColonnade(level, random, surface, alongX, wallLength);
        placedAny |= placeCollapseLine(level, random, surface, alongX, wallLength);

        // 残片龛：立在主墙的一端（轴线起点）。
        if (isReplaceable(level, surface)) {
            setBlock(level, surface, ModBlocks.FRAGMENT_NICHE.get().defaultBlockState());
            placedAny = true;
        }

        return placedAny;
    }

    /**
     * 规则一：主墙沿轴延伸，缺口成段塌掉。
     *
     * <p>关键细节是**塌陷以段为单位**：一段 2~4 格全部缺失，而不是逐格掷骰。
     * 逐格随机会得到「毛毛的」墙，那看起来像噪声；成段缺失才像倒塌。
     */
    private boolean placeMainWall(WorldGenLevel level, RandomSource random,
                                  BlockPos surface, boolean alongX, int wallLength) {
        boolean placed = false;
        int x = 0;
        while (x < wallLength) {
            // 这一段的性质：塌掉 30% / 矮残墙 30% / 完整 40%
            float roll = random.nextFloat();
            int segment = 2 + random.nextInt(3);

            if (roll < 0.30f) {
                // 整段塌掉：什么都不放，直接跳过。断就是断。
                x += segment;
                continue;
            }

            boolean fullHeight = roll >= 0.70f;
            for (int i = 0; i < segment && x + i < wallLength; i++) {
                int height = fullHeight ? 2 + random.nextInt(2) : 1;
                for (int dy = 0; dy < height; dy++) {
                    BlockPos pos = offset(surface, alongX, x + i, dy, 0);
                    if (isReplaceable(level, pos)) {
                        setBlock(level, pos, wallBlock(random));
                        placed = true;
                    }
                }
            }
            x += segment;
        }
        return placed;
    }

    /**
     * 规则二：等距柱列，然后突然中断。
     *
     * <p>柱距恒定是这套语法里最重要的一件事：只有等距，玩家才能读出
     * 「这是设计出来的间距」，从而在柱列断掉时感到**少了一段**，
     * 而不是觉得柱子本来就少。
     */
    private boolean placeColonnade(WorldGenLevel level, RandomSource random,
                                   BlockPos surface, boolean alongX, int wallLength) {
        boolean placed = false;
        int pillars = 0;
        // 柱列比主墙短 1 格，避免与端点的残片龛重叠
        for (int x = 1; x < wallLength - 1; x += PILLAR_SPACING) {
            if (pillars >= PILLAR_LIMIT) {
                // 到此为止。不做渐矮、不放残桩——柱列就是断在这里。
                break;
            }
            int height = 2 + random.nextInt(4);
            // 三分之一概率顶端已经断了（少放最上面一块）
            if (random.nextFloat() < 0.33f) {
                height = Math.max(1, height - 1);
            }
            for (int dy = 0; dy < height; dy++) {
                BlockPos pos = offset(surface, alongX, x, dy, COLONNADE_DEPTH);
                if (isReplaceable(level, pos)) {
                    setBlock(level, pos, wallBlock(random));
                    placed = true;
                }
            }
            // 柱顶偶尔留一块较亮的石材，作为「曾有横梁搭在这里」的接口
            if (random.nextFloat() < 0.4f) {
                BlockPos cap = offset(surface, alongX, x, height, COLONNADE_DEPTH);
                if (isReplaceable(level, cap)) {
                    setBlock(level, cap, accent());
                    placed = true;
                }
            }
            pillars++;
        }
        return placed;
    }

    /**
     * 规则三：倒塌的横梁沿一条线落在主墙前方，越远越稀。
     *
     * <p>旧版把碎砖均匀撒在圆盘里，结果是「地面上一堆石头」。
     * 沿一条线撒会读成**一次事件**：有东西从上面掉下来，并且朝一个方向散开。
     */
    private boolean placeCollapseLine(WorldGenLevel level, RandomSource random,
                                      BlockPos surface, boolean alongX, int wallLength) {
        boolean placed = false;
        // 倒塌方向：垂直于主墙的一侧
        int direction = random.nextBoolean() ? 1 : -1;
        int lineStart = random.nextInt(Math.max(1, wallLength / 2));
        int lineLength = 3 + random.nextInt(5);

        for (int step = 0; step < lineLength; step++) {
            int along = lineStart + step;
            if (along >= wallLength) {
                break;
            }
            // 越远越稀：密度从 1.0 线性衰减到约 0.25
            float density = 1.0f - 0.75f * (step / (float) lineLength);
            if (random.nextFloat() > density) {
                continue;
            }
            int side = direction * (DEBRIS_OFFSET + step);
            BlockPos pos = offset(surface, alongX, along, 0, side);
            if (isReplaceable(level, pos)) {
                setBlock(level, pos, debrisBlock(random));
                placed = true;
            }
            // 横梁碎块可能滚到相邻格
            if (random.nextFloat() < 0.35f) {
                BlockPos drift = offset(surface, alongX, along + 1, 0, side);
                if (isReplaceable(level, drift)) {
                    setBlock(level, drift, debrisBlock(random));
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * 沿轴线偏移。
     *
     * @param along 沿主轴（{@code alongX} 时是 +X，否则是 +Z）的距离
     * @param dy    垂直高度
     * @param side  垂直于主轴的距离（正负决定在主墙的哪一侧）
     */
    private static BlockPos offset(BlockPos surface, boolean alongX, int along, int dy, int side) {
        return alongX
            ? surface.offset(along, dy, side)
            : surface.offset(side, dy, along);
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

    /** 柱顶接口用的较亮石材。 */
    private static BlockState accent() {
        return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }
}
