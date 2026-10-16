package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.Level;

public class TrackModelRenderContext {
    private static final ThreadLocal<Boolean> IS_RENDERING_TRACK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_RENDERING_CUSTOM_CHAIN = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_RENDERING_WIDE_CHAIN = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Level> RENDERING_LEVEL = new ThreadLocal<>();

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

    public static void setRenderingWideChain(boolean isWide) {
        IS_RENDERING_WIDE_CHAIN.set(isWide);
    }

    public static boolean isRenderingWideChain() {
        return IS_RENDERING_WIDE_CHAIN.get();
    }

    public static void setRenderingLevel(Level level) {
        RENDERING_LEVEL.set(level);
    }

    public static Level getRenderingLevel() {
        return RENDERING_LEVEL.get();
    }

    public static boolean isCustomOrIndustrialCogwheel(KineticBlockEntity be) {
        return be != null && be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock;
    }
}
