package dev.qwxon.bitsntracks.content;

public final class TrackModelRenderContext {
    private static final ThreadLocal<Boolean> IS_RENDERING_TRACK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_RENDERING_CUSTOM_CHAIN = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> CHAIN_QUARTERS = ThreadLocal.withInitial(() -> BntCogwheelWidth.SINGLE);
    private static final ThreadLocal<Boolean> WRAPPED = ThreadLocal.withInitial(() -> false);

    private TrackModelRenderContext() {
    }

    public static void setRenderingTrack(boolean isRenderingTrack) {
        IS_RENDERING_TRACK.set(isRenderingTrack);
    }

    public static boolean isRenderingTrack() {
        return IS_RENDERING_TRACK.get();
    }

    public static void setRenderingCustomChain(boolean isCustom) {
        IS_RENDERING_CUSTOM_CHAIN.set(isCustom);
    }

    public static boolean isRenderingCustomChain() {
        return IS_RENDERING_CUSTOM_CHAIN.get();
    }

    public static void setChainQuarters(int quarters) {
        CHAIN_QUARTERS.set(quarters);
    }

    public static int chainQuarters() {
        return CHAIN_QUARTERS.get();
    }

    public static void setWrapped(boolean wrapped) {
        WRAPPED.set(wrapped);
    }

    public static boolean isWrapped() {
        return WRAPPED.get();
    }
}
