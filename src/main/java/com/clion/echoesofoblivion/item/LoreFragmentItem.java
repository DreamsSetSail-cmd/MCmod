package com.clion.echoesofoblivion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 残片（v2.0.0）——集齐后可读的叙事碎片。
 *
 * <p>这是「故事太少」的直接解法：把文明的编年史拆成 12 段残片，
 * 散落在走廊的遗迹里（由 {@code MonumentFeature} 与环境生成放置）。
 * 每读一段，{@code docs/story-bible.md} 里那个七纪元历史就多露出一块。
 *
 * <h2>为什么不做成一本完整的书</h2>
 *
 * <p>一次性给出全部历史会毁掉「考古」这件事。残片系统让玩家**逐步**理解，
 * 而且顺序不可控——你可能先读到实验记录，再读到桥的时代，
 * 于是因果是**自己拼出来的**，不是被讲出来的。这与 5 段记忆的设计意图一致，
 * 只是粒度更细、更偏历史而非个人体验。
 *
 * <h2>残片的语气</h2>
 *
 * <p>12 段残片分四类，语气各不相同，这是刻意区分「谁在说话」：
 * <ul>
 *   <li><b>编年</b>（chronicle）：冷静的、事后的记述，像官方档案。</li>
 *   <li><b>实验记录</b>（log）：短句、术语、编号，像实验室笔记。</li>
 *   <li><b>私人信件</b>（letter）：有情绪、有犹豫，唯一出现「我」的地方。</li>
 *   <li><b>祈祷</b>（prayer）：后期出现的、没有对象的祈词。</li>
 * </ul>
 */
public class LoreFragmentItem extends Item {

    /** 残片类别——决定提示文字的标签颜色与语气标签。 */
    public enum Kind {
        /** 编年史：官方档案语气。 */
        CHRONICLE("chronicle", ChatFormatting.GRAY),
        /** 实验记录：术语与编号。 */
        LOG("log", ChatFormatting.AQUA),
        /** 私人信件：唯一出现「我」的地方。 */
        LETTER("letter", ChatFormatting.GOLD),
        /** 祈祷：没有对象的祈词。 */
        PRAYER("prayer", ChatFormatting.LIGHT_PURPLE);

        private final String id;
        private final ChatFormatting color;

        Kind(String id, ChatFormatting color) {
            this.id = id;
            this.color = color;
        }

        public String id() {
            return id;
        }

        public ChatFormatting color() {
            return color;
        }

        /** 语言文件键：{@code fragment.echoesofoblivion.kind.<id>} */
        public String translationKey() {
            return "fragment.echoesofoblivion.kind." + id;
        }
    }

    private final String fragmentId;
    private final Kind kind;

    public LoreFragmentItem(String fragmentId, Kind kind, Properties properties) {
        super(properties);
        this.fragmentId = fragmentId;
        this.kind = kind;
    }

    public String fragmentId() {
        return fragmentId;
    }

    public Kind kind() {
        return kind;
    }

    /** 正文的语言文件键。 */
    public String textKey() {
        return "fragment.echoesofoblivion." + fragmentId + ".text";
    }

    /** 标题的语言文件键。 */
    public String titleKey() {
        return "fragment.echoesofoblivion." + fragmentId + ".title";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 打开阅读界面。延迟一 tick 打开，避免与物品使用动画抢输入。
            com.clion.echoesofoblivion.client.ClientPacketHandler.openFragment(
                titleKey(), textKey(), kind().id());
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(kind.translationKey()).withStyle(kind.color()));
        tooltip.add(Component.translatable("item.echoesofoblivion.lore_fragment.hint")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 私人信件与祈祷有附魔光效：它们是唯一「有个人」的两类
        return kind == Kind.LETTER || kind == Kind.PRAYER;
    }
}
