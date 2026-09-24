package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntSuspensionPayload(BlockPos pos, int side) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntSuspensionPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("suspension_piece"));

    public static final StreamCodec<FriendlyByteBuf, BntSuspensionPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntSuspensionPayload::pos,
        ByteBufCodecs.VAR_INT, BntSuspensionPayload::side,
        BntSuspensionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
