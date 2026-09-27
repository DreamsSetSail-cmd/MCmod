package com.clion.echoesofoblivion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 稳定剂（v2.0.0）——**注射后短时间内不被世界推开**。
 *
 * <h2>它解决什么问题</h2>
 *
 * <p>感染区块里最难受的不是亡魂，是重力闪烁：你正在搭桥、正在跑，
 * 突然被向上推 0.45 或向下压 0.35。它不致命，但它让你**无法计划**，
 * 而模组里所有需要耐心的动作（读残片、找水晶、打 Boss）都被它打断。
 *
 * <p>稳定剂给 90 秒的免疫。它不治疗、不保护、不降侵蚀，只是让物理规则
 * 在这 90 秒里保持不变——这是「研究者的做法」：不试图消灭异常，只是让自己
 * 在异常里还能做完手上的事。
 *
 * <h2>为什么不做成永久</h2>
 *
 * <p>永久免疫会把感染区从威胁降级成背景。90 秒是一个**作业窗口**：
 * 足够你穿过一片感染区，不够你把它当成家。
 */
public class StabilizerItem extends Item {

    /** 免疫持续时间（tick）。90 秒。 */
    private static final int DURATION_TICKS = 1800;

    /**
     * 生效中的玩家 → 失效时的游戏刻。
     *
     * <p>用游戏刻而不是墙钟：暂停、卡顿、服务器重启后都以世界时间为准。
     * 键用 UUID 而非玩家实例，避免实例在维度切换后被替换导致的失效。
     */
    private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();

    public StabilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        ACTIVE.put(player.getUUID(), serverLevel.getGameTime() + DURATION_TICKS);

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW,
            SoundSource.PLAYERS, 0.7f, 1.6f);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
            player.getX(), player.getY() + 1.0, player.getZ(),
            40, 0.6, 0.9, 0.6, 0.02);

        serverPlayer.displayClientMessage(
            Component.translatable("item.echoesofoblivion.stabilizer.applied", DURATION_TICKS / 20)
                .withStyle(ChatFormatting.AQUA), true);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 玩家此刻是否受稳定剂保护。
     *
     * <p>由 {@code MirrorChunkHandler} 在施加重力闪烁前查询。
     * 顺带做惰性清理：过期的条目在查询时移除，不需要额外的 tick 任务。
     */
    public static boolean isStabilized(Player player, long gameTime) {
        Long expiry = ACTIVE.get(player.getUUID());
        if (expiry == null) {
            return false;
        }
        if (gameTime >= expiry) {
            ACTIVE.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /** 玩家退出时清理，避免长跑服务器的 Map 缓慢增长。 */
    public static void clear(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.stabilizer.desc",
                DURATION_TICKS / 20)
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
