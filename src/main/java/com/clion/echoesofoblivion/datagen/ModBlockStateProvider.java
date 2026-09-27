package com.clion.echoesofoblivion.datagen;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 方块状态与方块模型的数据生成（阶段 7）。
 *
 * <p>等价替换原先手写的 {@code blockstates/*.json} 与 {@code models/block/*.json}，
 * 但改为由代码推导，避免将来加方块时忘记补 JSON。
 */
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, EchoesOfOblivionMod.MOD_ID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 记忆水晶：cube_all + block/memory_crystal 纹理，并同时生成物品模型
        simpleBlockWithItem(ModBlocks.MEMORY_CRYSTAL.get(), cubeAll(ModBlocks.MEMORY_CRYSTAL.get()));

        // 残片龛（v2.0.0）：同为 cube_all 的满方块，纹理 textures/block/fragment_niche.png
        simpleBlockWithItem(ModBlocks.FRAGMENT_NICHE.get(), cubeAll(ModBlocks.FRAGMENT_NICHE.get()));

        // 不稳定传送门：方块本身不可见（RenderShape.INVISIBLE），因此引用原版空气模型，
        // 保证缺少 blockstate 时不会出现「模型缺失」的紫黑格
        invisibleBlock(ModBlocks.UNSTABLE_PORTAL.get());
    }

    /** 生成一个指向 {@code minecraft:block/air} 的 blockstate。 */
    private void invisibleBlock(Block block) {
        ModelFile air = models().getExistingFile(ResourceLocation.withDefaultNamespace("block/air"));
        simpleBlock(block, air);
    }
}
