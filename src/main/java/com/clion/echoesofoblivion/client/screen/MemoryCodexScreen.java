package com.clion.echoesofoblivion.client.screen;

import com.clion.echoesofoblivion.client.ClientData;
import com.clion.echoesofoblivion.memory.MemoryEntry;
import com.clion.echoesofoblivion.memory.MemoryRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆图鉴（v1.1.0）。
 *
 * <p>把记忆卷轴从装饰品变成可读的界面。它做三件事：
 * <ol>
 *   <li><b>收集状态</b>：五段记忆逐条列出，未见证的显示为「被划掉的空行」而不是空白格——
 *       空白不吓人，<b>被擦掉的痕迹</b>才吓人。</li>
 *   <li><b>线索</b>：逐条列出已获得的线索词，提示玩家「这些是可以配对的东西」。</li>
 *   <li><b>共鸣连线</b>：当两段本该相连的记忆都已见证，在它们之间画一条金线。</li>
 * </ol>
 *
 * <p>第 3 点是这个界面的真正目的：**把玩家脑内的推理过程变成可见的东西**。
 * 在它之前，「非线性拼图」只存在于玩家的笔记里；有了它，拼图成为游戏给予的反馈。
 *
 * <p>数据全部来自客户端缓存 {@link ClientData}（由 {@code MemorySyncPacket} 同步），
 * 本界面不做任何权威判定，也不向服务端写入。
 */
public class MemoryCodexScreen extends Screen {

    /** 左侧列表宽度占比上限。 */
    private static final int LIST_MIN_WIDTH = 120;
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 12;

    private final List<Row> rows = new ArrayList<>();
    private String resonancelessText;
    private String emptyTitleText;
    private String emptyBodyText;
    private String hintText;

    /** 一段记忆在列表中的渲染信息。 */
    private record Row(MemoryEntry entry, int index, boolean collected, boolean resonant,
                       int clueLineY) {
    }

