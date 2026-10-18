package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BntChains {
    private BntChains() {
    }

    @Nullable
    public static CogwheelChainBehaviour behaviour(@Nullable BlockEntity be) {
        return be instanceof SmartBlockEntity smart ? smart.getBehaviour(CogwheelChainBehaviour.TYPE) : null;
    }

    @Nullable
    public static CogwheelChainBehaviour behaviour(Level level, @Nullable BlockPos pos) {
        return pos == null ? null : behaviour(level.getBlockEntity(pos));
    }

    @Nullable
    public static BlockPos controllerPos(@Nullable CogwheelChainBehaviour behaviour) {
        if (behaviour == null || behaviour.getBlockEntity() == null) {
            return null;
        }
        BlockPos pos = behaviour.getBlockEntity().getBlockPos();
        if (behaviour.getControlledChain() != null) {
            return pos;
        }
        return behaviour.getControllerOffset() == null ? null : pos.offset(behaviour.getControllerOffset());
    }

    @Nullable
    public static BlockPos controllerPos(@Nullable BlockEntity be) {
        return controllerPos(behaviour(be));
    }

    @Nullable
    public static BlockPos controllerPos(Level level, BlockPos pos) {
        return controllerPos(behaviour(level, pos));
    }

    @Nullable
    public static CogwheelChain chainAt(Level level, @Nullable BlockPos controllerPos) {
        CogwheelChainBehaviour behaviour = behaviour(level, controllerPos);
        return behaviour == null ? null : behaviour.getControlledChain();
    }

    @Nullable
    public static CogwheelChain chainOf(@Nullable CogwheelChainBehaviour behaviour) {
        if (behaviour == null) {
            return null;
        }
        CogwheelChain direct = behaviour.getControlledChain();
        Level level = behaviour.getLevel();
        return direct != null || level == null ? direct : chainAt(level, controllerPos(behaviour));
    }

    public static List<PathedCogwheelNode> beltOrder(Level level, BlockPos controllerPos) {
        CogwheelChain chain = chainAt(level, controllerPos);
        if (chain == null) {
            return List.of();
        }
        List<PathedCogwheelNode> latched = chain instanceof BntChainGeometryRefresh refreshable ? refreshable.bnt$latchedBeltOrder() : List.of();
        return latched.size() >= 2 ? latched : chain.getChainPathCogwheelNodes();
    }
}
