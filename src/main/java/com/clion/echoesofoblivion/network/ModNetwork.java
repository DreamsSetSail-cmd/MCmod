package com.clion.echoesofoblivion.network;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.network.packets.CodexRequestPacket;
import com.clion.echoesofoblivion.network.packets.CorruptionUpdatePacket;
import com.clion.echoesofoblivion.network.packets.MemoryAttackPacket;
import com.clion.echoesofoblivion.network.packets.MemoryCrystalUsePacket;
import com.clion.echoesofoblivion.network.packets.MemorySyncPacket;
import com.clion.echoesofoblivion.network.packets.MemoryVisionPacket;
import com.clion.echoesofoblivion.network.packets.PortalTravelPacket;
import com.clion.echoesofoblivion.network.packets.RealityShiftPacket;
import com.clion.echoesofoblivion.network.packets.RenderStatePacket;
import com.clion.echoesofoblivion.network.packets.ShadowSyncPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * 网络通道注册。
 *
 * <p>所有包都实现 {@link CustomPacketPayload}，编解码由 {@code StreamCodec} 承担，
 * 因此不可能再出现「编码器与解码器字段顺序不一致」这类静默错误。
 *
 * <p><b>服务端/客户端隔离</b>：各包的 {@code handle} 方法只负责分发，
 * 真正的客户端逻辑全部放在 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}
 * 之内。旧代码在包里直接 {@code import net.minecraft.client.Minecraft}，
 * 导致服务端加载时抛 {@code NoClassDefFoundError} —— 这是必须避免的。
 */
public final class ModNetwork {

    /** 协议版本：客户端与服务端必须完全一致。 */
    private static final int PROTOCOL_VERSION = 2;

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(EchoesOfOblivionMod.id("main"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
        .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
        .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        // 统一用 .codec(...)：编码器与解码器来自同一个 StreamCodec，
        // 从结构上排除「两边字段顺序不一致」这类静默错误。
        CHANNEL.messageBuilder(MemoryCrystalUsePacket.class, 0)
            .codec(MemoryCrystalUsePacket.STREAM_CODEC)
            .consumerMainThread(MemoryCrystalUsePacket::handle)
            .add();

        CHANNEL.messageBuilder(MemoryVisionPacket.class, 1)
            .codec(MemoryVisionPacket.STREAM_CODEC)
            .consumerMainThread(MemoryVisionPacket::handle)
            .add();

        CHANNEL.messageBuilder(MemorySyncPacket.class, 2)
            .codec(MemorySyncPacket.STREAM_CODEC)
            .consumerMainThread(MemorySyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(CorruptionUpdatePacket.class, 3)
            .codec(CorruptionUpdatePacket.STREAM_CODEC)
            .consumerMainThread(CorruptionUpdatePacket::handle)
            .add();

        CHANNEL.messageBuilder(ShadowSyncPacket.class, 4)
            .codec(ShadowSyncPacket.STREAM_CODEC)
            .consumerMainThread(ShadowSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(RealityShiftPacket.class, 5)
            .codec(RealityShiftPacket.STREAM_CODEC)
            .consumerMainThread(RealityShiftPacket::handle)
            .add();

        CHANNEL.messageBuilder(PortalTravelPacket.class, 6)
            .codec(PortalTravelPacket.STREAM_CODEC)
            .consumerMainThread(PortalTravelPacket::handle)
            .add();

        // 阶段 6：Boss 阶段/稳定度与终结演出的表现同步
        CHANNEL.messageBuilder(RenderStatePacket.class, 7)
            .codec(RenderStatePacket.STREAM_CODEC)
            .consumerMainThread(RenderStatePacket::handle)
            .add();

        // 阶段 6：玩家按下「记忆攻击」键
        CHANNEL.messageBuilder(MemoryAttackPacket.class, 8)
            .codec(MemoryAttackPacket.STREAM_CODEC)
            .consumerMainThread(MemoryAttackPacket::handle)
            .add();

        // v1.1.0：记忆图鉴的进度刷新请求（无载荷）
        CHANNEL.messageBuilder(CodexRequestPacket.class, 9)
            .codec(CodexRequestPacket.STREAM_CODEC)
            .consumerMainThread(CodexRequestPacket::handle)
            .add();
    }

    public static void sendToServer(CustomPacketPayload payload) {
        CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }
}
