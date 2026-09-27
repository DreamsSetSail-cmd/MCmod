package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.entity.MemoryCrystalBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, EchoesOfOblivionMod.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EchoesOfOblivionMod.MOD_ID);

    /** 记忆水晶（阶段 1）：微光、可交互。 */
    public static final RegistryObject<MemoryCrystalBlock> MEMORY_CRYSTAL = BLOCKS.register("memory_crystal",
        () -> new MemoryCrystalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(0.5f)
            .sound(SoundType.AMETHYST)
            .noOcclusion()
            .lightLevel(state -> 7)));

    /**
     * 不稳定传送门（阶段 2）：无碰撞、可穿过，实体进入即传送。
     * {@code noLootTable()} 是必须的——传送门方块不应掉落自身。
     */
    public static final RegistryObject<UnstablePortalBlock> UNSTABLE_PORTAL =
        BLOCKS.register("unstable_portal", () -> new UnstablePortalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(-1.0f, 3600000.0f)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 11)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)));

    /**
     * 残片龛（v2.0.0）：12 段残片在世界里的唯一来源。
     *
     * <p>刻意**不发光**（没有 {@code lightLevel}）。它是空的——槽底那点紫只是
     * 贴图里画的余辉，不是真的光源。一个会照亮周围的空槽会显得「这里还有东西」，
     * 而它要传达的恰恰是**这里的东西很久以前就被拿走了**。
     *
     * <p>{@code noLootTable()}：破坏它不应该把「龛」本身变成可刷的资源。
     * 它是一次性的历史遗留物，不是一个可以搬回家的家具。
     */
    public static final RegistryObject<FragmentNicheBlock> FRAGMENT_NICHE =
        BLOCKS.register("fragment_niche", () -> new FragmentNicheBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(1.5f)
            .sound(SoundType.DEEPSLATE_BRICKS)
            .noOcclusion()
            .noLootTable()));

    public static final RegistryObject<BlockEntityType<MemoryCrystalBlockEntity>> MEMORY_CRYSTAL_BE =
        BLOCK_ENTITIES.register("memory_crystal", () ->
            BlockEntityType.Builder.of(MemoryCrystalBlockEntity::new, MEMORY_CRYSTAL.get()).build(null));
}
