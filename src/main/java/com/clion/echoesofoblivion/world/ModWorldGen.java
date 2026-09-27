package com.clion.echoesofoblivion.world;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 世界生成注册入口。
 *
 * <p>维度、维度类型、群系、地物配置、结构集这些在 1.20.6 里都是数据驱动，
 * 通过 {@code data/echoesofoblivion/} 下的 JSON 声明；这里只注册<b>需要 Java 实现</b>
 * 的那个地物类型（{@link RuinPileFeature}）。
 */
public class ModWorldGen {

    public static final ResourceKey<Level> SILENT_CORRIDOR =
        ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(EchoesOfOblivionMod.MOD_ID, "silent_corridor"));

    public static final ResourceKey<Biome> SILENT_BIOME =
        ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(EchoesOfOblivionMod.MOD_ID, "silent_biome"));

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(ForgeRegistries.FEATURES, EchoesOfOblivionMod.MOD_ID);

    /** 走廊废墟：随机散布的断墙、碎砖与残柱。 */
    public static final RegistryObject<RuinPileFeature> RUIN_PILE = FEATURES.register("ruin_pile",
        () -> new RuinPileFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
