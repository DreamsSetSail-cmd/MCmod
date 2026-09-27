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
 * S2C：聚合体光环强度（v1.2.0）。
 *
 * <p>光环的表现形式是**声音消失**，而不是受到攻击。客户端用它来抑制环境音——
 * 这正是它「环境级技能」的实现方式：它不伤害你，它让世界静下来。
 *
 * <p>强度 0~1：0 表示不在光环内（恢复正常音量），1 表示完全静默。
 */
public record BossAuraPacket(float intensity) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BossAuraPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("boss_aura"));

    public static final StreamCodec<FriendlyByteBuf, BossAuraPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.FLOAT, BossAuraPacket::intensity,
            BossAuraPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(BossAuraPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static BossAuraPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(BossAuraPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.applyBossAura(packet.intensity())));
        context.setPacketHandled(true);
    }
}
