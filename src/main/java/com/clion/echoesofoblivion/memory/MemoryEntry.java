package com.clion.echoesofoblivion.memory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 单条记忆的静态定义。
 *
 * <p>线索（{@link #clue()}）是非线性谜题的连接词：{@link #connectedClues()}
 * 中列出的线索若已被玩家收集，则本条记忆解锁「共鸣」内容。
 *
 * <p>{@link #resonanceKey()} 是共鸣时额外显示的一段「拼图碎片」。
 * 它是全篇叙事的关键：单独看每段记忆都只是悲剧片段，**两两共鸣后才浮现因果**。
 * 例如「天空碎裂」与「最后的实验」放在一起，玩家才能推出那道光不是天灾，是试运行。
 * 这段文本由语言文件提供，键名见 {@link MemoryRegistry}。
 */
public record MemoryEntry(
    ResourceLocation id,
    String titleKey,
    String contentKey,
    String clueKey,
    String clue,
    String resonanceKey,
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

    /** 共鸣时显示的因果碎片。 */
    public Component resonanceText() {
        return Component.translatable(resonanceKey);
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
