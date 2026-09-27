package com.clion.echoesofoblivion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 研究者提灯（v2.0.0）——**让污染长得慢一点，仅此而已**。
 *
 * <h2>它的克制是设计出来的</h2>
 *
 * <p>拿着它时，你身上的侵蚀增长速度减半。它不治病、不免疫、不清感染区，
 * 只是把你走向坍缩的速度拉长一倍。
 *
 * <p>这是刻意的弱：如果有一件装备能显著降低侵蚀，感染区就不再是压力，
 * 而模组全部的紧张感都建立在「你正在被慢慢消耗」上。
 * 减半正好——它让你能多做一次远征，但改变不了终点。
 *
 * <h2>为什么是提灯</h2>
 *
 * <p>灯是「在黑暗里继续工作」的隐喻，而研究者正是那个明知危险还要记录的人。
 * 提灯只在她手里有意义，而她已经不在了——玩家捡起来用它，
 * 等于接手了一个没有人的岗位。
 */
public class ResearcherLanternItem extends Item {

    /** 减益倍率。0.5 = 侵蚀增长减半。 */
    public static final float GROWTH_MULTIPLIER = 0.5f;

    public ResearcherLanternItem(Properties properties) {
        super(properties);
    }

    /**
     * 玩家是否正持有提灯（主手或副手）。
     *
     * <p>由 {@code CorruptionEventHandler} 在每秒结算侵蚀增长前查询。
     * 检查两只手而不是只查主手，是因为玩家会一手灯一手铲子——
     * 那正是这个模组里最典型的姿势。
     */
    public static boolean isCarried(Player player) {
        return player.getMainHandItem().getItem() instanceof ResearcherLanternItem
            || player.getOffhandItem().getItem() instanceof ResearcherLanternItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.researcher_lantern.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.researcher_lantern.slow")
            .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
