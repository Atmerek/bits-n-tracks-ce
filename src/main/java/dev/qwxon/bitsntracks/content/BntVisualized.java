package dev.qwxon.bitsntracks.content;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BntVisualized {
    private static final Set<BlockEntity> DRAWN = ConcurrentHashMap.newKeySet();

    private BntVisualized() {
    }

    public static boolean drawn(BlockEntity be) {
        return be != null && DRAWN.contains(be) && VisualizationManager.supportsVisualization(be.getLevel());
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
