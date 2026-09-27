package com.clion.echoesofoblivion.world;

/**
 * 仪式（v2.0.0）——玩家主动发起的、**可见地改变世界**的事件。
 *
 * <h2>为什么需要仪式</h2>
 *
 * <p>v1.x 的所有机制都是**被动的**：侵蚀值会涨、感染会扩散、亡魂会成形，
 * 玩家只能观察和忍受。这导致玩法偏低调——你在这个世界里没有主动权。
 *
 * <p>仪式把主动权交还给玩家，而且是**有代价的主动权**：
 *
 * <table>
 *   <tr><th>仪式</th><th>材料</th><th>效果</th><th>代价</th></tr>
 *   <tr><td>安魂曲 Requiem</td><td>共鸣音叉 + 骸骨灰</td>
 *       <td>驱散半径 32 内所有亡魂与幻影</td><td>消耗材料，无永久收益</td></tr>
 *   <tr><td>寂静降临 Descent</td><td>共鸣音叉 + 寂静碎片 + 虚空余烬</td>
 *       <td>半径 48 内的世界**彻底静音** 30 秒，颜色去饱和</td>
 *       <td>静音期间玩家也听不见任何提示音</td></tr>
 *   <tr><td>虚空通道 Void Passage</td><td>共鸣音叉 + 镜面碎片</td>
 *       <td>在走廊与主世界间做一次**定向**传送（不依赖传送门）</td>
 *       <td>扭曲玩家自身的侵蚀值 +10</td></tr>
 * </table>
 *
 * <p><b>关键设计</b>：所有仪式都**不造成伤害，也不召唤敌人**。
 * 它们改变的是**玩家周围的环境状态**——这与聚合体的环境级技能是同一条设计线：
 * 这个世界里没有「打怪」，只有「改变你所处的条件」。
 *
 * <p>「寂静降临」是最重要的一个：它让玩家**亲手发动**一次静默。
 * 玩家会第一次站在寂静那一侧，而不是被它侵害——这是一个立场反转，
 * 也是全篇唯一一次让玩家尝到「污染源」这个身份的滋味。
 */
public final class Rituals {

    /** 仪式类型。 */
    public enum Type {
        /** 安魂曲：驱散范围内的亡魂与幻影。 */
        REQUIEM("requiem", 32.0),
        /** 寂静降临：范围内的世界静音并去饱和。 */
        DESCENT("descent", 48.0),
        /** 虚空通道：定向传送。 */
        VOID_PASSAGE("void_passage", 0.0);

        private final String id;
        private final double radius;

        Type(String id, double radius) {
            this.id = id;
            this.radius = radius;
        }

        public String id() {
            return id;
        }

        public double radius() {
            return radius;
        }

        /** 语言文件键。 */
        public String translationKey() {
            return "ritual.echoesofoblivion." + id;
        }

        public String descriptionKey() {
            return "ritual.echoesofoblivion." + id + ".desc";
        }
    }

    private Rituals() {
    }

    /** 侵蚀值的代价表（虚空通道用）。 */
    public static int corruptionCost(Type type) {
        return switch (type) {
            case VOID_PASSAGE -> 10;
            default -> 0;
        };
    }
}
