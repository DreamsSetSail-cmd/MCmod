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
 * S2C：打开记忆幻境屏幕。
 *
 * <p>标题、正文、颜色、线索全部由客户端从 {@code MemoryRegistry} 解析，
 * 避免把整段文案塞进网络包。这里只传三个小字段。
 *
 * <p>{@code contradictionKey}（v2.2.0）是**唯一被算在服务端**的部分：
 * 「这段记忆是不按顺序看到的吗」取决于该玩家的见证顺序，而顺序只存在服务端。
 * 传的是**语言键**而不是成品句子，因此仍然不违反「文案不下网络」的原则。
 * 没有矛盾碎片时为空串。
 */
public record MemoryVisionPacket(int memoryIndex, boolean firstTime, String contradictionKey)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemoryVisionPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("memory_vision"));

    public static final StreamCodec<FriendlyByteBuf, MemoryVisionPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MemoryVisionPacket::memoryIndex,
            ByteBufCodecs.BOOL, MemoryVisionPacket::firstTime,
            ByteBufCodecs.STRING_UTF8, MemoryVisionPacket::contradictionKey,
            MemoryVisionPacket::new);

    /** 没有矛盾碎片时的便捷构造。 */
    public static MemoryVisionPacket plain(int memoryIndex, boolean firstTime) {
        return new MemoryVisionPacket(memoryIndex, firstTime, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MemoryVisionPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.openVision(
                packet.memoryIndex(), packet.firstTime(), packet.contradictionKey())));
        context.setPacketHandled(true);
    }
}
