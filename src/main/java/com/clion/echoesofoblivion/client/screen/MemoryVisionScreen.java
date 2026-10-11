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

    /**
     * 「第一段没说完的话」（v2.2.0）：这段记忆的正文在幻境里永远少最后一句。
     *
     * <p>选 {@code her_last_note}（《最后一句完整的话》）是刻意的：
     * 标题承诺「完整」，而正文不完整。玩家第一次读不会觉得异样，
     * 只有重读、或者想起标题时才会发现——**那句话一直没有结束**。
     *
     * <p>这是这一版的主题句：有些东西不是因为丢失而不完整，
     * 而是本来就不完整。
     */
    private static final String UNFINISHED_FRAGMENT_ID = "her_last_note";

    /**
     * 抹掉结尾标点。
     *
     * <p>覆盖大多数语言里表示「句末」的字符。宁可多抹几个，
     * 也不要因为某个语言的标点不在表里而让「没说完」露馅。
     */
    private static final String TRAILING_PUNCTUATION = "。．.！!？?…‥：:；;、，,";

    private static String stripTrailingPunctuation(String line) {
        int end = line.length();
        while (end > 0 && TRAILING_PUNCTUATION.indexOf(line.charAt(end - 1)) >= 0) {
            end--;
        }
        return line.substring(0, end);
    }

    private final MemoryEntry entry;
    private final boolean firstTime;
    private final boolean resonant;
    private final List<String> lines;
    private final List<String> resonanceLines;

    /**
     * 矛盾碎片（v2.2.0）——按错误顺序见证时多出来的那一句。
     *
     * <p>为空表示没有。它是**语言键**，在这里才解析成文本——
     * 文案不下网络这条原则没有破。
     */
    private final List<String> contradictionLines;

    private final String titleText;
    private final String clueText;
    private final String resonantText;
    private final String resonanceMarker;
    private final String skipText;

    private int ticks;

    public MemoryVisionScreen(MemoryEntry entry, boolean firstTime, boolean resonant,
                              String contradictionKey) {
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
        // 矛盾碎片：空键表示没有。解析空键会得到空串，因此这里显式判空。
        this.contradictionLines = (contradictionKey == null || contradictionKey.isBlank())
            ? List.of()
            : List.of(Component.translatable(contradictionKey).getString().split("\\\\n|\\n"));
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

        // 「第一段没说完的话」（v2.2.0）。
        //
        // 记着见证的两段里，有一段的正文在幻境里**永远少最后一句**。
        // 这不是丢失——丢失会让它变成「内容被吞了」，而这一版要的是
        // 「它本来就没有结束」。
        //
        // 为什么在渲染层砍、而不是改语言文件：改语言文件需要 21 个语言
        // 一起把末句删掉，任何一个漏掉都会让这个细节只在部分语言里成立；
        // 而且要验证「删对了没」得逐语言比对。放在渲染层只需要一条规则，
        // 它在所有语言里同时成立。
        List<String> rendered = lines;
        if (entry.id().getPath().equals(UNFINISHED_FRAGMENT_ID) && !lines.isEmpty()) {
            rendered = new ArrayList<>(lines);
            // 去掉最后一行，并抹掉新末行结尾的标点——
            // 「话说到一半停下」和「说完一句但没有句号」是两种不同的感觉，
            // 这一版要的是前者，所以连标点一起拿走。
            rendered.remove(rendered.size() - 1);
            if (!rendered.isEmpty()) {
                int last = rendered.size() - 1;
                rendered.set(last, stripTrailingPunctuation(rendered.get(last)));
            }
        }

        int y = height / 3;
        for (int i = 0; i < rendered.size(); i++) {
            String line = rendered.get(i);
            float la = lineAlpha(i);
            if (line.isEmpty() || la <= 0.0f) {
                y += LINE_HEIGHT;
                continue;
            }
            int color = argb((int) (la * 225.0f), 0xE0E0E0);
            int jitterX = Math.round((JITTER.nextFloat() - 0.5f) * 2.0f * jitterScale);
            int jitterY = Math.round((JITTER.nextFloat() - 0.5f) * jitterScale);
            int wrapIndex = 0;
            for (String wrapped : wrap(line, textWidth)) {
                int lineWidth = font.width(wrapped);
                int startX = centerX - lineWidth / 2 + jitterX;
                if (textInstability() > 0.0f) {
                    // 逐字绘制：抖动 + 形近字替换 + 偶发缺字。
                    // 行号用 (i, wrapIndex) 组合，保证同一行内每个字符的位置判定稳定。
                    drawUnstableLine(graphics, wrapped, i * 7 + wrapIndex, startX, y + jitterY, color);
                } else {
                    graphics.drawString(font, wrapped, startX, y + jitterY, color, false);
                }
                y += LINE_HEIGHT;
                wrapIndex++;
            }
        }

        // 矛盾碎片（v2.2.0）：按错误顺序见证时多出来的那句。
        //
        // 刻意紧接正文之后、用同样的字体与抖动、只是颜色略暗——
        // 它必须读起来像**这段记忆自己的另一句正文**，而不是一条系统提示。
        // 如果它看起来像 UI（「你收集顺序有误！」），整个效果就毁了。
        //
        // 出现时机比正文晚：先让玩家读完已知的内容，再让矛盾浮现。
        if (!contradictionLines.isEmpty()) {
            int appearAt = FADE_IN_TICKS + HOLD_TICKS / 2 + 30;
            if (ticks >= appearAt) {
                float local = Math.min(1.0f, (ticks - appearAt) / 30.0f);
                y += LINE_HEIGHT;
                // 左侧一个很细的记号，让它与正文有区隔但不喧哗
                graphics.drawString(font, "│", left - 10, y,
                    argb((int) (local * globalAlpha * 150.0f), 0x7A6A8A), false);
                for (String line : contradictionLines) {
                    if (line.isEmpty()) {
                        y += LINE_HEIGHT;
                        continue;
                    }
                    for (String wrapped : wrap(line, textWidth)) {
                        int lineWidth = font.width(wrapped);
                        int startX = centerX - lineWidth / 2;
                        int color = argb((int) (local * globalAlpha * 190.0f), 0xC8B4D8);
                        if (textInstability() > 0.0f) {
                            drawUnstableLine(graphics, wrapped, 900 + y, startX, y, color);
                        } else {
                            graphics.drawString(font, wrapped, startX, y, color, false);
                        }
                        y += LINE_HEIGHT;
                    }
                }
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

    // ---------------------------------------------------------------- 文本不稳定（v1.2.0）

    /** 侵蚀低于该比例时文字完全正常。 */
    private static final float TEXT_UNSTABLE_ONSET = 0.40f;

    /**
     * 相似的替换字符。
     *
     * <p>选择标准：**看起来像同一个字**，但细看不对。
     * 这比随机乱码有效得多——乱码会被立刻识别为「特效」，
     * 而一个长得几乎一样的字形会让人怀疑自己是不是看错了。
     */
    private static final char[] LOOKALIKES = {'o', 'l', 'I', 'i', 'ı', 'ɑ', 'е', 'о', 'ѕ', 'ν'};

    /**
     * 按侵蚀程度让记忆文本变得不稳定。
     *
     * <p>三件事按强度递增发生：
     * <ol>
     *   <li>字符位置轻微错位（{@code glyphOffset}）</li>
     *   <li>个别字符被替换成形近字（{@code corruptChar}）</li>
     *   <li>个别字符整个消失（{@code corruptChar} 返回 0）</li>
     * </ol>
     *
     * <p><b>两个必须遵守的约束</b>：
     * <ul>
     *   <li><b>必须仍然可读。</b>缺字太多会变成烦人而不是恐怖，
     *       因此最高侵蚀下也只有约 1/7 的字符受影响。</li>
     *   <li><b>不能闪烁。</b>随机源由 {@code (行号, 列号)} 的固定哈希构成，
     *       与时间无关——同一个字在所有帧里的状态完全一致。
     *       如果文字每帧都在跳，玩家会立刻明白那是渲染效果。</li>
     * </ul>
     *
     * @return 要绘制的单个字符；返回 {@code 0} 表示这个字消失了
     */
    private char corruptChar(String line, int lineIndex, int charIndex) {
        char c = line.charAt(charIndex);
        if (c == ' ') {
            return c;
        }

        float intensity = textInstability();
        if (intensity <= 0.0f) {
            return c;
        }

        // 固定哈希：同一位置在任何一帧都得到同样的判定。
        // 0x9E3779B1 是 Knuth 黄金比常数的 32 位形式（对应无符号的 2654435761）。
        int hash = (lineIndex * 31 + charIndex) * 0x9E3779B1;
        hash ^= hash >>> 15;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        int roll = Math.floorMod(hash, 1000);

        // 消失：最高侵蚀下约 4%
        if (roll < (int) (intensity * 40)) {
            return 0;
        }
        // 形近字替换：最高侵蚀下约 10%
        if (roll < (int) (intensity * 140)) {
            int pick = Math.floorMod(hash >> 8, LOOKALIKES.length);
            return LOOKALIKES[pick];
        }
        return c;
    }

    /** 每个字符的额外水平抖动（像素）。侵蚀越高，字距越乱。 */
    private int glyphOffset(int lineIndex, int charIndex) {
        float intensity = textInstability();
        if (intensity <= 0.0f) {
            return 0;
        }
        // 0x9E3779B1 同样是黄金比常数的 32 位形式
        int hash = (lineIndex * 17 + charIndex * 7) * 0x9E3779B1;
        hash ^= hash >>> 13;
        int roll = Math.floorMod(hash, 3) - 1;   // -1 / 0 / 1
        return Math.round(roll * intensity * 1.6f);
    }

    /** 当前文本不稳定强度 0~1。 */
    private float textInstability() {
        float ratio = ClientData.getCorruptionRatio();
        if (ratio <= TEXT_UNSTABLE_ONSET) {
            return 0.0f;
        }
        return Math.min(1.0f, (ratio - TEXT_UNSTABLE_ONSET) / (1.0f - TEXT_UNSTABLE_ONSET));
    }

    /**
     * 逐字绘制一行，应用抖动与不稳定。
     *
     * <p>不用 {@code drawString} 整行绘制，因为需要给每个字符单独定位与替换。
     */
    private void drawUnstableLine(GuiGraphics graphics, String line, int lineIndex,
                                  int startX, int y, int color) {
        int x = startX;
        for (int i = 0; i < line.length(); i++) {
            char c = corruptChar(line, lineIndex, i);
            if (c != 0) {
                graphics.drawString(font, String.valueOf(c), x + glyphOffset(lineIndex, i), y,
                    color, false);
            }
            // 用原字符的宽度推进，避免文字整体「缩水」而失去可读性
            x += font.width(String.valueOf(line.charAt(i)));
        }
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
