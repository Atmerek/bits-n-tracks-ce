package dev.qwxon.bitsntracks.access;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface BntChainGeometryRefresh {
    void bnt$refreshChainGeometry(Level level, BlockPos controllerPos);

    boolean bnt$isNodeEngaged(Level level, BlockPos controllerPos, BlockPos nodeLocalPos);

    void bnt$verifyKinetics(Level level, BlockPos controllerPos);
}
