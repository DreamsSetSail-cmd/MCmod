package com.clion.echoesofoblivion.memory;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 记忆内容库——**全篇叙事的单一事实来源**。
 *
 * <p>记忆不是随机生成的：ID 必须稳定，否则 {@code memory.echoesofoblivion.<id>.title}
 * 永远匹配不上语言文件中的键。
 *
 * <h2>叙事结构</h2>
 *
 * <p>五段记忆构成一条**因果链**而非并列的悲剧片段。每段声明了与谁共鸣
 * （{@code connectedClues}）以及共鸣时浮现的「因果碎片」（{@code resonanceKey}）：
 *
 * <pre>
 *   bridge(2)      桥的时代：他们用「安静」换取共享思想的能力
 *      ↓
 *   sky(0)         首次触及一个从未被占据的位置，他们以为抵达了完美
 *      ↓
 *   silence(1)     声音逐一消失，而他们起初把这当成奇迹
 *      ↓
 *   apparatus(3)   为自救造出的仪器，其成功恰恰需要腾空周围的声音
 *      ↓
 *   warning(4)     她试图警告，话语在传输途中被改写，变成会回应的东西
 * </pre>
 *
 * <p>关键设计：**单独看每段都只是悲剧，两段共鸣后才浮现因果**。
 * 玩家必须自己排出顺序——这正是非线性解谜的叙事载体。
 * 完整设定见 {@code docs/story-bible.md}。
 */
public final class MemoryRegistry {

    /** 非线性关联线索词——用于共鸣判定。 */
    public static final String CLUE_SKY = "sky";
    public static final String CLUE_SILENCE = "silence";
    public static final String CLUE_BRIDGE = "bridge";
    public static final String CLUE_APPARATUS = "apparatus";
    public static final String CLUE_WARNING = "warning";

    /** 共鸣文本的键前缀。 */
    private static final String RESONANCE = "memory.echoesofoblivion.resonance.";

    private static final List<MemoryEntry> MEMORIES = List.of(
        // 0 —— 陨落：他们触及了一个不该触及的位置。
        //      与「最后的实验」共鸣 → 那道光不是天灾，是试运行。
        //      与「最初的寂静」共鸣 → 第一次庆贺就是替换的第一个证据。
        new MemoryEntry(
            id("memory_0"),
            "memory.echoesofoblivion.memory_0.title",
            "memory.echoesofoblivion.memory_0.content",
            "memory.echoesofoblivion.memory_0.clue",
            CLUE_SKY,
            RESONANCE + "sky_apparatus",
            0x9A6BFF,
            List.of(CLUE_APPARATUS, CLUE_SILENCE)
        ),
        // 1 —— 最初的寂静：替换是渐进的，而且他们起初在庆贺。
        //      与「建筑师」共鸣 → 桥就是寂静的输送管道。
        new MemoryEntry(
            id("memory_1"),
            "memory.echoesofoblivion.memory_1.title",
            "memory.echoesofoblivion.memory_1.content",
            "memory.echoesofoblivion.memory_1.clue",
            CLUE_SILENCE,
            RESONANCE + "silence_bridge",
            0x6FD3E8,
            List.of(CLUE_BRIDGE, CLUE_SKY)
        ),
        // 2 —— 建筑师：他们建的不只是城市，是思想之间的桥。
        //      与「最初的寂静」共鸣 → 桥是双向的，每次跨越都要付出安静。
        new MemoryEntry(
            id("memory_2"),
            "memory.echoesofoblivion.memory_2.title",
            "memory.echoesofoblivion.memory_2.content",
            "memory.echoesofoblivion.memory_2.clue",
            CLUE_BRIDGE,
            RESONANCE + "bridge_silence",
            0xE8C46F,
            List.of(CLUE_SILENCE)
        ),
        // 3 —— 最后的实验：自救手段本身就是扩增手段。
        //      与「警告」共鸣 → 她警告的就是仪器，而仪器改写了警告。
        new MemoryEntry(
            id("memory_3"),
            "memory.echoesofoblivion.memory_3.title",
            "memory.echoesofoblivion.memory_3.content",
            "memory.echoesofoblivion.memory_3.clue",
            CLUE_APPARATUS,
            RESONANCE + "apparatus_warning",
            0xFF7A5C,
            List.of(CLUE_WARNING, CLUE_SKY)
        ),
        // 4 —— 警告：语言本身被污染了，这是最深的伤。
        //      与「最初的寂静」共鸣 → 她说完之后就没有再出声。
        new MemoryEntry(
            id("memory_4"),
            "memory.echoesofoblivion.memory_4.title",
            "memory.echoesofoblivion.memory_4.content",
            "memory.echoesofoblivion.memory_4.clue",
            CLUE_WARNING,
            RESONANCE + "warning_silence",
            0xD45CFF,
            List.of(CLUE_APPARATUS, CLUE_SILENCE)
        )
    );

    private MemoryRegistry() {
    }

    private static ResourceLocation id(String path) {
        return EchoesOfOblivionMod.id(path);
    }

    public static List<MemoryEntry> all() {
        return MEMORIES;
    }

    public static int size() {
        return MEMORIES.size();
    }

    public static MemoryEntry byIndex(int index) {
        if (index < 0 || index >= MEMORIES.size()) {
            throw new IllegalArgumentException("Memory index out of range: " + index);
        }
        return MEMORIES.get(index);
    }

    public static Optional<MemoryEntry> byId(ResourceLocation id) {
        return MEMORIES.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public static Optional<MemoryEntry> byClue(String clue) {
        return MEMORIES.stream().filter(m -> m.clue().equals(clue)).findFirst();
    }

    public static int indexOf(ResourceLocation id) {
        for (int i = 0; i < MEMORIES.size(); i++) {
            if (MEMORIES.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
