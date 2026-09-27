package com.clion.echoesofoblivion.sound;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EchoesOfOblivionMod.MOD_ID);

    public static final RegistryObject<SoundEvent> MEMORY_COLLECT = register("memory_collect");
    public static final RegistryObject<SoundEvent> WHISPER_AMBIENT = register("whisper_ambient");
    public static final RegistryObject<SoundEvent> HEARTBEAT = register("heartbeat");
    public static final RegistryObject<SoundEvent> CORRUPTION_PULSE = register("corruption_pulse");
    public static final RegistryObject<SoundEvent> BOSS_SUMMON = register("boss_summon");
    public static final RegistryObject<SoundEvent> BOSS_HIT = register("boss_hit");
    public static final RegistryObject<SoundEvent> BOSS_DEATH = register("boss_death");
    public static final RegistryObject<SoundEvent> PORTAL_OPEN = register("portal_open");
    public static final RegistryObject<SoundEvent> PORTAL_TRAVEL = register("portal_travel");
    public static final RegistryObject<SoundEvent> MEMORY_VISION = register("memory_vision");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(EchoesOfOblivionMod.MOD_ID, name)));
    }
}