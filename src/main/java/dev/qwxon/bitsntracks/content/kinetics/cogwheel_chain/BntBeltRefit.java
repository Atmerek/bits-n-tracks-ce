package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class BntBeltRefit {
    private BntBeltRefit() {
    }

    public static void refit(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || !level.isLoaded(pos)) {
            return;
        }
        BlockPos controllerPos = BntBeltTension.trackController(level, pos);
        if (controllerPos != null && BntChains.chainAt(level, controllerPos) instanceof BntChainGeometryRefresh chain) {
            chain.bnt$relatch(level, controllerPos);
        }
        BntBeltTension.refit(level, pos);
    }
}
