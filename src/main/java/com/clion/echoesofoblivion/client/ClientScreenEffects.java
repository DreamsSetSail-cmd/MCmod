package com.clion.echoesofoblivion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 全屏过渡表现（传送时的视野压制，阶段 2 的地基）。
 *
 * <p>实现方式说明：Forge 在 1.20.5 移除了 {@code RenderGuiEvent} 以及整个覆层体系
 * （{@code IGuiOverlay} / {@code ForgeGui} / {@code RegisterGuiOverlaysEvent}
 * 在 Forge 50.2.10 的 jar 里都不存在）。因此这里用 {@link RenderLevelStageEvent}
 * 的 {@code AFTER_LEVEL} 阶段直接绘制全屏四边形——不依赖任何已删除的 API，
 * 也不需要自定义着色器（对 GT 720 这类老卡友好）。
 */
@Mod.EventBusSubscriber(modid = "echoesofoblivion", value = Dist.CLIENT)
public final class ClientScreenEffects {

    private static final int TRAVEL_FADE_TICKS = 30;

    private static int travelFade;
    private static boolean travelEntering;
    private static boolean travelStable;

    private ClientScreenEffects() {
    }

    public static void startTravelFade(boolean entering, boolean stable) {
        travelFade = TRAVEL_FADE_TICKS;
        travelEntering = entering;
        travelStable = stable;
    }

    public static void clear() {
        travelFade = 0;
    }

    /** 每 tick 递减，由 {@link ClientTickHandler} 调用。 */
    static void tick() {
        if (travelFade > 0) {
            travelFade--;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (travelFade <= 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null) {
            return;
        }

        float progress = travelFade / (float) TRAVEL_FADE_TICKS;
        // 进入走廊偏冷紫，返回主世界偏白——用色相区分「去哪边」
        int rgb = travelEntering ? 0x1A0B26 : 0xE8E8F0;
        float alpha = progress;
        if (!travelStable) {
            // 不稳定传送：亮度闪烁，制造被撕扯的感觉
            alpha *= 0.55f + 0.45f * (float) Math.sin(travelFade * 1.7f);
        }

        // 顶点直接写在相机空间：用当前的 model-view 矩阵变换即可铺满视野，
        // 不依赖事件提供的矩阵类型（不同版本返回值不同）。
        drawFullscreenQuad(new Matrix4f(RenderSystem.getModelViewMatrix()), rgb, alpha);
    }

    /**
     * 在当前相机空间里铺一个覆盖全屏的四边形。
     *
     * <p>顶点写在相机附近（-0.1~0.1）并关闭深度测试，避免被地形裁剪。
     */
    private static void drawFullscreenQuad(Matrix4f matrix, int rgb, float alpha) {
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, -1.0f, -1.0f, -0.1f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, 1.0f, -1.0f, -0.1f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, 1.0f, 1.0f, -0.1f).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, -1.0f, 1.0f, -0.1f).color(red, green, blue, alpha).endVertex();
        // Tesselator.end() 内部就是 BufferUploader.drawWithShader(builder.end())
        Tesselator.getInstance().end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
