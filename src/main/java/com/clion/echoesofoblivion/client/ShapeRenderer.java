package com.clion.echoesofoblivion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 无贴图的彩色几何绘制工具。
 *
 * <p>为什么需要它：本模组的亡魂与 Boss 没有 Blockbench 模型，
 * 而 {@code EntityRenderer} 的默认实现不绘制任何几何体（旧代码的症状就是实体完全不可见）。
 * 这里直接用 {@link VertexConsumer} 输出带颜色的立方体，绕开模型层与贴图图集，
 * 让两个实体<b>立即可见</b>，且不引入额外资源依赖。
 *
 * <p>顶点绕序按 Minecraft 的约定（逆时针为正面），法线朝外。
 */
public final class ShapeRenderer {

    private ShapeRenderer() {
    }

    /**
     * 绘制一个以原点为中心的立方体。
     *
     * @param pose      当前变换矩阵
     * @param consumer  目标顶点消费者
     * @param sizeX/sizeY/sizeZ 立方体尺寸
     * @param light     打包光照值（可用 {@code LightTexture.FULL_BRIGHT} 表示自发光）
     * @param argb      颜色（含 alpha）
     */
    public static void drawBox(PoseStack.Pose pose, VertexConsumer consumer,
                               float sizeX, float sizeY, float sizeZ, int light, int argb) {
        float hx = sizeX / 2.0f;
        float hy = sizeY / 2.0f;
        float hz = sizeZ / 2.0f;

        // 下 (-Y)
        quad(pose, consumer, light, argb,
            -hx, -hy, hz, hx, -hy, hz, hx, -hy, -hz, -hx, -hy, -hz, 0, -1, 0);
        // 上 (+Y)
        quad(pose, consumer, light, argb,
            -hx, hy, -hz, hx, hy, -hz, hx, hy, hz, -hx, hy, hz, 0, 1, 0);
        // 北 (-Z)
        quad(pose, consumer, light, argb,
            hx, -hy, -hz, hx, hy, -hz, -hx, hy, -hz, -hx, -hy, -hz, 0, 0, -1);
        // 南 (+Z)
        quad(pose, consumer, light, argb,
            -hx, -hy, hz, -hx, hy, hz, hx, hy, hz, hx, -hy, hz, 0, 0, 1);
        // 西 (-X)
        quad(pose, consumer, light, argb,
            -hx, -hy, -hz, -hx, hy, -hz, -hx, hy, hz, -hx, -hy, hz, -1, 0, 0);
        // 东 (+X)
        quad(pose, consumer, light, argb,
            hx, -hy, hz, hx, hy, hz, hx, hy, -hz, hx, -hy, -hz, 1, 0, 0);
    }

    /** 绘制一个可指定最小/最大角的盒体（便于拼装不规则形状）。 */
    public static void drawBoxFromTo(PoseStack.Pose pose, VertexConsumer consumer,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     int light, int argb) {
        // 下
        quad(pose, consumer, light, argb, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);
        // 上
        quad(pose, consumer, light, argb, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
        // 北
        quad(pose, consumer, light, argb, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, 0, 0, -1);
        // 南
        quad(pose, consumer, light, argb, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0, 0, 1);
        // 西
        quad(pose, consumer, light, argb, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1, 0, 0);
        // 东
        quad(pose, consumer, light, argb, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, 1, 0, 0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light, int argb,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        vertex(pose, consumer, light, argb, ax, ay, az, nx, ny, nz);
        vertex(pose, consumer, light, argb, bx, by, bz, nx, ny, nz);
        vertex(pose, consumer, light, argb, cx, cy, cz, nx, ny, nz);
        vertex(pose, consumer, light, argb, dx, dy, dz, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light, int argb,
                               float x, float y, float z, float nx, float ny, float nz) {
        consumer.vertex(pose.pose(), x, y, z)
            .color(argb)
            .uv(0.0f, 0.0f)
            .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(pose, nx, ny, nz)
            .endVertex();
    }

    /** 按比例调整颜色的 alpha 通道。 */
    public static int withAlpha(int argb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
