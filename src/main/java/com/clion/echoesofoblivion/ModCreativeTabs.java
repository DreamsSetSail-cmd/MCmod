package com.clion.echoesofoblivion;

import com.clion.echoesofoblivion.block.ModBlocks;
import com.clion.echoesofoblivion.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
                // 完整清单由 ModItems.creativeTabContents() 提供，顺序即分类顺序
                // （考古 / 仪式 / 消耗 / 材料 / 工具 / 残片）。
                // 这样加物品时只需要改一处，不会出现「注册了但标签页里没有」。
                for (RegistryObject<Item> item : ModItems.creativeTabContents()) {
                    output.accept(item.get());
                }
            })
            .build());
}