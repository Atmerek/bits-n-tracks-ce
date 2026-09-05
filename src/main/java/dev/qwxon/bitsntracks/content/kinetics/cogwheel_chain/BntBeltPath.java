package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

public final class BntBeltPath {
    private BntBeltPath() {
    }

    /** Tangent between two wheels, as offsets from each centre. */
    public static double[] tangent(double xi, double yi, double ai, double xj, double yj, double aj) {
        return BntBeltSolver.tangent(xi, yi, ai, xj, yj, aj);
    }

    /** Inverse of BntChainGeometry.planarX and planarY, w along the axis. */
    public static Vec3 fromPlanar(double u, double v, double w, Axis axis) {
        return switch (axis) {
            case X -> new Vec3(w, u, v);
            case Y -> new Vec3(v, w, u);
            case Z -> new Vec3(u, v, w);
        };
    }

    public static double axisCoord(Vec3 centre, Axis axis) {
        return switch (axis) {
            case X -> centre.x;
            case Y -> centre.y;
            case Z -> centre.z;
        };
    }
}
