package com.clion.echoesofoblivion.memory;

import java.util.List;

/**
 * 矛盾碎片（v2.2.0）——**按错误顺序见证记忆时，幻境里多出互相矛盾的碎片**。
 *
 * <h2>为什么只为三对写反馈</h2>
 *
 * <p>五段记忆有 120 种见证顺序。为全部排列写文案是不值得的（backlog 早已判定），
 * 而更重要的是：**大多数逆序并没有意义**。先看到「建筑师」再看到「陨落」
 * 和反过来看，读起来只是不同的入场角度，不是矛盾。
 *
 * <p>只有少数几对存在真正的**信息落差**——你知道了一个结论，
 * 再从后面回头看它的原因，那两句正文会互相顶住。这一版只为这几对写：
 *
 * <table>
 *   <tr><th>顺序</th><th>为什么这一对会矛盾</th></tr>
 *   <tr><td>先 4（警告）再 3（最后的实验）</td>
 *       <td>最自然的「剧透自己」：先听到那句没人听进去的警告，
 *           再倒着看它为什么没被听进去。</td></tr>
 *   <tr><td>先 4（警告）再 0（陨落）</td>
 *       <td>先知道有人试图阻止，再看到阻止失败的那一刻。</td></tr>
 *   <tr><td>先 3（最后的实验）再 1（最初的寂静）</td>
 *       <td>先看到仪器，再看到它本该解决的那个问题——
 *           于是它的成功本身就是它的失败。</td></tr>
 * </table>
 *
 * <h2>它不是惩罚</h2>
 *
 * <p>矛盾碎片**不扣任何东西**，不降侵蚀、不给负面效果。它只是让玩家在幻境里
 * 多读到一句与自己已知内容冲突的话。惩罚会把它变成一个需要规避的机制；
 * 而它应当只是**非线性叙事的后果**——你选择了一个顺序，于是你承担了这个顺序。
 *
 * <h2>它必须像正文，不像系统提示</h2>
 *
 * <p>语言键放在 {@code message.*} 下，但**渲染在正文之后**，使用与正文相同的
 * 字体与抖动，只有颜色略暗。如果它看起来像一个 UI 提示
 * （「你收集顺序有误！」），整个效果就毁了。
 */
public final class Contradictions {

    /**
     * 一对会互相矛盾的见证顺序。
     *
     * @param earlier  应当**后**被见证的那段（也就是玩家过早看到的那段）
     * @param later    应当**先**被见证的那段
     * @param key      矛盾碎片的语言键
     */
    public record Reversal(int earlier, int later, String key) {
    }

    /**
     * 全部会触发矛盾碎片的逆序对。
     *
     * <p>列表顺序即优先级：{@link #forPlayer} 返回**第一个**命中的那一对。
     * 同时命中多对时只显示一句——幻境里堆三句矛盾会让它从「不安」变成「吵闹」。
     */
    public static final List<Reversal> REVERSALS = List.of(
        // 最优先：先听到警告，再看那场没听警告的实验
        new Reversal(4, 3, "message.echoesofoblivion.contradiction.warning_before_apparatus"),
        // 其次：先听到警告，再看到陨落
        new Reversal(4, 0, "message.echoesofoblivion.contradiction.warning_before_fall"),
        // 最后：先看到仪器，再看到它本该解决的问题
        new Reversal(3, 1, "message.echoesofoblivion.contradiction.apparatus_before_silence")
    );

    private Contradictions() {
    }

    /**
     * 查出这名玩家当前进度下应当显示哪一句矛盾碎片。
     *
     * @return 语言键；没有命中任何一对时返回 {@code null}
     */
    public static String forPlayer(PlayerProgress progress) {
        if (progress == null) {
            return null;
        }
        for (Reversal reversal : REVERSALS) {
            // 语义：earlier 本应更晚被见证（它在叙事上是「后来的事」），
            // 所以当它**先于** later 出现时，顺序就是错的那个方向。
            if (progress.witnessedBefore(reversal.earlier(), reversal.later())) {
                return reversal.key();
            }
        }
        return null;
    }

    /** 供测试与文档使用：这个键是否确实是已登记的逆序对。 */
    public static boolean isReversalKey(String key) {
        return REVERSALS.stream().anyMatch(r -> r.key().equals(key));
    }
}
