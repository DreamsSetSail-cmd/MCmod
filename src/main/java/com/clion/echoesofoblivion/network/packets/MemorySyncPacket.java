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
 * S2C：记忆收集进度全量同步（登录、重生、跨维度时发送）。
 *
 * <p>已收集记忆用位掩码表示（记忆上限 32 条，目前 5 条），线索串用逗号连接。
 * 这样包结构是定长的定长 + 单个字符串，不依赖 List 编解码器，跨版本更稳。
 */
public record MemorySyncPacket(int collectedMask, String clueCsv) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemorySyncPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("memory_sync"));

    public static final StreamCodec<FriendlyByteBuf, MemorySyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MemorySyncPacket::collectedMask,
            ByteBufCodecs.STRING_UTF8, MemorySyncPacket::clueCsv,
            MemorySyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MemorySyncPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static MemorySyncPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(MemorySyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applyMemorySync(packet.collectedMask(), packet.clueCsv())));
        context.setPacketHandled(true);
    }
}
