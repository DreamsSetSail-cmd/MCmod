package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * S2C：打开记忆幻境屏幕。
 *
 * <p>只传记忆索引与「是否首次目睹」。标题、正文、颜色、线索全部由客户端从
 * {@code MemoryRegistry} 解析，避免把整段文案塞进网络包。
 */
public record MemoryVisionPacket(int memoryIndex, boolean firstTime) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemoryVisionPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("memory_vision"));

    public static final StreamCodec<FriendlyByteBuf, MemoryVisionPacket> STREAM_CODEC =
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, MemoryVisionPacket::memoryIndex,
            net.minecraft.network.codec.ByteBufCodecs.BOOL, MemoryVisionPacket::firstTime,
            MemoryVisionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MemoryVisionPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static MemoryVisionPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(MemoryVisionPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openVision(packet.memoryIndex(), packet.firstTime())));
        context.setPacketHandled(true);
    }
}
