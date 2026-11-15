package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C：静默区块同步（v2.6.0）。
 *
 * <p>形状刻意与 {@link RealityShiftPacket} 一致（{@code long} 编码的区块 + 增量标志），
 * 这样两套「区块集合」的同步走同一套思路，排查时不需要切换心智模型。
 *
 * <p>客户端需要它只有**一个**理由：静默区里不能有回响
 * （见 {@code MisalignedEchoes}）。因此这个包只承载「哪些区块是静的」，
 * 不承载任何强度、剩余时间之类的东西——静默没有强度，也没有时间。
 */
public record SilenceSyncPacket(List<Long> chunks, boolean add) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SilenceSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("silence_sync"));

    public static final StreamCodec<FriendlyByteBuf, SilenceSyncPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public SilenceSyncPacket decode(FriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                List<Long> chunks = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    chunks.add(buffer.readLong());
                }
                return new SilenceSyncPacket(chunks, buffer.readBoolean());
            }

            @Override
            public void encode(FriendlyByteBuf buffer, SilenceSyncPacket packet) {
                List<Long> chunks = packet.chunks();
                buffer.writeVarInt(chunks.size());
                for (long chunk : chunks) {
                    buffer.writeLong(chunk);
                }
                buffer.writeBoolean(packet.add());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SilenceSyncPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static SilenceSyncPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(SilenceSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applySilenceSync(packet.chunks(), packet.add())));
        context.setPacketHandled(true);
    }
}
