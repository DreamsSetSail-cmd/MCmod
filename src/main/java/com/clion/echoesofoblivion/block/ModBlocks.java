package com.clion.echoesofoblivion.block;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.block.entity.MemoryCrystalBlockEntity;
import com.clion.echoesofoblivion.block.entity.NameSteleBlockEntity;
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

    /**
     * 名字碑（v2.3.0）——玩家可以把自己刻上去，然后承担结果。
     *
     * <p>它是一个**满方块**（与原有的碑身石材同样是一整块），因此外观上
     * 与纪念碑里的其它石头没有区别——这一点是刻意的：你不该一眼看出
     * 「这块能写字」。
     *
     * <p>刻名**不可见**（没有任何渲染路径显示它），代价却真实存在：
     * 侵蚀增长加快，并开始出现关于你的记录式短句。
     * 详见 {@code NameSteleBlock} 与 {@code NameSteleObservation} 的类注释。
     */
    public static final RegistryObject<NameSteleBlock> NAME_STELE =
        BLOCKS.register("name_stele", () -> new NameSteleBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(2.0f)
            .sound(SoundType.DEEPSLATE_BRICKS)));

    /**
     * 空碑（v2.5.0）——「缺席优于出现」的最终形态。
     *
     * <p>击败聚合体、走完清点之后，世界中会出现一块**比原来更多**的碑，
     * 而它是空的。它**不接受任何铭刻**（用空白铭牌右键只会被拒绝）。
     *
     * <p>不解释。你以为你赢了，而有些东西比你赢之前更少。
     */
    public static final RegistryObject<EmptyPlaqueBlock> EMPTY_PLAQUE =
        BLOCKS.register("empty_plaque", () -> new EmptyPlaqueBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(2.0f)
            .sound(SoundType.DEEPSLATE_BRICKS)));

    public static final RegistryObject<BlockEntityType<MemoryCrystalBlockEntity>> MEMORY_CRYSTAL_BE =
        BLOCK_ENTITIES.register("memory_crystal", () ->
            BlockEntityType.Builder.of(MemoryCrystalBlockEntity::new, MEMORY_CRYSTAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<NameSteleBlockEntity>> NAME_STELE_BE =
        BLOCK_ENTITIES.register("name_stele", () ->
            BlockEntityType.Builder.of(NameSteleBlockEntity::new, NAME_STELE.get()).build(null));
}
