package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TrackModelRenderContext {
    private static final ThreadLocal<Boolean> IS_RENDERING_TRACK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_RENDERING_CUSTOM_CHAIN = ThreadLocal.withInitial(() -> false);
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

    public static void setRenderingLevel(Level level) {
        RENDERING_LEVEL.set(level);
    }

    public static Level getRenderingLevel() {
        return RENDERING_LEVEL.get();
    }

    public static boolean isCustomOrIndustrialCogwheel(KineticBlockEntity be) {
        if (be == null) {
            return false;
        } else {
            BlockState state = be.getBlockState();
            if (state == null) {
                return false;
            } else {
                Block block = state.getBlock();
                if (block instanceof BntFlangedCogwheelBlock) {
                    return true;
                } else {
                    if (HiddenCogwheelCompat.isHiddenCogwheel(state) && be instanceof KineticBlockEntityPhysicsAccess access) {
                        String originalBlock = access.bnt$getOriginalBlock();
                        if (originalBlock != null && HiddenCogwheelCompat.isBitsNTracksId(originalBlock)) {
                            return true;
                        }
                    }

                    return false;
                }
            }
        }
    }
}
