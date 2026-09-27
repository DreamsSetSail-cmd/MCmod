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
 * S2C：意识侵蚀值同步。
 *
 * <p>侵蚀值的<b>唯一权威</b>在服务端 {@code PlayerProgress}。客户端只缓存显示用数值，
 * 不再自行推算（旧实现里客户端与持久化数据各有一套侵蚀值，且从不同步）。
 */
public record CorruptionUpdatePacket(int level, float ratio) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CorruptionUpdatePacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("corruption_update"));

    public static final StreamCodec<FriendlyByteBuf, CorruptionUpdatePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CorruptionUpdatePacket::level,
            ByteBufCodecs.FLOAT, CorruptionUpdatePacket::ratio,
            CorruptionUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(CorruptionUpdatePacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static CorruptionUpdatePacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(CorruptionUpdatePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applyCorruption(packet.level(), packet.ratio())));
        context.setPacketHandled(true);
    }
}
