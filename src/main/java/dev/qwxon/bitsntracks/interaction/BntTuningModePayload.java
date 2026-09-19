package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntTuningModePayload(int setting, boolean wholeTrack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntTuningModePayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("tuning_mode"));

    public static final StreamCodec<FriendlyByteBuf, BntTuningModePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, BntTuningModePayload::setting,
        ByteBufCodecs.BOOL, BntTuningModePayload::wholeTrack,
        BntTuningModePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
