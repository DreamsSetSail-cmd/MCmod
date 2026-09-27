package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.memory.PlayerProgress;
import com.clion.echoesofoblivion.server.ServerPacketHandler;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 记忆之瓶（v2.0.0）——喝下它，污染会退，但**代价是一段记忆**。
 *
 * <h2>为什么它必须有代价</h2>
 *
 * <p>如果没有代价，这件物品就是一个纯粹的回复药，而模组的主题会被它稀释：
 * 这个世界里没有白拿的东西。所以它降低 25 点侵蚀值，
 * 同时**永久烧掉你最近见证的一段记忆**——不是遗忘，是消耗。
 *
 * <p>它和「记忆中焚烧」是两种不同的代价结构：
 * <ul>
 *   <li><b>焚烧</b>是玩家主动的、有选择的：你想换一个效果，代价由你挑。</li>
 *   <li><b>瓶</b>是被动的、被剥夺的：你只想活下去，代价由它挑（最近的那段）。</li>
 * </ul>
 *
 * <p>于是玩家会开始**囤积**——不是囤瓶，是囤「还没看的记忆」。
 * 这恰恰是设计要的效果：你开始盘算自己知道多少，而盘算本身就是消耗。
 *
 * <h2>没有记忆可燃时会怎样</h2>
 *
 * <p>它会照常生效。这不是仁慈，是更冷的一层：你已经在用不知道的东西付账了。
 */
public class MemoryVialItem extends Item {

    /** 喝一瓶降低的侵蚀值。 */
    private static final int CORRUPTION_RELIEF = 25;

    public MemoryVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        PlayerMemoryData data = PlayerMemoryData.get(serverLevel);
        PlayerProgress progress = data.progressOf(serverPlayer);

        if (progress.corruption() <= 0) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.memory_vial.clear")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        // 代价：挑最近见证的一段记忆（索引最大的已收集项），把它烧掉
        Optional<Integer> victim = progress.collected().stream()
            .max(Comparator.naturalOrder());
        String forgotten = null;
        if (victim.isPresent()) {
            int index = victim.get();
            MemoryEntry entry = MemoryRegistry.byIndex(index);
            progress.forget(index);
            forgotten = entry == null ? null : entry.title().getString();
        }

        progress.addCorruption(-CORRUPTION_RELIEF);
        data.markDirty();

        // 演出：瓶里的东西被喝下去，然后是一段很短的静
        serverLevel.sendParticles(ParticleTypes.SOUL,
            player.getX(), player.getY() + 1.4, player.getZ(),
            30, 0.4, 0.5, 0.4, 0.03);
        serverLevel.playSound(null, player.blockPosition(), ModSounds.CORRUPTION_PULSE.get(),
            SoundSource.PLAYERS, 0.8f, 1.8f);

        // 立刻同步，图鉴上那一行要在本次 tick 内变回划痕
        ServerPacketHandler.syncProgress(serverPlayer, progress);

        if (forgotten != null) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.memory_vial.used", forgotten)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        } else {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.memory_vial.used_empty")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.memory_vial.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.memory_vial.cost")
            .withStyle(ChatFormatting.RED));
    }
}
