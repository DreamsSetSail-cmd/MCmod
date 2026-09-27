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
 * S2C：传送反馈信号（阶段 2）。
 *
 * <p>只作为「刚刚发生了什么」的表现信号：坐标由服务端 {@code teleportTo} 权威设定，
 * 不需要再通过网络包回传，避免浮点精度与不同步问题。
 */
public record PortalTravelPacket(boolean entering, boolean stable) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PortalTravelPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("portal_travel"));

    public static final StreamCodec<FriendlyByteBuf, PortalTravelPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, PortalTravelPacket::entering,
            ByteBufCodecs.BOOL, PortalTravelPacket::stable,
            PortalTravelPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(PortalTravelPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static PortalTravelPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(PortalTravelPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.onPortalTravel(packet.entering(), packet.stable())));
        context.setPacketHandled(true);
    }
}
