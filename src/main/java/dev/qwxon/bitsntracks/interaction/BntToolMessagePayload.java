package dev.qwxon.bitsntracks.interaction;

import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BntToolMessagePayload(Component message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BntToolMessagePayload> TYPE =
        new CustomPacketPayload.Type<>(BitsNTracks.asResource("tool_message"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BntToolMessagePayload> CODEC = StreamCodec.composite(
        ComponentSerialization.TRUSTED_STREAM_CODEC, BntToolMessagePayload::message,
        BntToolMessagePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
