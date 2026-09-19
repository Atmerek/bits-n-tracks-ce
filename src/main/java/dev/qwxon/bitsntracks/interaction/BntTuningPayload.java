package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntTuningPayload(BlockPos pos, int setting, int step, boolean wholeTrack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntTuningPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("tuning"));

    public static final StreamCodec<FriendlyByteBuf, BntTuningPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntTuningPayload::pos,
        ByteBufCodecs.VAR_INT, BntTuningPayload::setting,
        ByteBufCodecs.VAR_INT, BntTuningPayload::step,
        ByteBufCodecs.BOOL, BntTuningPayload::wholeTrack,
        BntTuningPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
