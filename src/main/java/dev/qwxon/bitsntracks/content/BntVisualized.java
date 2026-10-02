package dev.qwxon.bitsntracks.content;

import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BntVisualized {
    private static final Set<BlockEntity> DRAWN = ConcurrentHashMap.newKeySet();

    private BntVisualized() {
    }

    public static boolean drawn(BlockEntity be) {
        return be != null && DRAWN.contains(be);
    }

    public static void mark(BlockEntity be, boolean drawn) {
        if (drawn) {
            DRAWN.add(be);
        } else {
            DRAWN.remove(be);
        }
    }

    public static boolean ownType(BlockEntity be) {
        return be != null
            && (be.getType() == BitsNTracksBlockEntityTypes.HIDDEN_COGWHEEL.get() || be.getType() == BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get());
    }
}
