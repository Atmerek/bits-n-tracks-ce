package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntLeverPayload(BlockPos pos, int action, int axis, int steps, boolean wholeTrack) implements CustomPacketPayload {
    public static final int OFFSET = 0;
    public static final int VISIBILITY = 1;
    public static final int PHYSICS = 2;
    public static final int RESET = 3;
    public static final int WIDTH = 4;

    public static final CustomPacketPayload.Type<BntLeverPayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("lever"));

    public static final StreamCodec<FriendlyByteBuf, BntLeverPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BntLeverPayload::pos,
        ByteBufCodecs.VAR_INT, BntLeverPayload::action,
        ByteBufCodecs.VAR_INT, BntLeverPayload::axis,
        ByteBufCodecs.VAR_INT, BntLeverPayload::steps,
        ByteBufCodecs.BOOL, BntLeverPayload::wholeTrack,
        BntLeverPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
