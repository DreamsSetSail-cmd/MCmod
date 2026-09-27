package com.clion.echoesofoblivion.network.packets;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import com.clion.echoesofoblivion.server.BossCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * C2S：玩家使用某条记忆攻击 Boss（阶段 6）。
 *
 * <p>只发送记忆索引；是否允许、造成多少概念伤害、Boss 是否进入下一阶段，
 * 全部由服务端 {@link BossCombat} 判定。
 */
public record MemoryAttackPacket(int memoryIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MemoryAttackPacket> TYPE =
        new CustomPacketPayload.Type<>(EchoesOfOblivionMod.id("memory_attack"));

    public static final StreamCodec<FriendlyByteBuf, MemoryAttackPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MemoryAttackPacket::memoryIndex,
            MemoryAttackPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(MemoryAttackPacket packet, FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, packet);
    }

    public static MemoryAttackPacket decode(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }

    public static void handle(MemoryAttackPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            var player = context.getSender();
            // 潜行使用 = 焚烧（v2.0.0）：牺牲这段记忆换取一次即时效果。
            // 与普通攻击共用同一个键与同一个包，是因为两者的输入意图完全相同
            // （「我要用这段记忆」），区别只在代价——而代价由玩家自己选择。
            if (player != null && player.isShiftKeyDown()) {
                com.clion.echoesofoblivion.server.MemoryBurn.burn(player, packet.memoryIndex());
                return;
            }
            BossCombat.onMemoryAttack(player, packet.memoryIndex());
        });
        context.setPacketHandled(true);
    }
}
