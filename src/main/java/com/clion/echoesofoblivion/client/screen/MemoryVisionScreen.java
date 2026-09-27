package com.clion.echoesofoblivion.client.screen;

import com.clion.echoesofoblivion.client.ClientData;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 记忆幻境屏幕（阶段 1）。
 *
 * <p>设计要点（对应技术大纲 §7.1）：
 * <ul>
 *   <li>继承 {@code Screen}——幻境不是容器界面</li>
 *   <li>黑屏渐入 → 记忆正文逐行浮现 → 渐出</li>
 *   <li>不暂停游戏（{@code isPauseScreen=false}），保持恐怖节奏连续</li>
 *   <li>文字带抖动，幅度随侵蚀值升高，制造「这段记忆不稳定」的感觉</li>
 *   <li>关联线索齐备时显示「共鸣」提示（阶段 3 非线性奖励）</li>
 * </ul>
 */
public class MemoryVisionScreen extends Screen {

    private static final int FADE_IN_TICKS = 40;
    private static final int HOLD_TICKS = 190;
    private static final int FADE_OUT_TICKS = 40;
    private static final int TOTAL_TICKS = FADE_IN_TICKS + HOLD_TICKS + FADE_OUT_TICKS;
    private static final int LINE_HEIGHT = 14;

    private static final Random JITTER = new Random();

    private final MemoryEntry entry;
    private final boolean firstTime;
    private final boolean resonant;
    private final List<String> lines;
    private final List<String> resonanceLines;
    private final String titleText;
    private final String clueText;
    private final String resonantText;
    private final String resonanceMarker;
    private final String skipText;

    private int ticks;

