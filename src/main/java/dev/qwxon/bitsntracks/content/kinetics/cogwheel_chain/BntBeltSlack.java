package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class BntBeltSlack {
    private static final double TIGHT_FLOOR = 1.0E-3;

    private BntBeltSlack() {
    }

    public static float contextSpeed() {
        Level level = BntRadiusProvider.level();
        BlockPos origin = BntRadiusProvider.origin();
        return level != null && origin != null && level.getBlockEntity(origin) instanceof KineticBlockEntity kinetic
            ? kinetic.getSpeed()
            : 0.0F;
    }

    public static int tightRun(List<PathedCogwheelNode> nodes, float speed) {
        int count = nodes == null ? 0 : nodes.size();
        if (count < 2 || speed == 0.0F) {
            return -1;
        }

        int driver = 0;
        for (int i = 0; i < count; i++) {
            if (nodes.get(i).localPos().equals(BlockPos.ZERO)) {
                driver = i;
                break;
            }
        }
        return speed > 0.0F ? (driver - 1 + count) % count : driver;
    }

    public static double[] distribute(double[] runLengths, double surplus, int tightRun, float speed) {
        int count = runLengths.length;
        double[] share = new double[count];
        if (count == 0 || surplus <= 0.0) {
            return share;
        }

        double pull = tightRun < 0 || tightRun >= count
            ? 0.0
            : BntPhysicsTuning.getBeltTightSideBias()
                * Mth.clamp(Math.abs(speed) / BntPhysicsTuning.getBeltTightSideSpeed(), 0.0, 1.0);

        double total = 0.0;
        for (int i = 0; i < count; i++) {
            double weight = Math.max(runLengths[i], 0.0);
            if (i == tightRun) {
                weight *= 1.0 - pull;
            }
            share[i] = weight;
            total += weight;
        }

        if (total <= TIGHT_FLOOR) {
            Arrays.fill(share, surplus / count);
            return share;
        }
        for (int i = 0; i < count; i++) {
            share[i] = surplus * share[i] / total;
        }
        return share;
    }
}
