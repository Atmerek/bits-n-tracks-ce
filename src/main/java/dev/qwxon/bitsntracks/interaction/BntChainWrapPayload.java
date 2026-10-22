package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntChainWrapPayload(BlockPos controllerPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntChainWrapPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("chain_wrap"));

    public static final StreamCodec<FriendlyByteBuf, BntChainWrapPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntChainWrapPayload::controllerPos,
        BntChainWrapPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
