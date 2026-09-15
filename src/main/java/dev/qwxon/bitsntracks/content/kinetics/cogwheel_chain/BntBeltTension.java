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
    public static final float DEFAULT = MAX;

    /** Arc surplus of the droop shape, from integrating its slope over the span. */
    private static final double SAG_ARC = 105.0 / 256.0;

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
        float step = (float)BntPhysicsTuning.getBeltTensionStep();
        float target = clamp(at(level, pos) + (tighten ? step : -step));
        apply(level, pos, target);
        return target;
    }

    public static void reset(Level level, BlockPos pos) {
        apply(level, pos, DEFAULT);
    }

    /** Writes a tension across the chain and rebuilds the loop at that many links. */
    public static void apply(Level level, BlockPos pos, float tension) {
        BlockPos controller = controllerPos(level, pos);
        double fit = BntBeltLinks.fitLength(level, controller);
        write(level, pos, tension, BntBeltLinks.linksFor(fit), fit);
    }

    /** Writes a tension and an already solved link count across the chain. */
    public static void write(Level level, BlockPos pos, float tension, int links, double fit) {
        for (BlockPos nodePos : chainPositions(level, pos)) {
            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access) {
                access.bnt$setBeltTension(tension);
                access.bnt$setBeltLinks(links);
                access.bnt$setBeltFit(fit);
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

    /** Hang at mid span that spends exactly the surplus length given to this run. */
    public static double sagFromSurplus(double spanLength, double surplus) {
        if (spanLength <= 1.0E-6 || surplus <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.sqrt(SAG_ARC * spanLength * surplus), BntPhysicsTuning.getBeltMaxSag());
    }

    /** Droop that leaves both wheels along the run and is deepest at mid span. */
    public static double droopAt(double along, double sagDepth) {
        double centred = along * 2.0 - 1.0;
        double arch = 1.0 - centred * centred;
        return sagDepth * arch * arch;
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

    /** Block that owns the chain this position belongs to, or the position itself. */
    public static BlockPos controllerPos(Level level, BlockPos pos) {
        CogwheelChainBehaviour behaviour = behaviour(level.getBlockEntity(pos));
        if (behaviour == null || behaviour.getControlledChain() != null || behaviour.getControllerOffset() == null) {
            return pos;
        }
        return pos.offset(behaviour.getControllerOffset());
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
