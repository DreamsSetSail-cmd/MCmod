package com.clion.echoesofoblivion.server;

import com.clion.echoesofoblivion.item.ModItems;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.network.packets.RenderStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 清点（v2.5.0）——**打完之后的三十秒，也是全篇最后一次反转**。
 *
 * <h2>Boss 战原来的结局</h2>
 *
 * <p>原来击败聚合体之后：一句「你赢了」、一件真理碎片、屏幕上消散的光。
 * 那是一个**奖励**，而这一版把奖励拿掉了，换成一次**结算**。
 *
 * <h2>它做什么</h2>
 *
 * <ol>
 *   <li>屏幕淡出（复用既有的 {@code RenderStatePacket.bossDefeat()}）。</li>
 *   <li>聊天栏**逐条**列出你烧掉的每一段记忆、以及你刻下的每一个名字。</li>
 *   <li>列完之后显示你**再也没有**的东西。</li>
 *   <li>**只有走完这段**，真理碎片才真正交到手上。</li>
 * </ol>
 *
 * <h2>为什么这不是惩罚</h2>
 *
 * <p>前几版让你一点点交出东西，但从不强调。这一版把账摊开放在你面前——
 * 而它**不扣任何东西**（记忆早就烧掉了，碑早就刻了）。
 *
 * <p>它只是**让你看见**。这是全篇唯一一次明确的清点，
 * 而它出现的时机是「你刚刚赢了」。
 *
 * <h2>为什么逐条列出而不是一次给完</h2>
 *
 * <p>一条一条地出现，玩家会一条一条地读。一次性刷屏会被当成噪声略过，
 * 而这一版要的是他**读到自己做过什么**。
 *
 * <p>条目之间的间隔刻意偏慢（{@value #ITEMS_PER_TICK} 秒一条）——
 * 快节奏会让它变成一个过场动画。
 */
public final class Reckoning {

    /** 每条之间的间隔（tick）。2 秒。 */
    private static final int LINE_INTERVAL = 40;

    /** 清点开始前的静默（tick）。4 秒——让淡出先完成。 */
    private static final int OPENING_PAUSE = 80;

    /** 收尾与发放之间的间隔（tick）。3 秒。 */
    private static final int CLOSING_PAUSE = 60;

    /** 烧掉的记忆索引，存在玩家持久化数据里。 */
    private static final String BURNED_KEY = "echoes_burned";

    /** 刻过名的碑的标记数量（只记数量，名字不进任何渲染）。 */
    private static final String INSCRIBED_COUNT_KEY = "echoes_inscribed_count";

    /** 进行中的清点：UUID → 剩余步骤队列。 */
    private static final Map<UUID, Deque<Step>> RUNNING = new ConcurrentHashMap<>();

    /** 一个待执行的步骤。 */
    private record Step(int delayTicks, Runnable action) {
    }

    private Reckoning() {
    }

    // ------------------------------------------------------------ 记录

    /** 记下「这段记忆被烧掉了」。由 {@code MemoryBurn} 调用。 */
    public static void recordBurn(ServerPlayer player, int memoryIndex) {
        var data = player.getPersistentData();
        int[] before = data.getIntArray(BURNED_KEY);
        for (int value : before) {
            if (value == memoryIndex) {
                return;
            }
        }
        int[] after = java.util.Arrays.copyOf(before, before.length + 1);
        after[before.length] = memoryIndex;
        data.putIntArray(BURNED_KEY, after);
    }

    /** 记下「又刻了一块碑」。由 {@code NameSteleBlock} 调用。 */
    public static void recordInscription(ServerPlayer player) {
        var data = player.getPersistentData();
        data.putInt(INSCRIBED_COUNT_KEY, data.getInt(INSCRIBED_COUNT_KEY) + 1);
    }

    /** 供测试与调试：这名玩家烧掉了几段记忆。 */
    public static int burnedCount(ServerPlayer player) {
        return player.getPersistentData().getIntArray(BURNED_KEY).length;
    }

    /** 供测试与调试：这名玩家刻了几块碑。 */
    public static int inscribedCount(ServerPlayer player) {
        return player.getPersistentData().getInt(INSCRIBED_COUNT_KEY);
    }

    // ------------------------------------------------------------ 执行

    /**
     * 开始清点。由 {@code BossCombat} 在终结演出**之后**调用，
     * 取代原本「立刻发放真理碎片」的那一步。
     */
    public static void begin(ServerPlayer player) {
        if (RUNNING.containsKey(player.getUUID())) {
            return;
        }
        ServerLevel level = player.serverLevel();

        // 1. 屏幕淡出。复用既有的终结表现包，不新增网络包。
        ModNetwork.sendToPlayer(RenderStatePacket.bossDefeat(), player);

        Deque<Step> steps = new ArrayDeque<>();
        steps.addLast(new Step(OPENING_PAUSE, () -> announceOpening(player)));

        // 2. 逐条列出烧掉的记忆
        int[] burned = player.getPersistentData().getIntArray(BURNED_KEY);
        if (burned.length == 0) {
            steps.addLast(new Step(LINE_INTERVAL, () -> player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.reckoning.burned_none")
                    .withStyle(ChatFormatting.GRAY), false)));
        } else {
            for (int index : burned) {
                MemoryEntry entry = MemoryRegistry.byIndex(index);
                String title = entry == null ? ("#" + index) : entry.title().getString();
                steps.addLast(new Step(LINE_INTERVAL, () -> player.displayClientMessage(
                    Component.translatable("message.echoesofoblivion.reckoning.burned", title)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false)));
            }
        }

        // 3. 逐条列出刻下的名字（只有数量，没有名字）
        int inscribed = player.getPersistentData().getInt(INSCRIBED_COUNT_KEY);
        if (inscribed == 0) {
            steps.addLast(new Step(LINE_INTERVAL, () -> player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.reckoning.inscribed_none")
                    .withStyle(ChatFormatting.GRAY), false)));
        } else {
            for (int i = 0; i < inscribed; i++) {
                steps.addLast(new Step(LINE_INTERVAL, () -> player.displayClientMessage(
                    Component.translatable("message.echoesofoblivion.reckoning.inscribed")
                        .withStyle(ChatFormatting.GRAY), false)));
            }
        }

        // 4. 收尾：你「再也没有」的东西
        steps.addLast(new Step(CLOSING_PAUSE, () -> {
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.reckoning.closing")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }));

        // 5. 只有走完这段，真理碎片才到手
        steps.addLast(new Step(CLOSING_PAUSE, () -> {
            player.getInventory().placeItemBackInInventory(
                new ItemStack(ModItems.SHARD_OF_TRUTH.get(), 1));
            player.displayClientMessage(
                Component.translatable("message.echoesofoblivion.reckoning.settled")
                    .withStyle(ChatFormatting.WHITE), false);
            // 世界的回应：一块比原来更多的空碑
            placeEmptyPlaque(level, player);
        }));

        RUNNING.put(player.getUUID(), steps);
    }

    /**
     * 推进一个正在进行的清点。由 {@code CorruptionEventHandler} 的每秒 tick 调用。
     *
     * <p>注意这里的延迟单位是「秒」而 {@link Step#delayTicks()} 是 tick：
     * 每秒 tick 会把 delay 减 20。用秒驱动是为了复用既有 tick，
     * 而不是新增一个每 tick 的监听器。
     */
    public static void tick(ServerPlayer player) {
        Deque<Step> steps = RUNNING.get(player.getUUID());
        if (steps == null) {
            return;
        }
        Step head = steps.pollFirst();
        if (head == null) {
            RUNNING.remove(player.getUUID());
            return;
        }
        int remaining = head.delayTicks() - 20;
        if (remaining > 0) {
            steps.addFirst(new Step(remaining, head.action()));
            return;
        }
        head.action().run();
        if (steps.isEmpty()) {
            RUNNING.remove(player.getUUID());
        }
    }

    /** 玩家退出时清理（正在进行中的清点会被中断，但数据已经持久化）。 */
    public static void forget(UUID playerId) {
        RUNNING.remove(playerId);
    }

    /** 这一段开场白：不安慰、不庆祝。 */
    private static void announceOpening(ServerPlayer player) {
        player.displayClientMessage(
            Component.translatable("message.echoesofoblivion.reckoning.opening")
                .withStyle(ChatFormatting.DARK_GRAY), false);
    }

    /**
     * 在玩家附近放一块空碑。
     *
     * <p>放在**离玩家两三格、且地面可替换**的位置——不放在脚下（会把玩家埋了），
     * 也不放在远处（玩家会错过它）。刻意**不播任何音效、不给任何提示**：
     * 它应当在你回头时**已经在那里**。
     */
    private static void placeEmptyPlaque(ServerLevel level, ServerPlayer player) {
        BlockPos base = player.blockPosition();
        // 依次尝试几个偏移，取第一个能放的位置。
        // 顺序是「正前方、右前、左前、右、左」——避免放在玩家背后。
        int[][] offsets = {{2, 0}, {2, 2}, {2, -2}, {0, 2}, {0, -2}};
        for (int[] offset : offsets) {
            BlockPos candidate = base.offset(offset[0], 0, offset[1]);
            // 找该列的地面
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos pos = candidate.offset(0, dy, 0);
                if (level.getBlockState(pos).canBeReplaced()
                    && !level.getBlockState(pos.below()).canBeReplaced()) {
                    level.setBlock(pos, com.clion.echoesofoblivion.block.ModBlocks.EMPTY_PLAQUE.get()
                        .defaultBlockState(), 3);
                    return;
                }
            }
            // 兜底：直接把脚下那一层替换成空碑底座
            if (level.getBlockState(candidate).canBeReplaced()) {
                level.setBlock(candidate, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
                level.setBlock(candidate.above(),
                    com.clion.echoesofoblivion.block.ModBlocks.EMPTY_PLAQUE.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    /** 供测试：清点的步骤延迟必须为正，否则序列会瞬间刷完。 */
    public static boolean parametersAreSane() {
        return LINE_INTERVAL > 0 && OPENING_PAUSE > 0 && CLOSING_PAUSE > 0;
    }

    /** 供测试：把一段记忆索引转成历史列表（不触碰世界）。 */
    public static List<Integer> burnedList(ServerPlayer player) {
        List<Integer> result = new ArrayList<>();
        for (int value : player.getPersistentData().getIntArray(BURNED_KEY)) {
            result.add(value);
        }
        return result;
    }

    /** 供测试：确认对某名玩家没有进行中的清点。 */
    public static boolean isRunning(UUID playerId) {
        return RUNNING.containsKey(playerId);
    }
}
