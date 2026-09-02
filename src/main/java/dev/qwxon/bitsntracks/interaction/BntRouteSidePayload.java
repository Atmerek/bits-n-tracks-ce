package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntRouteSidePayload(BlockPos pos, int zone) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntRouteSidePayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("route_side"));

    public static final StreamCodec<FriendlyByteBuf, BntRouteSidePayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntRouteSidePayload::pos,
        ByteBufCodecs.VAR_INT, BntRouteSidePayload::zone,
        BntRouteSidePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
