package com.clion.echoesofoblivion.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 残片阅读界面（v2.0.0）。
 *
 * <p>比幻境屏幕克制得多——它不是「侵入」，而是**你在读一份文件**。
 * 因此：
 * <ul>
 *   <li>没有抖动、没有渐入渐出、没有侵蚀扭曲。腐坏的是记忆，不是档案本身。</li>
 *   <li>按类别用不同色调的标签，让玩家一眼分辨「谁在说话」。</li>
 *   <li>暂停游戏（与图鉴一致）。</li>
 * </ul>
 *
 * <p>这个对比是有意的：幻境是被强行灌进来的，残片是你自己翻出来的。
 * 前者失控，后者可控——而可控的东西读起来更让人不安，因为它是**冷静的记述**。
 */
public class FragmentScreen extends Screen {

    private static final int PADDING = 16;
    private static final int LINE_HEIGHT = 12;

    private final String titleKey;
    private final String textKey;
    private final String kindId;

    private String titleText;
    private String kindLabel;
    private String hintText;
    private List<String> bodyLines = List.of();

    public FragmentScreen(String titleKey, String textKey, String kindId) {
        super(Component.translatable(titleKey));
        this.titleKey = titleKey;
        this.textKey = textKey;
        this.kindId = kindId;
    }

    @Override
    protected void init() {
        this.titleText = Component.translatable(titleKey).getString();
        this.kindLabel = Component.translatable("fragment.echoesofoblivion.kind." + kindId).getString();
        this.hintText = Component.translatable("screen.echoesofoblivion.fragment.hint").getString();

        // 正文用字面量 \n 分段，再按面板宽度重新折行
        String raw = Component.translatable(textKey).getString();
        int maxWidth = Math.max(120, (int) (width * 0.62));
        List<String> wrapped = new ArrayList<>();
        for (String paragraph : raw.split("\\\\n|\\n")) {
            wrapped.addAll(wrap(paragraph, maxWidth));
            // 段落之间空一行，让文本读起来有段落感
            wrapped.add("");
        }
        if (!wrapped.isEmpty()) {
            wrapped.remove(wrapped.size() - 1);
        }
        this.bodyLines = wrapped;
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                result.add(current.toString());
                current = new StringBuilder();
            }
            if (font.width(word) <= maxWidth) {
                current = new StringBuilder(word);
                continue;
            }
            // 单「词」超宽：逐字符切分（CJK 路径）
            StringBuilder chunk = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (!chunk.isEmpty() && font.width(chunk.toString() + c) > maxWidth) {
                    result.add(chunk.toString());
                    chunk = new StringBuilder();
                }
                chunk.append(c);
            }
            current = new StringBuilder(chunk.toString());
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int top = PADDING + 8;

        // 类别标签（按类别着色）
        int kindColor = kindColor(kindId);
        graphics.drawCenteredString(font, kindLabel, centerX, top, kindColor);

        // 标题
        graphics.drawCenteredString(font, titleText, centerX, top + 16, 0xFFE8E0D0);

        // 分隔线
        int lineWidth = Math.max(120, (int) (width * 0.62));
        graphics.fill(centerX - lineWidth / 2, top + 32, centerX + lineWidth / 2, top + 33, 0xFF3A3A46);

        // 正文
        int y = top + 44;
        int bottom = height - PADDING - 14;
        for (String line : bodyLines) {
            if (y > bottom) {
                break;  // 超出面板就停，避免压到底部提示
            }
            if (!line.isEmpty()) {
                graphics.drawCenteredString(font, line, centerX, y, 0xFFC8C4BC);
            }
            y += LINE_HEIGHT;
        }

        // 底部提示
        graphics.drawCenteredString(font, hintText, centerX, height - PADDING, 0xFF6E6E80);
    }

    /** 类别 → 颜色。与 {@code LoreFragmentItem.Kind} 的 ChatFormatting 保持一致。 */
    private static int kindColor(String kindId) {
        return switch (kindId) {
            case "log" -> 0xFF55FFFF;        // AQUA
            case "letter" -> 0xFFFFAA00;     // GOLD
            case "prayer" -> 0xFFFF55FF;     // LIGHT_PURPLE
            default -> 0xFFAAAAAA;           // GRAY（编年）
        };
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /** 保留：与 ChatFormatting 的映射说明，方便将来加类别。 */
    @SuppressWarnings("unused")
    private static ChatFormatting toFormatting(String kindId) {
        return switch (kindId) {
            case "log" -> ChatFormatting.AQUA;
            case "letter" -> ChatFormatting.GOLD;
            case "prayer" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }
}
