package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.Config;
import com.clion.echoesofoblivion.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 侵蚀值的视觉反馈（阶段 4）。
 *
 * <p>Forge 1.20.6 移除了 GUI 覆层体系，所以「高侵蚀压迫感」改用两个更合适的手段：
 * <ol>
 *   <li><b>雾</b>：侵蚀越高，雾越近、颜色越冷越脏（{@link ViewportEvent}）。
 *       这比贴一张半透明全屏图更有沉浸感，也让走廊的「看不见尽头」成为机制。</li>
 *   <li><b>视野扭曲的替代</b>：{@link ClientScreenEffects} 中的闪烁压制复用于此。</li>
 * </ol>
 *
 * <p>所有表现只读 {@link ClientData} 的同步值，客户端不自行推算侵蚀。
 */
@Mod.EventBusSubscriber(modid = "echoesofoblivion", value = Dist.CLIENT)
public final class CorruptionVisuals {

    /** 侵蚀超过该值才开始明显起雾。 */
    private static final float FOG_ONSET = 0.35f;

    /** 雾最浓时的可见距离（格）。 */
    private static final float MIN_FOG_DISTANCE = 12.0f;

    private CorruptionVisuals() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float ratio = effectiveRatio();
        if (ratio <= 0.0f) {
            return;
        }
        // 向暗紫红偏移：被污染的世界连空气都是脏的
        float red = event.getRed() * (1.0f - ratio) + 0.10f * ratio;
        float green = event.getGreen() * (1.0f - ratio) + 0.02f * ratio;
        float blue = event.getBlue() * (1.0f - ratio) + 0.06f * ratio;
        event.setRed(red);
        event.setGreen(green);
        event.setBlue(blue);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float ratio = effectiveRatio();
        if (ratio <= 0.0f) {
            return;
        }
        // 侵蚀越高，雾从远处一路压到脸上
        float near = event.getNearPlaneDistance();
        float far = event.getFarPlaneDistance();
        float factor = Math.min(1.0f, ratio);

        float newFar = Math.max(MIN_FOG_DISTANCE, far * (1.0f - factor * 0.8f));
        float newNear = Math.min(near, newFar * 0.25f);
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(newNear);
    }

    /**
     * 侵蚀超过 {@link #FOG_ONSET} 之后才开始生效，避免刚开局就视野受阻。
     * 同时受客户端配置的强度系数控制。
     */
    private static float effectiveRatio() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return 0.0f;
        }
        float raw = ClientData.getCorruptionRatio();
        if (raw <= FOG_ONSET) {
            return 0.0f;
        }
        float normalized = (raw - FOG_ONSET) / (1.0f - FOG_ONSET);
        double intensity = ConfigHelper.getDouble(Config.overlayIntensity, 1.0);
        return (float) Math.max(0.0, Math.min(1.0, normalized * intensity));
    }
}
