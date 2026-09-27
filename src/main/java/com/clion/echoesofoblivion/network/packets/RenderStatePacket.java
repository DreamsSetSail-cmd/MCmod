package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * S2C：通用「表现指令」包（阶段 6）。
 *
 * <p>传送渐入、Boss 阶段变化、Boss 终结演出都需要服务端权威下发，但把它们做成
 * 三个各自独立的包并不划算。这里用单一包 + {@code kind} 区分，客户端只负责播放。
 *
 * <p>{@code kind} 取值见下方的 {@code KIND_*} 常量。
 */
public record RenderStatePacket(int kind, int value, float ratio, boolean flag) implements CustomPacketPayload {

    /** 传送表现：{@code value}=1 进入走廊，0 返回主世界。 */
    public static final int KIND_PORTAL = 0;
    /** Boss 阶段：{@code value}=阶段，{@code ratio}=稳定度比例。 */
    public static final int KIND_BOSS_PHASE = 1;
    /** Boss 终结演出。 */
    public static final int KIND_BOSS_DEFEAT = 2;

    public static final CustomPacketPayload.Type<RenderStatePacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("render_state"));

    public static final StreamCodec<FriendlyByteBuf, RenderStatePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RenderStatePacket::kind,
            ByteBufCodecs.VAR_INT, RenderStatePacket::value,
            ByteBufCodecs.FLOAT, RenderStatePacket::ratio,
            ByteBufCodecs.BOOL, RenderStatePacket::flag,
            RenderStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RenderStatePacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static RenderStatePacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(RenderStatePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.clion.echoesofoblivion.client.ClientPacketHandler.applyRenderState(
                packet.kind(), packet.value(), packet.ratio(), packet.flag())));
        context.setPacketHandled(true);
    }

    /** 便捷构造：传送表现。 */
    public static RenderStatePacket portal(boolean entering, boolean stable) {
        return new RenderStatePacket(KIND_PORTAL, entering ? 1 : 0, 0.0f, stable);
    }

    /** 便捷构造：Boss 阶段与稳定度。 */
    public static RenderStatePacket bossPhase(int phase, float stabilityRatio) {
        return new RenderStatePacket(KIND_BOSS_PHASE, phase, stabilityRatio, false);
    }

    /** 便捷构造：Boss 终结演出。 */
    public static RenderStatePacket bossDefeat() {
        return new RenderStatePacket(KIND_BOSS_DEFEAT, 0, 0.0f, true);
    }
}
