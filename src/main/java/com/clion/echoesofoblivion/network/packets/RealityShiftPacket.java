package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C：镜像区块同步（阶段 5）。
 *
 * <p>旧的实现用 {@code BlockPos} 承载区块坐标（语义错误，且每个坐标多传 8 字节），
 * 并且每次全量发送整个集合。这里改为发送 {@code ChunkPos} 的 {@code long} 编码，
 * 且区分「全量」与「增量」（{@link #add} 为 true 表示只追加）。
 */
public record RealityShiftPacket(List<Long> chunks, boolean add, int phase) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RealityShiftPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("reality_shift"));

    public static final StreamCodec<FriendlyByteBuf, RealityShiftPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public RealityShiftPacket decode(FriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                List<Long> chunks = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    chunks.add(buffer.readLong());
                }
                boolean add = buffer.readBoolean();
                int phase = buffer.readVarInt();
                return new RealityShiftPacket(chunks, add, phase);
            }

            @Override
            public void encode(FriendlyByteBuf buffer, RealityShiftPacket packet) {
                List<Long> chunks = packet.chunks();
                buffer.writeVarInt(chunks.size());
                for (long chunk : chunks) {
                    buffer.writeLong(chunk);
                }
                buffer.writeBoolean(packet.add());
                buffer.writeVarInt(packet.phase());
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RealityShiftPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static RealityShiftPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(RealityShiftPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applyRealityShift(packet.chunks(), packet.add(), packet.phase())));
        context.setPacketHandled(true);
    }

    /** 便于服务端构造：区块坐标 → long。 */
    public static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int unpackChunkX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackChunkZ(long packed) {
        return (int) packed;
    }

    public static BlockPos chunkToPos(long packed) {
        return new BlockPos(unpackChunkX(packed) << 4, 0, unpackChunkZ(packed) << 4);
    }
}
