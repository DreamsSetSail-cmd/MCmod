package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.server.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * C2S：请求刷新记忆进度（v1.1.0，配合记忆图鉴）。
 *
 * <p>无载荷——它只是一个「请把最新进度发给我」的信号。服务端收到后会回送
 * {@code MemorySyncPacket}，客户端随即打开图鉴。
 *
 * <p>用空载荷而不是复用 {@code MemoryCrystalUsePacket}，是因为这两件事的语义完全不同：
 * 一个是「我触摸了方块」，一个是「我要看我的记录」。分开也让服务端校验逻辑保持独立。
 *
 * <p>推进位掩码只有 32 位，因此并发与乱序都无害——任何一次同步都是完整状态。
 */
public record CodexRequestPacket() implements CustomPacketPayload {

    /** 唯一实例：无字段的包不需要每次新建。 */
    public static final CodexRequestPacket INSTANCE = new CodexRequestPacket();

    public static final CustomPacketPayload.Type<CodexRequestPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("codex_request"));

    /**
     * 空编解码：没有字段可写。
     *
     * <p>必须显式写出 {@code <FriendlyByteBuf, ...>}——{@code messageBuilder} 默认
     * 用 {@code FriendlyByteBuf} 作为缓冲类型，让编译器自行推断会得到 {@code ByteBuf}
     * 而无法赋值。
     */
    public static final StreamCodec<FriendlyByteBuf, CodexRequestPacket> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CodexRequestPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> ServerPacketHandler.onCodexRequest(context.getSender()));
        context.setPacketHandled(true);
    }
}
