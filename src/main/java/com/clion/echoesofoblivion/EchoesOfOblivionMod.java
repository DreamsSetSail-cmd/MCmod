package com.clion.echoesofoblivion;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.item.ModItems;
import com.clion.echoesofoblivion.memory.PlayerMemoryData;
import com.clion.echoesofoblivion.network.ModNetwork;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.ModWorldGen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoesOfOblivionMod.MOD_ID)
public class EchoesOfOblivionMod {
    public static final String MOD_ID = "echoesofoblivion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoesOfOblivionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModWorldGen.register(modBus);

        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CorruptionEventHandler());
        // 阶段 5：镜像区块扩散、重力闪烁、感染区亡魂生成
        MinecraftForge.EVENT_BUS.register(new MirrorChunkHandler());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        ModNetwork.register();

        LOGGER.info("Echoes of Oblivion - the silence is here");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Echoes of Oblivion common setup complete");
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PlayerMemoryData.get(serverLevel);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}