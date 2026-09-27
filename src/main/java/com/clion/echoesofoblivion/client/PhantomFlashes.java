package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.Config;
import com.clion.echoesofoblivion.ConfigHelper;
import com.clion.echoesofoblivion.entity.ModEntities;
import com.clion.echoesofoblivion.entity.PhantomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 幻影闪现（阶段 4 第 4 条）。
 *
 * <p>在玩家视野侧后方随机生成<b>纯客户端</b>的亡魂实体，停留一两秒后消失。
 * 侵蚀越高出现越频繁。因为只在客户端生成，它不会攻击玩家、也不会写进存档——
 * 「你看错了」正是这里想制造的体验。
 */
@Mod.EventBusSubscriber(modid = "echoesofoblivion", value = Dist.CLIENT)
public final class PhantomFlashes {

    /** 侵蚀低于该比例不出现幻影。 */
    private static final float ONSET = 0.30f;

    /** 幻影存活时间（tick）。 */
    private static final int LIFETIME = 30;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int cooldown;
    private static int activeTicks;
    private static PhantomEntity active;

    private PhantomFlashes() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clearActive();
            return;
        }

        // 已有幻影：计时到期后移除
        if (active != null) {
            activeTicks++;
            if (activeTicks >= LIFETIME || active.isRemoved()) {
                clearActive();
            }
            return;
        }

        if (!ConfigHelper.getBoolean(Config.enableShadowEffects, true)) {
            return;
        }

        float ratio = ClientData.getCorruptionRatio();
        if (ratio < ONSET) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 侵蚀越高，间隔越短
        int interval = (int) (240 * (1.0f - ratio) + 120 * ratio);
        cooldown = interval + RANDOM.nextInt(60);
        spawnFlash(minecraft, player);
    }

    /** 在玩家侧后方生成一次幻影。 */
    private static void spawnFlash(Minecraft minecraft, Player player) {
        if (minecraft.level == null) {
            return;
        }
        PhantomEntity phantom = ModEntities.PHANTOM.get().create(minecraft.level);
        if (phantom == null) {
            return;
        }

        // 侧后方 90°±60°，距离 6~12 格：制造「余光瞥见」的感觉
        double angle = player.getYRot() + 90.0 + (RANDOM.nextDouble() - 0.5) * 120.0;
        double distance = 6.0 + RANDOM.nextDouble() * 6.0;
        double radians = Math.toRadians(angle);
        double x = player.getX() + Math.cos(radians) * distance;
        double z = player.getZ() + Math.sin(radians) * distance;
        double y = Math.max(player.getY() + (RANDOM.nextDouble() - 0.3) * 2.0,
            minecraft.level.getMinBuildHeight() + 2);

        phantom.moveTo(x, y, z, (float) (RANDOM.nextDouble() * 360.0), 0.0f);
        phantom.setDeltaMovement(Vec3.ZERO);
        phantom.setNoAi(true);
        phantom.setInvulnerable(true);
        phantom.setSilent(true);

        minecraft.level.addEntity(phantom);
        active = phantom;
        activeTicks = 0;
    }

    private static void clearActive() {
        if (active != null) {
            active.discard();
            active = null;
        }
        activeTicks = 0;
    }
}
