package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Key mapping registered via Forge registry
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SILENT_AGGREGATE.get(),
            SilentAggregateRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM.get(),
            PhantomRenderer::new);
        // v1.2.0：不透明渲染，与上面两个的加法混合刻意相反（见 MirrorRenderer 注释）
        event.registerEntityRenderer(ModEntities.MIRROR.get(),
            MirrorRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyBindings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.MEMORY_ATTACK);
    }
}