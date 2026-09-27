package com.clion.echoesofoblivion.datagen;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品模型的数据生成（阶段 7）。
 *
 * <p>所有物品都是普通的 2D 图标（{@code item/generated} + 一张贴图），
 * 因此统一走 {@link #basicItem(Item)}——它会生成
 * {@code models/item/<name>.json} 并引用 {@code item/<name>} 纹理。
 *
 * <p>注意：记忆水晶是 {@code BlockItem}，它的物品模型由
 * {@link ModBlockStateProvider#simpleBlockWithItem} 生成，这里必须跳过，
 * 否则两个 provider 会写出同名文件而互相覆盖。
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, EchoesOfOblivionMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        // 记忆水晶由方块 provider 负责，见类注释
        basicItem(ModItems.CORRIDOR_KEY.get());
        basicItem(ModItems.MEMORY_SCROLL.get());
        basicItem(ModItems.ECHO_WHISPER.get());
        basicItem(ModItems.SHARD_OF_TRUTH.get());
        basicItem(ModItems.VOID_EMBERS.get());
        basicItem(ModItems.CORRUPTION_ESSENCE.get());
        basicItem(ModItems.EYE_OF_SILENCE.get());
    }
}
