package dev.qwxon.bitsntracks.physics;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BntArticulation {
    private static final double SINGULAR = 1.0E-12;
    private static final double RIDGE = 1.0E-8;
    private static final double SLACK = 1.0E-10;

    private final Map<ServerSubLevel, Integer> index;
    private final Vector3d[] centres;
    private final Vector3d[][] spins;
    private final Vector3d[][] drifts;
    private final double[][] inverse;
    private final int freeAxes;

    private BntArticulation(
        Map<ServerSubLevel, Integer> index, Vector3d[] centres, Vector3d[][] spins, Vector3d[][] drifts, double[][] inverse, int freeAxes
    ) {
        this.index = index;
        this.centres = centres;
        this.spins = spins;
        this.drifts = drifts;
        this.inverse = inverse;
        this.freeAxes = freeAxes;
    }

    private record Axis(boolean angular, Vector3d direction, Vector3d anchor, List<Integer> moved) {
    }

    @Nullable
    public static BntArticulation of(
        ServerLevel level, PhysicsPipeline pipeline, ServerSubLevel frame, List<ServerSubLevel> assembly, @Nullable MassData mass
    ) {
        if (assembly.size() < 2 || mass == null || mass.getCenterOfMass() == null) {
            return null;
        }

        Pose3dc framePose = frame.logicalPose();
        Quaterniond toFrame = framePose.orientation().conjugate(new Quaterniond());
        int count = assembly.size();
        Map<ServerSubLevel, Integer> index = new IdentityHashMap<>();
        double[] masses = new double[count];
        Vector3d[] centres = new Vector3d[count];
        Matrix3d[] inertias = new Matrix3d[count];
        for (int i = 0; i < count; i++) {
            ServerSubLevel body = assembly.get(i);
            index.put(body, i);
            Pose3dc pose = body.logicalPose();
            MassData data = body.getMassTracker();
            if (data == null || data.isInvalid() || data.getCenterOfMass() == null) {
                centres[i] = framePose.transformPositionInverse(new Vector3d(pose.position()));
                inertias[i] = new Matrix3d().zero();
                continue;
            }
            masses[i] = data.getMass();
            centres[i] = framePose.transformPositionInverse(pose.transformPosition(data.getCenterOfMass(), new Vector3d()));
            Matrix3d rotation = new Matrix3d().rotation(new Quaterniond(toFrame).mul(pose.orientation()));
            inertias[i] = new Matrix3d(rotation).mul(data.getInertiaTensor()).mul(rotation.transpose(new Matrix3d()));
        }

        Integer root = index.get(frame);
        if (root == null) {
            return null;
        }
        List<List<Integer>> children = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            children.add(new ArrayList<>());
        }
        BntSubLevelLinks.Link[] via = new BntSubLevelLinks.Link[count];
        boolean[] reached = new boolean[count];
        reached[root] = true;
        List<BntSubLevelLinks.Link> links = BntSubLevelLinks.within(level, pipeline, assembly);
        ArrayDeque<Integer> open = new ArrayDeque<>();
        open.add(root);
        while (!open.isEmpty()) {
            int next = open.poll();
            ServerSubLevel body = assembly.get(next);
            for (BntSubLevelLinks.Link link : links) {
                ServerSubLevel other = link.a() == body ? link.b() : link.b() == body ? link.a() : null;
                Integer at = other == null ? null : index.get(other);
                if (at != null && !reached[at]) {
                    reached[at] = true;
                    via[at] = link;
                    children.get(next).add(at);
                    open.add(at);
                }
            }
        }

        List<Axis> axes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (via[i] != null) {
                freeAxes(via[i], framePose, subtree(i, children), axes);
            }
        }
        if (axes.isEmpty()) {
            return null;
        }

        int dofs = 6 + axes.size();
        Vector3dc centre = mass.getCenterOfMass();
        Vector3d[][] spins = new Vector3d[dofs][count];
        Vector3d[][] drifts = new Vector3d[dofs][count];
        for (int d = 0; d < dofs; d++) {
            for (int i = 0; i < count; i++) {
                spins[d][i] = new Vector3d();
                drifts[d][i] = new Vector3d();
            }
        }
        for (int i = 0; i < count; i++) {
            Vector3d offset = new Vector3d(centres[i]).sub(centre);
            for (int k = 0; k < 3; k++) {
                drifts[k][i].setComponent(k, 1.0);
                spins[3 + k][i].setComponent(k, 1.0);
                spins[3 + k][i].cross(offset, drifts[3 + k][i]);
            }
        }
        for (int a = 0; a < axes.size(); a++) {
            Axis axis = axes.get(a);
            for (int i : axis.moved()) {
                if (axis.angular()) {
                    spins[6 + a][i].set(axis.direction());
                    axis.direction().cross(new Vector3d(centres[i]).sub(axis.anchor()), drifts[6 + a][i]);
                } else {
                    drifts[6 + a][i].set(axis.direction());
                }
            }
        }

        double[][] matrix = new double[dofs][dofs];
        Vector3d turned = new Vector3d();
        for (int d = 0; d < dofs; d++) {
            for (int e = d; e < dofs; e++) {
                double sum = 0.0;
                for (int i = 0; i < count; i++) {
                    sum += masses[i] * drifts[d][i].dot(drifts[e][i]) + spins[d][i].dot(inertias[i].transform(spins[e][i], turned));
                }
                matrix[d][e] = sum;
                matrix[e][d] = sum;
            }
        }
        double[][] inverse = invert(matrix);
        return inverse == null ? null : new BntArticulation(index, centres, spins, drifts, inverse, axes.size());
    }

    private static List<Integer> subtree(int start, List<List<Integer>> children) {
        List<Integer> moved = new ArrayList<>();
        ArrayDeque<Integer> open = new ArrayDeque<>();
        open.add(start);
        while (!open.isEmpty()) {
            int next = open.poll();
            moved.add(next);
            open.addAll(children.get(next));
        }
        return moved;
    }

    private static void freeAxes(BntSubLevelLinks.Link link, Pose3dc framePose, List<Integer> moved, List<Axis> axes) {
        Pose3dc pose = link.a().logicalPose();
        if (link.configuration() instanceof RotaryConstraintConfiguration rotary) {
            axes.add(new Axis(true, inFrame(pose, framePose, new Vector3d(rotary.normal1())), at(pose, framePose, rotary.pos1()), moved));
        } else if (link.configuration() instanceof GenericConstraintConfiguration generic) {
            for (ConstraintJointAxis axis : ConstraintJointAxis.ALL) {
                if (!generic.lockedAxes().contains(axis)) {
                    Vector3d local = unit(axis).rotate(generic.orientation1());
                    axes.add(new Axis(angular(axis), inFrame(pose, framePose, local), at(pose, framePose, generic.pos1()), moved));
                }
            }
        }
    }

    private static Vector3d inFrame(Pose3dc pose, Pose3dc framePose, Vector3d local) {
        return framePose.transformNormalInverse(pose.transformNormal(local)).normalize();
    }

    private static Vector3d at(Pose3dc pose, Pose3dc framePose, Vector3dc local) {
        return framePose.transformPositionInverse(pose.transformPosition(local, new Vector3d()));
    }

    private static boolean angular(ConstraintJointAxis axis) {
        return axis == ConstraintJointAxis.ANGULAR_X || axis == ConstraintJointAxis.ANGULAR_Y || axis == ConstraintJointAxis.ANGULAR_Z;
    }

    private static Vector3d unit(ConstraintJointAxis axis) {
        return switch (axis) {
            case LINEAR_X, ANGULAR_X -> new Vector3d(1.0, 0.0, 0.0);
            case LINEAR_Y, ANGULAR_Y -> new Vector3d(0.0, 1.0, 0.0);
            case LINEAR_Z, ANGULAR_Z -> new Vector3d(0.0, 0.0, 1.0);
        };
    }

    public int freeAxes() {
        return this.freeAxes;
    }

    public double[] row(ServerSubLevel body, Vector3dc point, Vector3dc direction) {
        int dofs = this.inverse.length;
        double[] row = new double[dofs];
        Integer at = this.index.get(body);
        if (at == null) {
            return row;
        }
        Vector3d lever = new Vector3d(point).sub(this.centres[at]);
        Vector3d motion = new Vector3d();
        for (int d = 0; d < dofs; d++) {
            row[d] = this.spins[d][at].cross(lever, motion).add(this.drifts[d][at]).dot(direction);
        }
        return row;
    }

    public double[] respond(double[] row) {
        int dofs = this.inverse.length;
        double[] response = new double[dofs];
        for (int d = 0; d < dofs; d++) {
            response[d] = dot(this.inverse[d], row);
        }
        return response;
    }

    public double[] fall(Vector3dc velocity) {
        double[] change = new double[this.inverse.length];
        change[0] = velocity.x();
        change[1] = velocity.y();
        change[2] = velocity.z();
        return change;
    }

    public double[] rest() {
        return new double[this.inverse.length];
    }

    public static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static void add(double[] target, double scale, double[] change) {
        for (int i = 0; i < target.length; i++) {
            target[i] += scale * change[i];
        }
    }

    public static double[] solveBox(double[][] matrix, double[] target, double[] ceiling) {
        int n = target.length;
        double[] solution = new double[n];
        if (n == 0) {
            return solution;
        }
        double[] ridge = new double[n];
        for (int i = 0; i < n; i++) {
            ridge[i] = RIDGE * matrix[i][i];
        }
        double slack = 0.0;
        for (int i = 0; i < n; i++) {
            slack = Math.max(slack, Math.abs(target[i]));
        }
        slack *= SLACK;
        boolean[] held = new boolean[n];
        Arrays.fill(held, true);
        int[] open = new int[n];
        for (int pass = 0; pass < 8 * n + 8; pass++) {
            int m = 0;
            for (int i = 0; i < n; i++) {
                if (!held[i]) {
                    open[m++] = i;
                }
            }
            if (m > 0) {
                double[][] system = new double[m][m + 1];
                for (int a = 0; a < m; a++) {
                    int i = open[a];
                    double rhs = target[i];
                    for (int j = 0; j < n; j++) {
                        if (held[j]) {
                            rhs -= matrix[i][j] * solution[j];
                        }
                    }
                    for (int b = 0; b < m; b++) {
                        system[a][b] = matrix[i][open[b]];
                    }
                    system[a][a] += ridge[i];
                    system[a][m] = rhs;
                }
                double[] goal = eliminate(system, m);
                double step = 1.0;
                int blocking = -1;
                double bound = 0.0;
                for (int a = 0; a < m; a++) {
                    int i = open[a];
                    double from = solution[i];
                    if (goal[a] < 0.0 && from - goal[a] > 0.0) {
                        double reach = from / (from - goal[a]);
                        if (reach < step) {
                            step = reach;
                            blocking = i;
                            bound = 0.0;
                        }
                    } else if (goal[a] > ceiling[i] && goal[a] - from > 0.0) {
                        double reach = (ceiling[i] - from) / (goal[a] - from);
                        if (reach < step) {
                            step = reach;
                            blocking = i;
                            bound = ceiling[i];
                        }
                    }
                }
                for (int a = 0; a < m; a++) {
                    int i = open[a];
                    solution[i] += step * (goal[a] - solution[i]);
                }
                if (blocking >= 0) {
                    solution[blocking] = bound;
                    held[blocking] = true;
                    continue;
                }
            }

            int release = -1;
            double pull = slack;
            for (int i = 0; i < n; i++) {
                if (!held[i]) {
                    continue;
                }
                double gradient = ridge[i] * solution[i] - target[i];
                for (int j = 0; j < n; j++) {
                    gradient += matrix[i][j] * solution[j];
                }
                double wrong = solution[i] <= 0.0 ? -gradient : gradient;
                if (wrong > pull) {
                    pull = wrong;
                    release = i;
                }
            }
            if (release < 0) {
                return solution;
            }
            held[release] = false;
        }
        for (int i = 0; i < n; i++) {
            solution[i] = Math.max(0.0, Math.min(solution[i], ceiling[i]));
        }
        return solution;
    }

    private static double[] eliminate(double[][] system, int m) {
        for (int c = 0; c < m; c++) {
            int pivot = c;
            for (int r = c + 1; r < m; r++) {
                if (Math.abs(system[r][c]) > Math.abs(system[pivot][c])) {
                    pivot = r;
                }
            }
            double[] swap = system[c];
            system[c] = system[pivot];
            system[pivot] = swap;
            if (!(Math.abs(system[c][c]) > 0.0)) {
                continue;
            }
            for (int r = 0; r < m; r++) {
                if (r != c) {
                    double factor = system[r][c] / system[c][c];
                    if (factor != 0.0) {
                        for (int k = c; k <= m; k++) {
                            system[r][k] -= factor * system[c][k];
                        }
                    }
                }
            }
        }
        double[] values = new double[m];
        for (int r = 0; r < m; r++) {
            values[r] = Math.abs(system[r][r]) > 0.0 ? system[r][m] / system[r][r] : 0.0;
        }
        return values;
    }

    @Nullable
    private static double[][] invert(double[][] matrix) {
        int n = matrix.length;
        double[][] system = new double[n][2 * n];
        double scale = 0.0;
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, system[i], 0, n);
            system[i][n + i] = 1.0;
            scale = Math.max(scale, Math.abs(matrix[i][i]));
        }
        for (int c = 0; c < n; c++) {
            int pivot = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(system[r][c]) > Math.abs(system[pivot][c])) {
                    pivot = r;
                }
            }
            double[] swap = system[c];
            system[c] = system[pivot];
            system[pivot] = swap;
            if (!(Math.abs(system[c][c]) > SINGULAR * Math.max(scale, 1.0))) {
                return null;
            }
            double lead = system[c][c];
            for (int k = 0; k < 2 * n; k++) {
                system[c][k] /= lead;
            }
            for (int r = 0; r < n; r++) {
                if (r != c && system[r][c] != 0.0) {
                    double factor = system[r][c];
                    for (int k = 0; k < 2 * n; k++) {
                        system[r][k] -= factor * system[c][k];
                    }
                }
            }
        }
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(system[i], n, inverse[i], 0, n);
        }
        return inverse;
    }
}
