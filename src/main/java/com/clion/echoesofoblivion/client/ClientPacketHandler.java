package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.client.screen.MemoryCodexScreen;
import com.clion.echoesofoblivion.client.screen.MemoryVisionScreen;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 客户端包处理（阶段 1 / 4 / 5）。
 *
 * <p>所有方法都在 {@code Dist.CLIENT} 保护下被调用，因此本类可以自由引用
 * {@code Minecraft} 与 {@code Screen}。方法内不再依赖任何服务端类型。
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    /** 打开记忆幻境。 */
    public static void openVision(int memoryIndex, boolean firstTime) {
        if (memoryIndex < 0 || memoryIndex >= MemoryRegistry.size()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        MemoryEntry entry = MemoryRegistry.byIndex(memoryIndex);
        // 共鸣判定在客户端只做展示：连接线索是否齐备
        boolean resonant = entry.connectedClues().stream().allMatch(ClientData::hasClue)
            && ClientData.hasCollected(memoryIndex);

        ClientData.markCollected(memoryIndex);
        minecraft.setScreen(new MemoryVisionScreen(entry, firstTime, resonant));
        if (minecraft.player != null) {
            minecraft.player.playSound(ModSounds.MEMORY_VISION.get(), 0.6f, 1.0f);
        }
    }

    /** 应用全量进度同步。 */
    public static void applyMemorySync(int collectedMask, String clueCsv) {
        ClientData.setCollectedMask(collectedMask);
        Set<String> clues = new HashSet<>();
        if (clueCsv != null && !clueCsv.isBlank()) {
            clues.addAll(Arrays.asList(clueCsv.split(",")));
        }
        ClientData.setClues(clues);
    }

    public static void applyCorruption(int level, float ratio) {
        ClientData.setCorruption(level, ratio);
    }

    public static void applyShadow(int state) {
        ClientData.setShadowState(state);
    }

    /** 镜像区块同步：{@code add} 为 true 时只追加，否则整体替换。 */
    public static void applyRealityShift(java.util.List<Long> chunks, boolean add, int phase) {
        Set<Long> set = new HashSet<>(chunks);
        if (add) {
            ClientData.addInfectedChunks(set);
        } else {
            ClientData.setInfectedChunks(set);
        }
        ClientData.setRealityPhase(phase);
    }

    /** 传送反馈：短暂的视野压制与音效（阶段 2 会在此基础上加渐入渐出）。 */
    public static void onPortalTravel(boolean entering, boolean stable) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.playSound(ModSounds.PORTAL_TRAVEL.get(), 1.0f, stable ? 1.0f : 0.6f);
        ClientScreenEffects.startTravelFade(entering, stable);
    }

    /** 打开记忆图鉴（v1.1.0）。由记忆卷轴触发。 */
    public static void openCodex() {
        Minecraft.getInstance().setScreen(new MemoryCodexScreen());
    }

    /**
     * 通用表现指令（阶段 6）。
     *
     * <p>{@code kind} 与 {@code RenderStatePacket} 中的常量对应。
     */
    public static void applyRenderState(int kind, int value, float ratio, boolean flag) {
        switch (kind) {
            case 0 -> ClientScreenEffects.startTravelFade(value == 1, flag);
            case 1 -> {
                ClientData.setBossPhase(value);
                ClientData.setBossStability(ratio);
            }
            case 2 -> {
                ClientData.setBossPhase(-1);
                ClientData.setBossStability(0.0f);
                ClientScreenEffects.startTravelFade(false, true);
            }
            default -> {
                // 未知类型忽略，保证向后兼容
            }
        }
    }
}
