package com.clion.echoesofoblivion;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // COMMON 配置
    public static ForgeConfigSpec.IntValue corruptionGainRate;
    public static ForgeConfigSpec.IntValue mirrorExpandInterval;
    public static ForgeConfigSpec.IntValue crystalGenChance;
    public static ForgeConfigSpec.DoubleValue bossStabilityThreshold;
    public static ForgeConfigSpec.BooleanValue enableMirrorChunks;

    // CLIENT 配置
    public static ForgeConfigSpec.DoubleValue overlayIntensity;
    public static ForgeConfigSpec.BooleanValue enableShadowEffects;
    public static ForgeConfigSpec.DoubleValue whisperVolume;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        commonBuilder.comment("General Settings");

        corruptionGainRate = commonBuilder
            .comment("侵蚀值每秒自然增长量 (0=关闭)")
            .defineInRange("corruptionGainRate", 1, 0, 100);

        mirrorExpandInterval = commonBuilder
            .comment("镜像区块扩散间隔(秒)")
            .defineInRange("mirrorExpandInterval", 300, 10, 3600);

        crystalGenChance = commonBuilder
            .comment("水晶生成概率 (1/x)")
            .defineInRange("crystalGenChance", 5, 1, 100);

        bossStabilityThreshold = commonBuilder
            .comment("Boss稳定值阈值")
            .defineInRange("bossStabilityThreshold", 0.0, 0.0, 1.0);

        enableMirrorChunks = commonBuilder
            .comment("是否启用镜像区块机制")
            .define("enableMirrorChunks", true);

        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("Client Settings");

        overlayIntensity = clientBuilder
            .comment("侵蚀Overlay强度系数")
            .defineInRange("overlayIntensity", 1.0, 0.0, 3.0);

        enableShadowEffects = clientBuilder
            .comment("是否启用影子异常效果")
            .define("enableShadowEffects", true);

        whisperVolume = clientBuilder
            .comment("低语音效音量系数")
            .defineInRange("whisperVolume", 0.5, 0.0, 1.0);

        CLIENT_SPEC = clientBuilder.build();
    }
}