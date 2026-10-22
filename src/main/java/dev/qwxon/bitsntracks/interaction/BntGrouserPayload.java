package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntGrouserPayload(BlockPos controllerPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntGrouserPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("grousers"));

    public static final StreamCodec<FriendlyByteBuf, BntGrouserPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntGrouserPayload::controllerPos,
        BntGrouserPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
