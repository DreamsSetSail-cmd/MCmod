package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.server.ServerPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * C2S：玩家触摸记忆水晶（阶段 1）。
 *
 * <p>只传方块坐标——「这枚水晶提供哪条记忆」由服务端按坐标确定性推导。
 * 旧实现由客户端指定 memoryId，客户端可以伪造任意记忆 ID，属于可利用的逻辑漏洞。
 */
public record MemoryCrystalUsePacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemoryCrystalUsePacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("memory_crystal_use"));

    public static final StreamCodec<FriendlyByteBuf, MemoryCrystalUsePacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, MemoryCrystalUsePacket::pos,
            MemoryCrystalUsePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MemoryCrystalUsePacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static MemoryCrystalUsePacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(MemoryCrystalUsePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> ServerPacketHandler.onCrystalUse(packet.pos(), context.getSender()));
        context.setPacketHandled(true);
    }
}
