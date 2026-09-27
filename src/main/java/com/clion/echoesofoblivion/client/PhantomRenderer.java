package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.entity.PhantomEntity;
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

/**
 * 亡魂渲染器（阶段 7 修复：实体可见性）。
 *
 * <p>旧实现继承 {@code EntityRenderer} 却只做 push/pop 变换，<b>不绘制任何几何体</b>，
 * 所以亡魂在游戏里完全不可见。这里改为自绘一套半透明的「破碎人形」：
 * 头、躯干、双臂、下摆，配合正弦扰动做出飘忽不定、边缘发虚的感觉。
 *
 * <p>不依赖模型文件与贴图——{@link RenderType#entityTranslucent} 需要绑定贴图，
 * 因此这里统一用 {@link RenderType#lightning()}（加法混合、无贴图），
 * 亮度由顶点颜色控制，视觉上接近「灵体发光」。
 */
public class PhantomRenderer extends EntityRenderer<PhantomEntity> {

    /** 灵体的基色：青灰偏冷。 */
    private static final int BODY_COLOR = 0x7FD8E0;

    /** 下摆的暗色。 */
    private static final int TATTER_COLOR = 0x2E4A52;

    /** 无贴图时返回空路径（{@code RenderType.lightning} 不会采样它）。 */
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public PhantomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomEntity entity) {
        return NO_TEXTURE;
    }

    @Override
    public void render(PhantomEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = entity.tickCount + partialTick;
        // 灵体呼吸般的透明度脉动
        float pulse = 0.55f + 0.25f * Mth.sin(time * 0.12f);

        poseStack.pushPose();
        // 面向观察方向
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        // 1. 头（略微左右摇摆）
        poseStack.pushPose();
        poseStack.translate(Mth.sin(time * 0.05f) * 0.06f, 1.72f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.5f, 0.5f, 0.5f,
            LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(BODY_COLOR, pulse));
        poseStack.popPose();

        // 2. 躯干
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.15f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.62f, 0.78f, 0.34f,
            LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(BODY_COLOR, pulse * 0.85f));
        poseStack.popPose();

        // 3. 双臂：一高一低，制造「拖着走」的失衡感
        float swing = Mth.sin(time * 0.08f) * 0.15f;
        poseStack.pushPose();
        poseStack.translate(-0.42f, 1.2f + swing, 0.0f);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(8.0f));
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.18f, 0.7f, 0.18f,
            LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(BODY_COLOR, pulse * 0.7f));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.42f, 1.2f - swing, 0.0f);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-8.0f));
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.18f, 0.7f, 0.18f,
            LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(BODY_COLOR, pulse * 0.7f));
        poseStack.popPose();

        // 4. 下摆：没有腿，只有逐渐散开的残影
        for (int i = 0; i < 3; i++) {
            float y = 0.75f - i * 0.28f;
            float width = 0.6f + i * 0.12f;
            float alpha = pulse * (0.5f - i * 0.13f);
            if (alpha <= 0.0f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(Mth.sin(time * 0.04f + i) * 0.05f, y, 0.0f);
            ShapeRenderer.drawBox(poseStack.last(), consumer, width, 0.24f, 0.3f,
                LightTexture.FULL_BRIGHT, ShapeRenderer.withAlpha(TATTER_COLOR, alpha));
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        // 抑制未使用变量告警（pose 用于可能的后续扩展）
        assert pose != null;
    }

    @Override
    protected int getBlockLightLevel(PhantomEntity entity, BlockPos pos) {
        // 灵体不反射环境光
        return 0;
    }
}
