package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BntBeltTension {
    public static final float MIN = 0.0F;
    public static final float MAX = 1.0F;
    public static final float DEFAULT = MAX;

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

    public static float step(Level level, BlockPos pos, boolean tighten) {
        float step = (float)BntPhysicsTuning.getBeltTensionStep();
        float target = clamp(at(level, pos) + (tighten ? step : -step));
        apply(level, pos, target);
        return target;
    }

    public static void reset(Level level, BlockPos pos) {
        apply(level, pos, DEFAULT);
    }

    public static void apply(Level level, BlockPos pos, float tension) {
        BlockPos controller = controllerPos(level, pos);
        double fit = BntBeltLinks.fitLength(level, controller);
        int links = BntBeltLinks.linksFor(fit);
        if (links <= BntBeltLinks.UNSET) {
            write(level, pos, tension, BntBeltLinks.at(level, controller), BntBeltLinks.fitAt(level, controller));
            return;
        }
        write(level, pos, tension, links, fit);
    }

    public static void refit(Level level, BlockPos pos) {
        BlockPos controller = trackController(level, pos);
        if (controller == null) {
            return;
        }
        double fit = BntBeltLinks.fitLength(level, controller);
        int links = BntBeltLinks.linksFor(fit);
        if (links > BntBeltLinks.UNSET) {
            write(level, controller, at(level, controller), links, fit);
        }
    }

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

    public static double sagFromSurplus(double spanLength, double surplus) {
        if (spanLength <= 1.0E-6 || surplus <= 0.0) {
            return 0.0;
        }
        return Math.min(Math.sqrt(SAG_ARC * spanLength * surplus), BntPhysicsTuning.getBeltMaxSag());
    }

    public static double droopAt(double along, double sagDepth) {
        double centred = along * 2.0 - 1.0;
        double arch = 1.0 - centred * centred;
        return sagDepth * arch * arch;
    }

    public static double supportScale(float tension) {
        double range = BntPhysicsTuning.getBeltStiffnessRange();
        double normalised = clamp(tension);
        return BntPhysicsTuning.BELT_SUPPORT * Mth.lerp(normalised, 1.0 / range, range);
    }

    public static BlockPos controllerPos(Level level, BlockPos pos) {
        BlockPos controller = BntChains.controllerPos(level, pos);
        return controller == null ? pos : controller;
    }

    @Nullable
    private static BlockPos trackController(Level level, BlockPos pos) {
        BlockPos controller = BntChains.controllerPos(level, pos);
        if (controller != null) {
            return controller;
        }
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        return partnerPos == null ? null : BntChains.controllerPos(level, partnerPos);
    }

    public static Set<BlockPos> chainPositions(Level level, BlockPos pos) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(pos);
        BlockPos controllerPos = trackController(level, pos);
        CogwheelChain chain = BntChains.chainAt(level, controllerPos);
        if (chain != null) {
            for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
                positions.add(controllerPos.offset(node.localPos()));
            }
        }
        return positions;
    }
}
