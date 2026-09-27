package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物品注册表（v2.0.0 扩充）。
 *
 * <p>v1.x 只有 9 件物品，其中 5 件没有实际功能。这一版扩到 35 件，分五类，
 * <b>每一件都有用途</b>：
 *
 * <table>
 *   <tr><th>类别</th><th>作用</th></tr>
 *   <tr><td>考古</td><td>找到并阅读内容：罗盘、图鉴、残片、铭文、探查铲</td></tr>
 *   <tr><td>仪式</td><td>召唤与开启：走廊之钥、寂静之眼、共鸣音叉、仪式合金</td></tr>
 *   <tr><td>消耗</td><td>可以真的改变世界：净化剂、稳定剂、记忆之瓶、腐蚀精粹</td></tr>
 *   <tr><td>材料</td><td>合成中间物：12 种，给合成表提供深度</td></tr>
 *   <tr><td>工具</td><td>战斗与辅助：记忆之刃、碎晶法杖、研究者提灯</td></tr>
 * </table>
 *
 * <h2>为什么残片也是物品</h2>
 *
 * <p>12 段残片做成物品而不是方块，理由是它们要能**被带走、被收集、被重读**。
 * 如果做成地上的方块，玩家读完就走，无法回头对照——而这段历史的价值恰恰在于
 * 前后互相印证（例如先读实验记录，再读私人信件，会得到完全不同的感受）。
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, EchoesOfOblivionMod.MOD_ID);

    /** 残片 ID → 物品，供图鉴与成就系统查询。 */
    private static final Map<String, RegistryObject<Item>> FRAGMENTS = new LinkedHashMap<>();

    // ================================================================ 考古

    public static final RegistryObject<Item> MEMORY_CRYSTAL = ITEMS.register("memory_crystal",
        () -> new BlockItem(ModBlocks.MEMORY_CRYSTAL.get(), new Properties()));

    /** 残片龛：12 段残片在世界里的唯一来源（v2.0.0）。 */
    public static final RegistryObject<Item> FRAGMENT_NICHE = ITEMS.register("fragment_niche",
        () -> new BlockItem(ModBlocks.FRAGMENT_NICHE.get(), new Properties()));

    /** 记忆卷轴：右键打开记忆图鉴。 */
    public static final RegistryObject<Item> MEMORY_SCROLL = ITEMS.register("memory_scroll",
        () -> new MemoryScrollItem(new Properties().stacksTo(1)));

    /** 遗迹罗盘：指向最近的记忆水晶。 */
    public static final RegistryObject<Item> RUINS_COMPASS = ITEMS.register("ruins_compass",
        () -> new RuinsCompassItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** 探查铲：从记忆水晶上取样水晶尘，并开启被封住的残片。 */
    public static final RegistryObject<Item> EXCAVATION_SHOVEL = ITEMS.register("excavation_shovel",
        () -> new ExcavationShovelItem(new Properties().stacksTo(1).durability(256)));

    // ================================================================ 仪式

    /** 走廊之钥：点燃不稳定传送门。 */
    public static final RegistryObject<Item> CORRIDOR_KEY = ITEMS.register("corridor_key",
        () -> new CorridorKeyItem(new Properties().stacksTo(1)));

    /** 寂静之眼：满足三条件后召唤聚合体。 */
    public static final RegistryObject<Item> EYE_OF_SILENCE = ITEMS.register("eye_of_silence",
        () -> new EyeOfSilenceItem(new Properties().stacksTo(1)));

    /** 共鸣音叉：发起三种仪式（v2.0.0）。 */
    public static final RegistryObject<Item> RESONANCE_FORK = ITEMS.register("resonance_fork",
        () -> new ResonanceForkItem(new Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** 仪式合金：仪式类物品的基础材料。 */
    public static final RegistryObject<Item> RITUAL_ALLOY = ITEMS.register("ritual_alloy",
        () -> new Item(new Properties()));

    /** 寂静碎片：聚合体与仪式的核心材料。 */
    public static final RegistryObject<Item> SILENCE_SHARD = ITEMS.register("silence_shard",
        () -> new Item(new Properties().rarity(Rarity.UNCOMMON)));

    // ================================================================ 消耗

    /** 净化剂：清除所在区块 3x3 范围内的感染。 */
    public static final RegistryObject<Item> PURIFICATION_AGENT = ITEMS.register("purification_agent",
        () -> new PurificationAgentItem(new Properties().stacksTo(16)));

    /** 稳定剂：注射后 90 秒内免疫重力闪烁（v2.0.0）。 */
    public static final RegistryObject<Item> STABILIZER = ITEMS.register("stabilizer",
        () -> new StabilizerItem(new Properties().stacksTo(16)));

    /** 记忆之瓶：降低 25 点侵蚀值，代价是烧掉最近见证的一段记忆。 */
    public static final RegistryObject<Item> MEMORY_VIAL = ITEMS.register("memory_vial",
        () -> new MemoryVialItem(new Properties().stacksTo(8)));

    public static final RegistryObject<Item> CORRUPTION_ESSENCE = ITEMS.register("corruption_essence",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> ECHO_WHISPER = ITEMS.register("echo_whisper",
        () -> new Item(new Properties()));

    // ================================================================ 材料

    public static final RegistryObject<Item> SHARD_OF_TRUTH = ITEMS.register("shard_of_truth",
        () -> new Item(new Properties().stacksTo(64)));

    public static final RegistryObject<Item> VOID_EMBERS = ITEMS.register("void_embers",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> CRYSTAL_DUST = ITEMS.register("crystal_dust",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> RESONANT_ALLOY = ITEMS.register("resonant_alloy",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> CORRUPTED_FRAGMENT = ITEMS.register("corrupted_fragment",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> MIRROR_SHARD = ITEMS.register("mirror_shard",
        () -> new Item(new Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> MEMBRANE = ITEMS.register("membrane",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> ARCHIVIST_INK = ITEMS.register("archivist_ink",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> OSSUARY_ASH = ITEMS.register("ossuary_ash",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> THREAD_OF_CHOIR = ITEMS.register("thread_of_choir",
        () -> new Item(new Properties().rarity(Rarity.UNCOMMON)));

    /** 空白的铭牌：写上名字后仍是空白的（叙事道具）。 */
    public static final RegistryObject<Item> BLANK_PLAQUE = ITEMS.register("blank_plaque",
        () -> new Item(new Properties().stacksTo(16)));

    /** 被封住的残片：需要使用探查铲开启。 */
    public static final RegistryObject<Item> SEALED_FRAGMENT = ITEMS.register("sealed_fragment",
        () -> new Item(new Properties().stacksTo(16)));

    // ================================================================ 工具

    /** 记忆之刃：对亡魂有效，对聚合体无效（强化「武器无用」的设定对比）。 */
    public static final RegistryObject<Item> MEMORY_BLADE = ITEMS.register("memory_blade",
        () -> new MemoryBladeItem(new Properties().stacksTo(1).durability(512).rarity(Rarity.UNCOMMON)));

    /** 碎晶法杖：右键射出反粒子，驱散落点范围内的亡魂。 */
    public static final RegistryObject<Item> SHATTER_STAFF = ITEMS.register("shatter_staff",
        () -> new ShatterStaffItem(new Properties().stacksTo(1).durability(256).rarity(Rarity.RARE)));

    /** 研究者提灯：持有期间侵蚀增长减半。 */
    public static final RegistryObject<Item> RESEARCHER_LANTERN = ITEMS.register("researcher_lantern",
        () -> new ResearcherLanternItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ================================================================ 残片（12 段）

    public static final RegistryObject<Item> FRAGMENT_FOUNDING =
        fragment("founding", LoreFragmentItem.Kind.CHRONICLE);
    public static final RegistryObject<Item> FRAGMENT_FIRST_BRIDGE =
        fragment("first_bridge", LoreFragmentItem.Kind.CHRONICLE);
    public static final RegistryObject<Item> FRAGMENT_CHOIR_RECORD =
        fragment("choir_record", LoreFragmentItem.Kind.LOG);
    public static final RegistryObject<Item> FRAGMENT_COST_LEDGER =
        fragment("cost_ledger", LoreFragmentItem.Kind.LOG);
    public static final RegistryObject<Item> FRAGMENT_LETTER_HOME =
        fragment("letter_home", LoreFragmentItem.Kind.LETTER);
    public static final RegistryObject<Item> FRAGMENT_HER_FIRST_NOTE =
        fragment("her_first_note", LoreFragmentItem.Kind.LETTER);
    public static final RegistryObject<Item> FRAGMENT_HER_LAST_NOTE =
        fragment("her_last_note", LoreFragmentItem.Kind.LETTER);
    public static final RegistryObject<Item> FRAGMENT_APPARATUS_SPEC =
        fragment("apparatus_spec", LoreFragmentItem.Kind.LOG);
    public static final RegistryObject<Item> FRAGMENT_THE_QUIET_ORDER =
        fragment("the_quiet_order", LoreFragmentItem.Kind.CHRONICLE);
    public static final RegistryObject<Item> FRAGMENT_AFTER =
        fragment("after", LoreFragmentItem.Kind.CHRONICLE);
    public static final RegistryObject<Item> FRAGMENT_PRAYER_FIRST =
        fragment("prayer_first", LoreFragmentItem.Kind.PRAYER);
    public static final RegistryObject<Item> FRAGMENT_PRAYER_LAST =
        fragment("prayer_last", LoreFragmentItem.Kind.PRAYER);

    private static RegistryObject<Item> fragment(String id, LoreFragmentItem.Kind kind) {
        RegistryObject<Item> item = ITEMS.register("fragment_" + id,
            () -> new LoreFragmentItem(id, kind, new Properties().stacksTo(1)));
        FRAGMENTS.put(id, item);
        return item;
    }

    /** 只读的残片清单，供图鉴与成就查询。 */
    public static Map<String, RegistryObject<Item>> fragments() {
        return Collections.unmodifiableMap(FRAGMENTS);
    }

    /** 创造模式标签页用的完整物品清单（顺序即展示顺序）。 */
    public static RegistryObject<Item>[] creativeTabContents() {
        @SuppressWarnings("unchecked")
        RegistryObject<Item>[] all = new RegistryObject[]{
            // 考古
            MEMORY_CRYSTAL, FRAGMENT_NICHE, MEMORY_SCROLL, RUINS_COMPASS, EXCAVATION_SHOVEL,
            // 仪式
            CORRIDOR_KEY, EYE_OF_SILENCE, RESONANCE_FORK, RITUAL_ALLOY, SILENCE_SHARD,
            // 消耗
            PURIFICATION_AGENT, STABILIZER, MEMORY_VIAL, CORRUPTION_ESSENCE, ECHO_WHISPER,
            // 材料
            SHARD_OF_TRUTH, VOID_EMBERS, CRYSTAL_DUST, RESONANT_ALLOY, CORRUPTED_FRAGMENT,
            MIRROR_SHARD, MEMBRANE, ARCHIVIST_INK, OSSUARY_ASH, THREAD_OF_CHOIR,
            BLANK_PLAQUE, SEALED_FRAGMENT,
            // 工具
            MEMORY_BLADE, SHATTER_STAFF, RESEARCHER_LANTERN,
            // 残片
            FRAGMENT_FOUNDING, FRAGMENT_FIRST_BRIDGE, FRAGMENT_CHOIR_RECORD,
            FRAGMENT_COST_LEDGER, FRAGMENT_LETTER_HOME, FRAGMENT_HER_FIRST_NOTE,
            FRAGMENT_HER_LAST_NOTE, FRAGMENT_APPARATUS_SPEC, FRAGMENT_THE_QUIET_ORDER,
            FRAGMENT_AFTER, FRAGMENT_PRAYER_FIRST, FRAGMENT_PRAYER_LAST
        };
        return all;
    }
}
