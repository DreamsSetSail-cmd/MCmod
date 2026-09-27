package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * S2C：影子异常状态（阶段 4）。
 *
 * <p>状态由服务端按侵蚀值决定并同步，避免旧实现里「客户端每 tick 自行随机取状态」
 * 造成的状态抖动（客户端随机出的值会覆盖服务端同步值）。
 */
public record ShadowSyncPacket(int state) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShadowSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("shadow_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShadowSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShadowSyncPacket::state,
            ShadowSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ShadowSyncPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static ShadowSyncPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(ShadowSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applyShadow(packet.state())));
        context.setPacketHandled(true);
    }
}
