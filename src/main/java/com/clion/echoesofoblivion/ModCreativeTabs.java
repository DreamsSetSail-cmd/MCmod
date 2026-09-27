package com.clion.echoesofoblivion;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EchoesOfOblivionMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ECHOES_TAB = TABS.register("echoes_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echoesofoblivion"))
            .icon(() -> new ItemStack(ModItems.MEMORY_CRYSTAL.get()))
            .displayItems((parameters, output) -> {
                // ModItems.MEMORY_CRYSTAL 是它的 BlockItem，无需再单独添加方块本体
                output.accept(ModItems.MEMORY_CRYSTAL.get());
                output.accept(ModItems.CORRIDOR_KEY.get());
                output.accept(ModItems.MEMORY_SCROLL.get());
                output.accept(ModItems.ECHO_WHISPER.get());
                output.accept(ModItems.SHARD_OF_TRUTH.get());
                output.accept(ModItems.VOID_EMBERS.get());
                output.accept(ModItems.CORRUPTION_ESSENCE.get());
                output.accept(ModItems.EYE_OF_SILENCE.get());
            })
            .build());
}