    public MemoryCodexScreen() {
        super(Component.translatable("screen.echoesofoblivion.codex"));
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.resonancelessText = Component.translatable("screen.echoesofoblivion.codex.no_resonance").getString();
        this.emptyTitleText = Component.translatable("screen.echoesofoblivion.codex.empty_title").getString();
        this.emptyBodyText = Component.translatable("screen.echoesofoblivion.codex.empty_body").getString();
        this.hintText = Component.translatable("screen.echoesofoblivion.codex.hint").getString();

        int y = PADDING + 26;
        for (int i = 0; i < MemoryRegistry.size(); i++) {
            MemoryEntry entry = MemoryRegistry.byIndex(i);
            boolean collected = ClientData.hasCollected(i);
            // 共鸣判定在客户端只是一次展示用的推算：连接线索是否齐备
            boolean resonant = collected && entry.connectedClues().stream()
                .allMatch(ClientData::hasClue);
            rows.add(new Row(entry, i, collected, resonant, y));
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // 标题
        int titleColor = 0xFFD6C8FF;
        graphics.drawCenteredString(font, title, width / 2, PADDING, titleColor);

        boolean anyCollected = rows.stream().anyMatch(Row::collected);
        int listWidth = Math.max(LIST_MIN_WIDTH, width / 3);
        int listLeft = PADDING;
        int detailLeft = listLeft + listWidth + PADDING * 2;

        // ---------------- 左侧：记忆列表 ----------------
        for (Row row : rows) {
            renderRow(graphics, row, listLeft, listWidth);
        }

        // ---------------- 右侧：详情或空状态 ----------------
        if (!anyCollected) {
            renderEmptyState(graphics);
        } else {
            renderDetail(graphics, detailLeft);
        }

        // ---------------- 共鸣连线 ----------------
        renderResonanceLinks(graphics, listLeft, listWidth);

        // 底部提示
        int hintColor = 0xFF6E6E80;
        graphics.drawCenteredString(font, hintText, width / 2, height - PADDING - 10, hintColor);
    }

    /** 单行：已见证显示序号与标题；未见证显示被划掉的空行。 */
    private void renderRow(GuiGraphics graphics, Row row, int left, int listWidth) {
        int y = row.clueLineY();
        boolean hovered = false;

        if (row.collected()) {
            // 左侧色条表示这条记忆已有内容
            graphics.fill(left, y, left + 3, y + ROW_HEIGHT - 4, 0xFF000000 | row.entry().color());
            int textColor = row.resonant() ? 0xFFFFD27A : 0xFFE0E0E8;
            String label = (row.index() + 1) + ". " + row.entry().title().getString();
            graphics.drawString(font, label, left + 9, y + 5, textColor, false);
        } else {
            // 未见证：画一段「被划掉」的占位横线，而不是留白。
            // 划痕比空白更让人不安——它暗示曾经写过东西。
            int lineY = y + 9;
            int scratchEnd = left + Math.min(listWidth - 12, 70);
            graphics.fill(left + 9, lineY, scratchEnd, lineY + 1, 0xFF3A3A46);
            // 两道交错的擦痕
            graphics.fill(left + 14, lineY - 2, left + 15, lineY + 3, 0xFF2E2E38);
            graphics.fill(left + 38, lineY - 1, left + 39, lineY + 2, 0xFF2E2E38);
            graphics.fill(left + 56, lineY - 2, left + 57, lineY + 2, 0xFF2E2E38);
        }
        // 静默 hover 变量，保留后续做交互提示
        if (hovered) {
            graphics.fill(left, y, left + listWidth, y + ROW_HEIGHT - 4, 0x10FFFFFF);
        }
    }

    /** 一条记忆都没有时的空状态。 */
    private void renderEmptyState(GuiGraphics graphics) {
        int centerX = width / 2;
        int maxWidth = Math.max(120, (int) (width * 0.55));
        int y = height / 2 - 24;

        // 被划掉的一行字：有人试图写下来，然后被擦掉了
        int scratchColor = 0xFF3A3A46;
        int lineWidth = Math.min(200, maxWidth);
        int lineY = y;
        graphics.fill(centerX - lineWidth / 2, lineY, centerX + lineWidth / 2, lineY + 1, scratchColor);
        for (int i = 0; i < 5; i++) {
            int x = centerX - lineWidth / 2 + 18 + i * 32;
            graphics.fill(x, lineY - 3, x + 1, lineY + 4, 0xFF2A2A34);
        }

        // 两段文案都要换行：德语、俄语等语言的行长明显超过英文，
        // 不换行会在窄窗口下溢出面板。
        y += 12;
        for (String line : wrap(emptyTitleText, maxWidth)) {
            graphics.drawCenteredString(font, line, centerX, y, 0xFF8A8A9A);
            y += 11;
        }
        y += 4;
        for (String line : wrap(emptyBodyText, maxWidth)) {
            graphics.drawCenteredString(font, line, centerX, y, 0xFF5A5A68);
            y += 11;
        }
    }

    /** 右侧详情：所有已见证记忆的线索与共鸣碎片状态。 */
    private void renderDetail(GuiGraphics graphics, int left) {
        int y = PADDING + 26;
        int maxWidth = Math.max(80, width - left - PADDING);

        // 已获得的线索
        int clueCount = 0;
        for (Row row : rows) {
            if (!row.collected()) {
                continue;
            }
            clueCount++;
            String clue = row.entry().clueText().getString();
            int clueColor = 0xFF9FA8C8;
            graphics.drawString(font, clue, left, y, clueColor, false);
            y += 12;

            if (row.resonant()) {
                // 共鸣达成的条目：把因果碎片也列出来
                for (String line : wrap(row.entry().resonanceText().getString(), maxWidth)) {
                    graphics.drawString(font, line, left + 8, y, 0xFFE8D6A0, false);
                    y += 11;
                }
            }
            y += 6;
        }

        if (clueCount == 0) {
            // 同样需要换行：这句话在各种语言里的长度差异很大
            for (String line : wrap(resonancelessText, maxWidth)) {
                graphics.drawString(font, line, left, y, 0xFF6E6E80, false);
                y += 11;
            }
        }
    }

    /**
     * 在已共鸣的记忆之间画连线。
     *
     * <p>连线画在列表左侧的空白区，用折线避开文字：先向左出一小段，
     * 再垂直走，最后回到目标行。金色半透明，不抢文字的注意力。
     */
    private void renderResonanceLinks(GuiGraphics graphics, int listLeft, int listWidth) {
        int railX = listLeft - 6;
        if (railX < 2) {
            // 窗口太窄时退化为在列表内右侧画竖线
            railX = listLeft + listWidth - 4;
        }
        int linkColor = 0x88FFD27A;

        for (Row from : rows) {
            if (!from.resonant()) {
                continue;
            }
            for (int j = 0; j < MemoryRegistry.size(); j++) {
                if (j == from.index()) {
                    continue;
                }
                Row to = rows.get(j);
                // 只在「双方都已见证」时画线，避免连线指向空槽
                if (!to.collected()) {
                    continue;
                }
                MemoryEntry target = MemoryRegistry.byIndex(j);
                // 必须确实是一条连接关系（避免重复画同一条）
                if (!from.entry().connectedClues().contains(target.clue())) {
                    continue;
                }
                // 只画一次：索引小的负责画
                if (from.index() > j) {
                    continue;
                }
                int yFrom = from.clueLineY() + (ROW_HEIGHT - 4) / 2;
                int yTo = to.clueLineY() + (ROW_HEIGHT - 4) / 2;
                graphics.fill(Math.min(railX, listLeft), yFrom, Math.max(railX, listLeft) + 1, yFrom + 1, linkColor);
                graphics.fill(railX, Math.min(yFrom, yTo), railX + 1, Math.max(yFrom, yTo) + 1, linkColor);
                graphics.fill(Math.min(railX, listLeft), yTo, Math.max(railX, listLeft) + 1, yTo + 1, linkColor);
            }
        }
    }

    /**
     * 按像素宽度换行。
     *
     * <p>先按空格断词；**若单个「词」本身就超宽，则退化为逐字符断行**。
     * 后一条对中国/日本/韩国文本是必需的——它们整句没有空格，
     * 只按空格断词会把一整句当成一个不可分割的词，直接溢出面板。
     */
    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\\\n|\\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
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
        }
        return result;
    }

    @Override
    public boolean isPauseScreen() {
        // 图鉴是「查阅」而不是「侵入」，暂停游戏是合理的
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
