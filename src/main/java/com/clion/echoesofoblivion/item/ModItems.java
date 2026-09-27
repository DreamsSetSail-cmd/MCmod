package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, EchoesOfOblivionMod.MOD_ID);

    public static final RegistryObject<Item> MEMORY_CRYSTAL = ITEMS.register("memory_crystal",
        () -> new BlockItem(ModBlocks.MEMORY_CRYSTAL.get(), new Properties()));

    /** 走廊之钥：点燃不稳定传送门（阶段 2）。 */
    public static final RegistryObject<Item> CORRIDOR_KEY = ITEMS.register("corridor_key",
        () -> new CorridorKeyItem(new Properties().stacksTo(1)));

    /** 记忆卷轴：右键打开记忆图鉴（v1.1.0 起不再是装饰品）。 */
    public static final RegistryObject<Item> MEMORY_SCROLL = ITEMS.register("memory_scroll",
        () -> new MemoryScrollItem(new Properties().stacksTo(1)));

    public static final RegistryObject<Item> ECHO_WHISPER = ITEMS.register("echo_whisper",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> SHARD_OF_TRUTH = ITEMS.register("shard_of_truth",
        () -> new Item(new Properties().stacksTo(64)));

    public static final RegistryObject<Item> VOID_EMBERS = ITEMS.register("void_embers",
        () -> new Item(new Properties()));

    public static final RegistryObject<Item> CORRUPTION_ESSENCE = ITEMS.register("corruption_essence",
        () -> new Item(new Properties()));

    /** 寂静之眼：在感染区内、见证全部记忆后召唤 Boss（阶段 6）。 */
    public static final RegistryObject<Item> EYE_OF_SILENCE = ITEMS.register("eye_of_silence",
        () -> new EyeOfSilenceItem(new Properties().stacksTo(1)));
}
