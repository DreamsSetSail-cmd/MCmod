package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.entity.SilentAggregateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * 寂静聚合体渲染器（阶段 7 修复：实体可见性）。
 *
 * <p>旧实现继承 {@code EntityRenderer} 却只做变换、不绘制几何体，Boss 完全不可见。
 * 这里自绘一个「无固定形态的聚合体」：
 * <ul>
 *   <li>核心：三个不同轴向自转的暗色立方体互相穿插，形成拼合体的观感；</li>
 *   <li>内层发光核：阶段越高越亮；</li>
 *   <li>符文环：两圈反向旋转的小方块，阶段 2 开始明灭；</li>
 *   <li>不稳定：阶段越高抖动越强。</li>
 * </ul>
 *
 * <p>不依赖贴图，使用 {@link RenderType#lightning()}（加法混合、无贴图采样）。
 */
public class SilentAggregateRenderer extends EntityRenderer<SilentAggregateEntity> {

    /** 核心暗色。 */
    private static final int CORE_COLOR = 0x2A0A3A;

    /** 第二层核心。 */
    private static final int CORE_INNER = 0x4A1A6A;

    /** 环与高光的紫调。 */
    private static final int RING_COLOR = 0x9A5CFF;
    private static final int ACCENT_COLOR = 0xD9A0FF;

    private static final ResourceLocation NO_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** 抖动随机源，避免每帧新建。 */
    private final RandomSource jitter = RandomSource.create();

    public SilentAggregateRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.6f;
        this.shadowStrength = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(SilentAggregateEntity entity) {
        return NO_TEXTURE;
    }

    @Override
    public void render(SilentAggregateEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = entity.tickCount + partialTick;
        int phase = entity.getPhase();

        poseStack.pushPose();
        // 整体悬停在两格高——聚合体不该贴着地面
        poseStack.translate(0.0f, 2.2f, 0.0f);

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());

        // 阶段越高越不稳定
        float instability = 0.04f + phase * 0.05f;
        float jx = (jitter.nextFloat() - 0.5f) * instability;
        float jy = (jitter.nextFloat() - 0.5f) * instability;
        float jz = (jitter.nextFloat() - 0.5f) * instability;

        // ---------------------------------------------------------- 核心
        float coreSpin = time * (1.2f + phase * 0.6f);

        drawRotatingBox(poseStack, consumer, jx, jy, jz, 1.7f,
            com.mojang.math.Axis.YP.rotationDegrees(coreSpin),
            com.mojang.math.Axis.XP.rotationDegrees(coreSpin * 0.7f),
            ShapeRenderer.withAlpha(CORE_COLOR, 0.92f));

        drawRotatingBox(poseStack, consumer, jx, jy, jz, 1.25f,
            com.mojang.math.Axis.XP.rotationDegrees(-coreSpin * 0.9f),
            com.mojang.math.Axis.ZP.rotationDegrees(coreSpin * 0.5f),
            ShapeRenderer.withAlpha(CORE_INNER, 0.85f));

        // 内层发光核：阶段越高越亮
        drawRotatingBox(poseStack, consumer, jx * 0.5f, jy * 0.5f, jz * 0.5f, 0.7f,
            com.mojang.math.Axis.YP.rotationDegrees(-coreSpin * 1.8f),
            com.mojang.math.Axis.ZP.rotationDegrees(coreSpin),
            ShapeRenderer.withAlpha(ACCENT_COLOR, 0.5f + phase * 0.2f));

        // ---------------------------------------------------------- 符文环
        drawRing(poseStack, consumer, time, phase, 3.1f, 10, 0.22f, RING_COLOR, 1.0f, jx, jy, jz);
        drawRing(poseStack, consumer, time, phase, 2.2f, 7, 0.16f, ACCENT_COLOR, -1.4f, jx, jy, jz);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void drawRotatingBox(PoseStack poseStack, VertexConsumer consumer,
                                 float jx, float jy, float jz, float size,
                                 Quaternionf first, Quaternionf second, int argb) {
        poseStack.pushPose();
        poseStack.translate(jx, jy, jz);
        poseStack.mulPose(first);
        poseStack.mulPose(second);
        ShapeRenderer.drawBox(poseStack.last(), consumer, size, size, size, LightTexture.FULL_BRIGHT, argb);
        poseStack.popPose();
    }

    /**
     * 绘制一圈小方块。
     *
     * @param spinSpeed 每 tick 的旋转系数；负值表示反向
     */
    private void drawRing(PoseStack poseStack, VertexConsumer consumer, float time, int phase,
                          float radius, int count, float blockSize, int color,
                          float spinSpeed, float jx, float jy, float jz) {
        float spin = time * spinSpeed * 4.0f;
        float bob = Mth.sin(time * 0.06f) * 0.15f;
        // 阶段 2 开始明灭
        float alpha = phase >= 2 ? 0.6f + 0.4f * Mth.sin(time * 0.25f) : 1.0f;

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(spin + i * (360.0 / count));
            poseStack.pushPose();
            poseStack.translate(
                Math.cos(angle) * radius + jx,
                bob + jy,
                Math.sin(angle) * radius + jz);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) Math.toDegrees(angle)));
            ShapeRenderer.drawBox(poseStack.last(), consumer, blockSize, blockSize, blockSize,
                LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(color, alpha));
            poseStack.popPose();
        }
    }

    @Override
    protected int getBlockLightLevel(SilentAggregateEntity entity, BlockPos pos) {
        // 自带微光
        return 12;
    }
}
