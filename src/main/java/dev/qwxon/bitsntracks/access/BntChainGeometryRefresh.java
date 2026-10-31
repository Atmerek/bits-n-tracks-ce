package dev.qwxon.bitsntracks.access;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface BntChainGeometryRefresh {
    void bnt$refreshChainGeometry(Level level, BlockPos controllerPos);

    List<PathedCogwheelNode> bnt$latchedBeltOrder();

    BntChainGeometry.Layout bnt$heldLayout();

    void bnt$relatch(Level level, BlockPos controllerPos);

    boolean bnt$isNodeEngaged(Level level, BlockPos controllerPos, BlockPos nodeLocalPos);

    void bnt$verifyKinetics(Level level, BlockPos controllerPos);

    boolean bnt$isOwn();

    boolean bnt$claim(Level level, BlockPos controllerPos);
}
