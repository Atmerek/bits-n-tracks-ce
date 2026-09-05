package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BntBeltTension {
    public static final float MIN = 0.0F;
    public static final float MAX = 1.0F;
    public static final float DEFAULT = 0.5F;

    private BntBeltTension() {
    }

    public static float clamp(float tension) {
        return Float.isFinite(tension) ? Mth.clamp(tension, MIN, MAX) : DEFAULT;
    }

    public static float at(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access
            ? clamp(access.bnt$getBeltTension())
            : DEFAULT;
    }

    /** Applies one lever step across the chain and returns the new value. */
    public static float step(Level level, BlockPos pos, boolean tighten) {
        Set<BlockPos> nodes = chainPositions(level, pos);
        float step = (float)BntPhysicsTuning.getBeltTensionStep();
        float target = clamp(at(level, pos) + (tighten ? step : -step));

        for (BlockPos nodePos : nodes) {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltTension(target);
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
        return target;
    }

    public static void reset(Level level, BlockPos pos) {
        for (BlockPos nodePos : chainPositions(level, pos)) {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltTension(DEFAULT);
                kinetic.setChanged();
                kinetic.sendData();
            }
        }
    }

    /** Tension of the chain at the level and origin set by the caller. */
    public static float contextTension() {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        if (level == null || origin == null) {
            return DEFAULT;
        }
        return level.getBlockEntity(origin) instanceof KineticBlockEntityPhysicsAccess access
            ? clamp(access.bnt$getBeltTension())
            : DEFAULT;
    }

    /** Hang at mid span, from span length and slack, capped by beltMaxSag. */
    public static double sagDepth(double spanLength, float tension) {
        double slack = 1.0 - clamp(tension);
        double sag = spanLength * BntPhysicsTuning.getBeltSagFraction() * slack;
        return Math.min(sag, BntPhysicsTuning.getBeltMaxSag() * slack);
    }

    /** Parabolic droop, zero at both wheels and deepest at mid span. */
    public static double droopAt(double along, double sagDepth) {
        double centred = along * 2.0 - 1.0;
        return sagDepth * (1.0 - centred * centred);
    }

    /** Contact stiffness, scaled between 1 over range and range by tension. */
    public static double supportScale(float tension) {
        double range = BntPhysicsTuning.getBeltStiffnessRange();
        double normalised = clamp(tension);
        return BntPhysicsTuning.getBeltSupportStrength() * Mth.lerp(normalised, 1.0 / range, range);
    }

    public static double gripScale(float tension) {
        return BntPhysicsTuning.getBeltGrip() * Mth.lerp(clamp(tension), 0.5, 1.0);
    }

    public static Set<BlockPos> chainPositions(Level level, BlockPos pos) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(pos);

        CogwheelChainBehaviour behaviour = behaviour(level.getBlockEntity(pos));
        if (behaviour == null) {
            return positions;
        }

        BlockPos controllerPos = pos;
        CogwheelChain chain = behaviour.getControlledChain();
        if (chain == null && behaviour.getControllerOffset() != null) {
            controllerPos = pos.offset(behaviour.getControllerOffset());
            CogwheelChainBehaviour controller = behaviour(level.getBlockEntity(controllerPos));
            if (controller != null) {
                chain = controller.getControlledChain();
            }
        }
        if (chain == null) {
            return positions;
        }

        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            positions.add(controllerPos.offset(node.localPos()));
        }
        return positions;
    }

    private static CogwheelChainBehaviour behaviour(BlockEntity be) {
        return be instanceof SmartBlockEntity smart
            ? (CogwheelChainBehaviour)smart.getBehaviour(CogwheelChainBehaviour.TYPE)
            : null;
    }
}
