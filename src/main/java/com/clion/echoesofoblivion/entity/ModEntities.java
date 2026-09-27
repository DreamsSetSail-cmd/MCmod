package com.clion.echoesofoblivion.entity;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体注册。
 *
 * <p>属性注册通过 {@code @Mod.EventBusSubscriber} 自动挂载到 MOD 总线。
 * 旧代码在 {@code EchoesOfOblivionMod} 构造里手写 {@code modBus.addListener(ModEntities::registerAttributes)}，
 * 一旦有人再给本类加上注解就会双重注册，因此这里统一为注解式。
 */
@Mod.EventBusSubscriber(modid = EchoesOfOblivionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoesOfOblivionMod.MOD_ID);

    public static final RegistryObject<EntityType<SilentAggregateEntity>> SILENT_AGGREGATE = ENTITIES.register(
        "silent_aggregate",
        () -> EntityType.Builder.of(SilentAggregateEntity::new, MobCategory.MONSTER)
            .sized(3.5f, 4.0f)
            .fireImmune()
            .build("silent_aggregate")
    );

    public static final RegistryObject<EntityType<PhantomEntity>> PHANTOM = ENTITIES.register(
        "phantom",
        () -> EntityType.Builder.of(PhantomEntity::new, MobCategory.MONSTER)
            .sized(1.0f, 2.0f)
            .fireImmune()
            .build("phantom")
    );

    /**
     * 镜中的你（v1.2.0）。
     *
     * <p>归入 {@code MISC} 而不是 {@code MONSTER}，因为它不参与刷怪上限与怪物生成逻辑：
     * 它由感染区块的扩散逻辑手动放置，数量应当极少，且绝不能挤占正常生物的名额。
     * 玩家尺寸（0.6 x 1.8），与它「长得和你一样」的设定一致。
     */
    public static final RegistryObject<EntityType<MirrorEntity>> MIRROR = ENTITIES.register(
        "mirror",
        () -> EntityType.Builder.of(MirrorEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build("mirror")
    );

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SILENT_AGGREGATE.get(), Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 800)
            .add(Attributes.FOLLOW_RANGE, 48)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1)
            .build());

        event.put(PHANTOM.get(), Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 50)
            .add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FLYING_SPEED, 0.3)
            .build());

        event.put(MIRROR.get(), MirrorEntity.createAttributes().build());
    }
}