    public MemoryVisionScreen(MemoryEntry entry, boolean firstTime, boolean resonant) {
        super(Component.translatable("screen.echoesofoblivion.memory_vision"));
        this.entry = entry;
        this.firstTime = firstTime;
        this.resonant = resonant;
        this.titleText = entry.title().getString();
        this.clueText = entry.clueText().getString();
        this.resonantText = Component.translatable("screen.echoesofoblivion.resonance").getString();
        this.resonanceMarker = "◈";
        this.skipText = Component.translatable("screen.echoesofoblivion.skip").getString();
        // 语言文件里正文以字面量 \n 分隔
        this.lines = List.of(entry.content().getString().split("\\\\n|\\n"));
        // 共鸣碎片同样以 \n 分隔；只在共鸣时才会被渲染
        this.resonanceLines = List.of(entry.resonanceText().getString().split("\\\\n|\\n"));
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= TOTAL_TICKS) {
            onClose();
        }
    }

    /** 当前整体不透明度 0~1。 */
    private float alpha() {
        if (ticks < FADE_IN_TICKS) {
            return ticks / (float) FADE_IN_TICKS;
        }
        int afterHold = ticks - FADE_IN_TICKS - HOLD_TICKS;
        if (afterHold <= 0) {
            return 1.0f;
        }
        return Math.max(0.0f, 1.0f - afterHold / (float) FADE_OUT_TICKS);
    }

    /** 第 i 行的独立渐入进度，让正文像回忆一样逐句浮现。 */
    private float lineAlpha(int index) {
        int appearAt = FADE_IN_TICKS + index * 12;
        if (ticks < appearAt) {
            return 0.0f;
        }
        float local = Math.min(1.0f, (ticks - appearAt) / 20.0f);
        return alpha() * local;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float globalAlpha = alpha();
        if (globalAlpha <= 0.0f) {
            return;
        }

        // 背景：几乎全黑
        int bgAlpha = (int) (globalAlpha * 235.0f);
        graphics.fill(0, 0, width, height, bgAlpha << 24);
        renderColorWash(graphics, globalAlpha);

        int centerX = width / 2;
        int textWidth = Math.max(80, (int) (width * 0.6));
        int left = centerX - textWidth / 2;

        // 标题
        int titleAlpha = (int) (globalAlpha * 255.0f);
        graphics.drawCenteredString(font, titleText, centerX, height / 4, argb(titleAlpha, entry.color()));

        // 抖动幅度随侵蚀值升高（最高约 ±3px）
        float jitterScale = 0.5f + ClientData.getCorruptionRatio() * 2.5f;

        int y = height / 3;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float la = lineAlpha(i);
            if (line.isEmpty() || la <= 0.0f) {
                y += LINE_HEIGHT;
                continue;
            }
            int color = argb((int) (la * 225.0f), 0xE0E0E0);
            int jitterX = Math.round((JITTER.nextFloat() - 0.5f) * 2.0f * jitterScale);
            int jitterY = Math.round((JITTER.nextFloat() - 0.5f) * jitterScale);
            for (String wrapped : wrap(line, textWidth)) {
                graphics.drawString(font, wrapped, left + jitterX, y + jitterY, color, false);
                y += LINE_HEIGHT;
            }
        }

        // 线索：这条记忆留下的、可用于关联的碎片
        // 位置定在 height-96 而不是更靠下：共鸣碎片最多 4 行，
        // 若贴着底部会与「按 ESC 脱离」提示重叠。
        if (ticks > FADE_IN_TICKS + 40) {
            int clueY = height - 96;
            int clueAlpha = (int) (globalAlpha * 190.0f);
            graphics.drawString(font, clueText, centerX - font.width(clueText) / 2, clueY,
                argb(clueAlpha, 0x9FA8C8), false);
        }

        // 共鸣：关联线索齐备时浮现的「因果碎片」。
        // 单独看每段记忆都只是悲剧，两段共鸣后才拼得出因果——这是叙事的关键一步，
        // 因此用金色单独成段，并且逐句淡入。
        if (resonant && ticks > FADE_IN_TICKS + 60) {
            int labelAlpha = (int) (globalAlpha * 230.0f);
            int labelY = height - 82;
            int labelX = centerX - font.width(resonantText) / 2;
            // 符号与文字分开画，让符号可以单独带色
            graphics.drawString(font, resonanceMarker, labelX, labelY,
                argb(labelAlpha, 0xFFD27A), false);
            graphics.drawString(font, resonantText,
                labelX + font.width(resonanceMarker + " "), labelY,
                argb(labelAlpha, 0xFFD27A), false);

            int maxWidth = Math.max(120, (int) (width * 0.62));
            int resY = labelY + LINE_HEIGHT + 4;
            for (int i = 0; i < resonanceLines.size() && i < 4; i++) {
                String line = resonanceLines.get(i);
                if (line.isEmpty()) {
                    resY += LINE_HEIGHT;
                    continue;
                }
                // 每一句比标签稍晚出现，读起来像因果在慢慢拼上
                int appearAt = FADE_IN_TICKS + 70 + i * 14;
                if (ticks < appearAt) {
                    break;
                }
                float local = Math.min(1.0f, (ticks - appearAt) / 20.0f);
                int lineAlpha = (int) (globalAlpha * local * 215.0f);
                for (String wrapped : wrap(line, maxWidth)) {
                    graphics.drawString(font, wrapped, centerX - font.width(wrapped) / 2, resY,
                        argb(lineAlpha, 0xE8D6A0), false);
                    resY += LINE_HEIGHT;
                }
            }
        }

        // 提示（首次目睹记忆时不提示跳过，让玩家先沉浸）
        if (ticks > FADE_IN_TICKS && !firstTime) {
            int hintAlpha = (int) (globalAlpha * 130.0f);
            graphics.drawString(font, skipText, centerX - font.width(skipText) / 2, height - 30,
                argb(hintAlpha, 0x808080), false);
        }
    }

    /** 上下边缘铺一条本条记忆的色调，替代自定义着色器（低端 GPU 友好）。 */
    private void renderColorWash(GuiGraphics graphics, float globalAlpha) {
        int bandHeight = Math.max(4, height / 12);
        int bandAlpha = (int) (globalAlpha * 34.0f);
        int color = argb(bandAlpha, entry.color());
        graphics.fill(0, 0, width, bandHeight, color);
        graphics.fill(0, height - bandHeight, width, height, color);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result.isEmpty() ? List.of(text) : result;
    }

    private static int argb(int alpha, int rgb) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Esc / 点击跳过：幻境应当可以被主动打断。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        // 不暂停：幻境是「侵入」而非「休息」
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
