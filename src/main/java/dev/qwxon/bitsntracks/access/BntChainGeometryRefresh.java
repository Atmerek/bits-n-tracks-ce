package dev.qwxon.bitsntracks.access;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface BntChainGeometryRefresh {
    void bnt$refreshChainGeometry(Level level, BlockPos controllerPos);

    List<PathedCogwheelNode> bnt$latchedBeltOrder();

    boolean bnt$isNodeEngaged(Level level, BlockPos controllerPos, BlockPos nodeLocalPos);

    void bnt$verifyKinetics(Level level, BlockPos controllerPos);

    boolean bnt$isOwn();
}
