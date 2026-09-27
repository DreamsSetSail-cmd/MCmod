package com.clion.echoesofoblivion.memory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 单条记忆的静态定义。
 *
 * <p>线索（{@link #clue()}）是阶段 3 非线性谜题的连接词：{@link #connectedClues()}
 * 中列出的线索若已被玩家收集，则本条记忆解锁「共鸣」内容。
 */
public record MemoryEntry(
    ResourceLocation id,
    String titleKey,
    String contentKey,
    String clueKey,
    String clue,
    int color,
    List<String> connectedClues
) {
    public Component title() {
        return Component.translatable(titleKey);
    }

    public Component content() {
        return Component.translatable(contentKey);
    }

    public Component clueText() {
        return Component.translatable(clueKey);
    }

    /** RGB 三通道，供幻境屏幕做渐变色混合。 */
    public float red() {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    public float blue() {
        return (color & 0xFF) / 255.0f;
    }
}
