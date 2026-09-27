package com.clion.echoesofoblivion.memory;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 记忆内容库（阶段 3 的「编写记忆内容库」）。
 *
 * <p>记忆不再随机生成：ID 必须稳定，否则 {@code memory.echoesofoblivion.<id>.title}
 * 永远匹配不上 zh_cn/en_us 中的翻译键。此处定义 5 段固定记忆及其非线性关联线索。
 */
public final class MemoryRegistry {

    /** 非线性关联线索词——用于阶段 3 的谜题判定。 */
    public static final String CLUE_SKY = "sky";
    public static final String CLUE_SILENCE = "silence";
    public static final String CLUE_BRIDGE = "bridge";
    public static final String CLUE_APPARATUS = "apparatus";
    public static final String CLUE_WARNING = "warning";

    private static final List<MemoryEntry> MEMORIES = List.of(
        new MemoryEntry(
            id("memory_0"),
            "memory.echoesofoblivion.memory_0.title",
            "memory.echoesofoblivion.memory_0.content",
            "memory.echoesofoblivion.memory_0.clue",
            CLUE_SKY,
            0x9A6BFF,
            List.of(CLUE_APPARATUS)
        ),
        new MemoryEntry(
            id("memory_1"),
            "memory.echoesofoblivion.memory_1.title",
            "memory.echoesofoblivion.memory_1.content",
            "memory.echoesofoblivion.memory_1.clue",
            CLUE_SILENCE,
            0x6FD3E8,
            List.of(CLUE_SKY, CLUE_BRIDGE)
        ),
        new MemoryEntry(
            id("memory_2"),
            "memory.echoesofoblivion.memory_2.title",
            "memory.echoesofoblivion.memory_2.content",
            "memory.echoesofoblivion.memory_2.clue",
            CLUE_BRIDGE,
            0xE8C46F,
            List.of(CLUE_SILENCE)
        ),
        new MemoryEntry(
            id("memory_3"),
            "memory.echoesofoblivion.memory_3.title",
            "memory.echoesofoblivion.memory_3.content",
            "memory.echoesofoblivion.memory_3.clue",
            CLUE_APPARATUS,
            0xFF7A5C,
            List.of(CLUE_WARNING, CLUE_SKY)
        ),
        new MemoryEntry(
            id("memory_4"),
            "memory.echoesofoblivion.memory_4.title",
            "memory.echoesofoblivion.memory_4.content",
            "memory.echoesofoblivion.memory_4.clue",
            CLUE_WARNING,
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
