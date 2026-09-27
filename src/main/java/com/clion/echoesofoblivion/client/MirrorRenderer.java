package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.entity.MirrorEntity;
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
 * 「镜中的你」渲染器（v1.2.0）。
 *
 * <p>与亡魂和聚合体**刻意采用不同的表现手法**：
 *
 * <table>
 *   <tr><th>实体</th><th>渲染</th><th>观感</th></tr>
 *   <tr><td>亡魂</td><td>{@code lightning()} 加法混合</td><td>半透明、发光、明显不属于这个世界</td></tr>
 *   <tr><td>聚合体</td><td>{@code lightning()} 加法混合</td><td>能量体、符文环、非生物</td></tr>
 *   <tr><td>镜中的你</td><td>{@code entityCutoutNoCull}，<b>不透明</b></td><td><b>看起来是真的</b></td></tr>
 * </table>
 *
 * <p>这是这个实体的全部恐怖来源：它不发光、不透明、没有异常效果，
 * 只是一个站在那里的**人形**。玩家会先以为自己看到了别的玩家，
 * 然后才意识到那是一个像素一样、完全没有颜色的人。
 *
 * <p>颜色接近全黑但有细微的色调偏移，因此在暗处几乎与影子无法区分，
 * 在亮处则像一个被抽掉颜色的剪影。
 */
public class MirrorRenderer extends EntityRenderer<MirrorEntity> {

    /** 极暗的冷色。比纯黑多一点信息，让轮廓在暗处可辨。 */
    private static final int BODY = 0x14141A;
    private static final int LIMB = 0x101016;
    private static final int HEAD = 0x18181F;

    private static final ResourceLocation NO_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public MirrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.shadowStrength = 0.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(MirrorEntity entity) {
        return NO_TEXTURE;
    }

    @Override
    public void render(MirrorEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));

        // entityCutoutNoCull 是给实体用的不透明渲染类型，不采样我们的"贴图"，
        // 因为我们只输出顶点颜色。用顶点光照而非全亮——它应当被环境光照影响，
        // 否则会像发光体，破坏「它看起来是真的」这个前提。
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(NO_TEXTURE));
        int light = packedLight;

        // 头（玩家的头大约在 1.6~1.8 格高）
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.62f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.5f, 0.5f, 0.5f, light, HEAD);
        poseStack.popPose();

        // 躯干
        poseStack.pushPose();
        poseStack.translate(0.0f, 1.12f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.6f, 0.72f, 0.32f, light, BODY);
        poseStack.popPose();

        // 双臂：笔直垂下。刻意不做任何摆动——
        // 一个静止的人形比一个在动的人形更不像活人。
        poseStack.pushPose();
        poseStack.translate(-0.4f, 1.15f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.18f, 0.72f, 0.18f, light, LIMB);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.4f, 1.15f, 0.0f);
        ShapeRenderer.drawBox(poseStack.last(), consumer, 0.18f, 0.72f, 0.18f, light, LIMB);
        poseStack.popPose();

        // 双腿
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * 0.14f, 0.38f, 0.0f);
            ShapeRenderer.drawBox(poseStack.last(), consumer, 0.22f, 0.76f, 0.22f, light, LIMB);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected int getBlockLightLevel(MirrorEntity entity, BlockPos pos) {
        // 不发光：它是暗的，和影子一样。这一点与亡魂、聚合体相反。
        return 0;
    }

    /** 保留：将来若要让它随侵蚀程度逐渐「染上」玩家的样子，从这里入手。 */
    @SuppressWarnings("unused")
    private static int tintFor(float corruptionRatio) {
        int dim = Mth.clamp((int) (60 * (1.0f - corruptionRatio)), 0, 60);
        return 0xFF000000 | (dim << 16) | (dim << 8) | (dim + 6);
    }
}
