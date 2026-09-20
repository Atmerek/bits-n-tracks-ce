package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntAlignmentModePayload(boolean wholeTrack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntAlignmentModePayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("alignment_mode"));

    public static final StreamCodec<FriendlyByteBuf, BntAlignmentModePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, BntAlignmentModePayload::wholeTrack,
        BntAlignmentModePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
