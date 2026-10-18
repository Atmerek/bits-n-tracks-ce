package dev.qwxon.bitsntracks.content;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class BntToolScope {
    private BntToolScope() {
    }

    public static MutableComponent name(boolean wholeTrack) {
        return Component.translatable(wholeTrack ? "bits_n_tracks.scope.track" : "bits_n_tracks.scope.cogwheel");
    }
}
