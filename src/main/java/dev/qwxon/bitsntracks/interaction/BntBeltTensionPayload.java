package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntBeltTensionPayload(BlockPos controllerPos, boolean tighten) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntBeltTensionPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("belt_tension"));

    public static final StreamCodec<FriendlyByteBuf, BntBeltTensionPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntBeltTensionPayload::controllerPos,
        ByteBufCodecs.BOOL, BntBeltTensionPayload::tighten,
        BntBeltTensionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
