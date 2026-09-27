package com.clion.echoesofoblivion.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端每 tick 与登录/登出钩子。
 *
 * <p>1.20.6 起 {@code TickEvent.Phase} 与 {@code TickEvent.ClientTickEvent(Phase)}
 * 已标记 {@code forRemoval}，应改用 {@code ClientTickEvent.Post} 子类。
 */
@Mod.EventBusSubscriber(modid = "echoesofoblivion", value = Dist.CLIENT)
public final class ClientTickHandler {

    private ClientTickHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        ClientScreenEffects.tick();
        handleMemoryAttackKey();
    }

    /**
     * 「记忆攻击」键（阶段 6）。
     *
     * <p>按下时把「当前选中的记忆」索引发给服务端。选择规则与快捷栏槽位绑定：
     * 槽位 0~8 映射到 5 条记忆中的一条，便于玩家用滚轮切换要用哪段记忆。
     */
    private static void handleMemoryAttackKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (KeyBindings.MEMORY_ATTACK.consumeClick()) {
            int slot = minecraft.player.getInventory().selected;
            int memoryIndex = slot % com.clion.echoesofoblivion.memory.MemoryRegistry.size();
            com.clion.echoesofoblivion.network.ModNetwork.sendToServer(
                new com.clion.echoesofoblivion.network.packets.MemoryAttackPacket(memoryIndex));
        }
    }

    /** 进入世界时清空上一个世界的残留状态，避免进度/侵蚀值串档。 */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ClientData.resetAll();
        ClientScreenEffects.clear();
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ClientData.resetAll();
        ClientScreenEffects.clear();
    }
}
