package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.ICogwheelNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.physics.BntPhysicsTuning;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

public final class BntChainGeometry {
    private static final double ORIENTATION_TOLERANCE = 1.0E-6;
    /** Below this a lined-up wheel's arc can round to a full turn. */
    private static final double GRAZE_TOLERANCE = 1.0E-4;
    private static final double LEAVE_SLACK = 0.25;
    private static final double LOOPED_WRAP = Math.PI * 1.5;
    private static final double NATURAL_FACE = -0.5;
    private static final double REVERSE_WRAP = Math.PI;
    private static final int REWRAP_PASSES = 3;

    private BntChainGeometry() {
    }

    public static Axis sharedAxis(List<? extends ICogwheelNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        Axis axis = nodes.get(0).rotationAxis();
        for (ICogwheelNode node : nodes) {
            if (node.rotationAxis() != axis) {
                return null;
            }
        }
        return axis;
    }

    public static double planarX(Vec3 center, Axis axis) {
        return switch (axis) {
            case X -> center.y;
            case Y -> center.z;
            case Z -> center.x;
        };
    }

    public static double planarY(Vec3 center, Axis axis) {
        return switch (axis) {
            case X -> center.z;
            case Y -> center.x;
            case Z -> center.y;
        };
    }

    public static double trackRadius(ICogwheelNode node) {
        double fallback = node.isLarge()
            ? (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getMediumTrackRadius() : BntPhysicsTuning.getLargeTrackRadius())
            : (node.hasSmallCogwheelOffset() ? BntPhysicsTuning.getSmallTrackRadius() : BntPhysicsTuning.getTinyTrackRadius());
        double radius = BntRadiusProvider.getTrackRadius(node.pos(), node.isLarge(), fallback);
        return radius > 0.0 ? radius : fallback;
    }

    public static boolean[] contacts(List<? extends ICogwheelNode> nodes) {
        int count = nodes.size();
        boolean[] touched = new boolean[count];
        Axis axis = sharedAxis(nodes);
        if (axis == null || count <= 2) {
            Arrays.fill(touched, true);
            return touched;
        }
        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        fill(nodes, axis, xs, ys, radii);
        return BntBeltSolver.contacts(xs, ys, radii);
    }

    public static void fill(List<? extends ICogwheelNode> nodes, Axis axis, double[] xs, double[] ys, double[] radii) {
        for (int i = 0; i < nodes.size(); i++) {
            ICogwheelNode node = nodes.get(i);
            Vec3 center = node.center();
            xs[i] = planarX(center, axis);
            ys[i] = planarY(center, axis);
            radii[i] = trackRadius(node);
        }
    }

    public static void fillLive(List<? extends ICogwheelNode> nodes, Axis axis, double[] xs, double[] ys, double[] radii) {
        for (int i = 0; i < nodes.size(); i++) {
            ICogwheelNode node = nodes.get(i);
            Vec3 center = BntChainMotion.liveCenter(node);
            xs[i] = planarX(center, axis);
            ys[i] = planarY(center, axis);
            radii[i] = trackRadius(node);
        }
    }

    public static List<PathedCogwheelNode> effectiveSequence(List<PathedCogwheelNode> pathNodes) {
        Layout layout = BntChainMotion.layout();
        if (layout == null || layout.sides().length != pathNodes.size()) {
            layout = resolve(pathNodes);
        }
        if (layout == null) {
            return pathNodes;
        }
        return applyLayout(pathNodes, layout);
    }

    public static List<PathedCogwheelNode> applyLayout(List<PathedCogwheelNode> pathNodes, Layout layout) {
        List<PathedCogwheelNode> result = new ArrayList<>(layout.sequence().length);
        for (int index : layout.sequence()) {
            PathedCogwheelNode node = pathNodes.get(index);
            result.add(new PathedCogwheelNode(
                layout.sides()[index], node.isLarge(), node.rotationAxis(), node.localPos(), node.hasSmallCogwheelOffset()));
        }
        return result;
    }

    public static boolean[] grazing(List<PathedCogwheelNode> nodes) {
        int count = nodes.size();
        boolean[] grazed = new boolean[count];
        Axis axis = sharedAxis(nodes);
        if (axis == null || count < 3) {
            return grazed;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        fillLive(nodes, axis, xs, ys, radii);

        int[] sides = new int[count];
        int chirality = 0;
        for (int i = 0; i < count; i++) {
            sides[i] = nodes.get(i).side();
            chirality += sides[i];
        }
        chirality = chirality >= 0 ? 1 : -1;

        for (int i = 0; i < count; i++) {
            int previous = (i - 1 + count) % count;
            int next = (i + 1) % count;
            if (sides[i] != chirality || previous == next) {
                continue;
            }
            grazed[i] = clearance(xs, ys, radii, sides, previous, next, i) < GRAZE_TOLERANCE;
        }
        return grazed;
    }

    public static Layout resolve(List<PathedCogwheelNode> pathNodes) {
        return resolve(pathNodes, null);
    }

    /** Wheels terrain has pushed inside the loop stay on the belt while it still passes cleanly through them. */
    public static Layout resolve(List<PathedCogwheelNode> pathNodes, Layout previous) {
        int count = pathNodes.size();
        Axis axis = sharedAxis(pathNodes);
        if (axis == null || count < 2) {
            return null;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        fillLive(pathNodes, axis, xs, ys, radii);

        boolean[] held = previous == null || previous.sides().length != count ? null : new boolean[count];
        if (held != null) {
            for (int node : previous.sequence()) {
                if (node >= 0 && node < count) {
                    held[node] = true;
                }
            }
        }

        Layout layout = null;
        for (int pass = 0; pass <= count; pass++) {
            layout = solve(xs, ys, radii, axis, pathNodes, held, previous);
            if (held == null || layout == null || !shed(xs, ys, radii, layout, held)) {
                break;
            }
        }
        return layout;
    }

    private static boolean shed(double[] xs, double[] ys, double[] radii, Layout layout, boolean[] held) {
        int[] sequence = layout.sequence();
        int[] sides = layout.sides();
        int length = sequence.length;
        boolean shed = false;
        for (int position = 0; position < length; position++) {
            int node = sequence[position];
            if (!held[node]) {
                continue;
            }

            int previous = sequence[(position - 1 + length) % length];
            int next = sequence[(position + 1) % length];
            if (previous == node || next == node || previous == next) {
                continue;
            }

            double[] incoming = BntBeltSolver.tangent(xs[previous], ys[previous], sides[previous] * radii[previous],
                xs[node], ys[node], sides[node] * radii[node]);
            double[] outgoing = BntBeltSolver.tangent(xs[node], ys[node], sides[node] * radii[node],
                xs[next], ys[next], sides[next] * radii[next]);
            if (incoming == null || outgoing == null) {
                continue;
            }
            if (BntBeltSolver.wrap(sides[node], incoming[3], incoming[4], outgoing[1], outgoing[2]) > REVERSE_WRAP) {
                held[node] = false;
                shed = true;
            }
        }
        return shed;
    }

    private static Layout solve(double[] xs, double[] ys, double[] radii, Axis axis,
                                List<PathedCogwheelNode> pathNodes, boolean[] held, Layout previous) {
        int count = pathNodes.size();
        Direction[] routes = routes(pathNodes);
        boolean[] requested = null;
        if (routes != null) {
            requested = new boolean[count];
            for (int i = 0; i < count; i++) {
                requested[i] = routes[i] != null;
            }
        }
        if (held != null) {
            if (requested == null) {
                requested = new boolean[count];
            }
            for (int i = 0; i < count; i++) {
                if (held[i]) {
                    requested[i] = true;
                }
            }
        }

        boolean[] free = BntBeltSolver.contacts(xs, ys, radii, requested);
        int[] pinned = routes == null ? null : pins(xs, ys, radii, axis, routes, free, requested);
        int[] sides = BntBeltSolver.sides(xs, ys, radii, free, pinned);
        if (sides == null) {
            // Keep which way the chain turns, but drop any single wheel the belt cannot reach round.
            int chirality = 0;
            for (PathedCogwheelNode node : pathNodes) {
                chirality += node.side();
            }
            sides = new int[count];
            Arrays.fill(sides, chirality >= 0 ? 1 : -1);
        } else if (pinned == null) {
            sides = orient(xs, ys, radii, sides, pathNodes, free);
        }

        int[] order = previous == null ? null : previous.sequence();
        int[] sequence = BntBeltSolver.contactSequence(xs, ys, radii, sides, requested, order);
        for (int pass = 0; pass < REWRAP_PASSES; pass++) {
            int[] rewrapped = rewrap(xs, ys, radii, sides, sequence, pinned, requested, order);
            if (rewrapped == null) {
                break;
            }
            sequence = rewrapped;
        }
        return new Layout(sequence, sides);
    }

    private static int[] rewrap(double[] xs, double[] ys, double[] radii, int[] sides, int[] sequence,
                                int[] pinned, boolean[] requested, int[] order) {
        int length = sequence.length;
        if (length < 3) {
            return null;
        }

        double[] px = new double[length];
        double[] py = new double[length];
        double[] pr = new double[length];
        int[] pp = new int[length];
        boolean[] seen = new boolean[sides.length];
        for (int i = 0; i < length; i++) {
            int node = sequence[i];
            if (seen[node]) {
                return null;
            }
            seen[node] = true;
            px[i] = xs[node];
            py[i] = ys[node];
            pr[i] = radii[node];
            pp[i] = pinned == null ? 0 : pinned[node];
        }

        int[] solved = BntBeltSolver.sidesInOrder(px, py, pr, pp);
        if (solved == null) {
            return null;
        }

        boolean changed = false;
        for (int i = 0; i < length; i++) {
            if (sides[sequence[i]] != solved[i]) {
                sides[sequence[i]] = solved[i];
                changed = true;
            }
        }
        return changed ? BntBeltSolver.contactSequence(xs, ys, radii, sides, requested, order) : null;
    }

    private static int[] pins(double[] xs, double[] ys, double[] radii, Axis axis, Direction[] routes,
                              boolean[] free, boolean[] requested) {
        int[] pinned = new int[routes.length];
        boolean any = false;

        for (int node = 0; node < routes.length; node++) {
            if (routes[node] == null) {
                continue;
            }

            double[] target = planarDirection(routes[node], axis);
            if (agreement(xs, ys, radii, free, requested, pinned, node, target) > NATURAL_FACE) {
                continue;
            }
            int held = 0;
            double best = 0.0;
            for (int side = 1; side >= -1; side -= 2) {
                pinned[node] = side;
                double agreement = agreement(xs, ys, radii, free, requested, pinned, node, target);
                if (agreement > best) {
                    best = agreement;
                    held = side;
                }
            }
            pinned[node] = held;
            any |= held != 0;
        }
        return any ? pinned : null;
    }

    private static double agreement(double[] xs, double[] ys, double[] radii, boolean[] free, boolean[] requested,
                                    int[] pinned, int node, double[] target) {
        int[] sides = BntBeltSolver.sides(xs, ys, radii, free, pinned);
        if (sides == null) {
            return 0.0;
        }

        int[] sequence = BntBeltSolver.contactSequence(xs, ys, radii, sides, requested);
        double[] face = contactFace(xs, ys, radii, sides, sequence, node);
        return face == null ? 0.0 : face[0] * target[0] + face[1] * target[1];
    }

    private static double[] contactFace(double[] xs, double[] ys, double[] radii, int[] sides, int[] sequence, int node) {
        int length = sequence.length;
        if (length < 2) {
            return null;
        }

        int position = -1;
        for (int i = 0; i < length; i++) {
            if (sequence[i] == node) {
                position = i;
                break;
            }
        }
        if (position < 0) {
            return null;
        }

        int previous = sequence[(position - 1 + length) % length];
        int next = sequence[(position + 1) % length];
        double[] incoming = BntBeltSolver.tangent(xs[previous], ys[previous], sides[previous] * radii[previous],
            xs[node], ys[node], sides[node] * radii[node]);
        double[] outgoing = BntBeltSolver.tangent(xs[node], ys[node], sides[node] * radii[node],
            xs[next], ys[next], sides[next] * radii[next]);
        if (incoming == null || outgoing == null) {
            return null;
        }

        double arc = BntBeltSolver.wrap(sides[node], incoming[3], incoming[4], outgoing[1], outgoing[2]);
        if (arc > LOOPED_WRAP) {
            return null;
        }

        double middle = Math.atan2(incoming[4], incoming[3]) - sides[node] * arc * 0.5;
        return new double[]{Math.cos(middle), Math.sin(middle)};
    }

    public static boolean stillHolds(List<PathedCogwheelNode> pathNodes, Layout layout) {
        int count = pathNodes.size();
        if (layout == null || layout.sides().length != count || layout.sequence().length < 2) {
            return false;
        }
        Axis axis = sharedAxis(pathNodes);
        if (axis == null || count < 2) {
            return false;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        fillLive(pathNodes, axis, xs, ys, radii);

        int[] sequence = layout.sequence();
        int[] sides = layout.sides();
        boolean[] onPath = new boolean[count];
        for (int node : sequence) {
            onPath[node] = true;
        }

        int chirality = 0;
        for (int node : sequence) {
            chirality += sides[node];
        }
        chirality = chirality >= 0 ? 1 : -1;

        double averageY = 0.0;
        for (int node : sequence) {
            averageY += BntChainMotion.liveCenter(pathNodes.get(node)).y;
        }
        averageY /= sequence.length;

        for (int position = 0; position < sequence.length; position++) {
            int node = sequence[position];
            int previous = sequence[(position - 1 + sequence.length) % sequence.length];
            int next = sequence[(position + 1) % sequence.length];
            if (previous == node || next == node || previous == next || sides[node] != chirality
                || axis != Axis.Y && BntChainMotion.liveCenter(pathNodes.get(node)).y <= averageY) {
                continue;
            }
            if (clearance(xs, ys, radii, sides, previous, next, node) < -LEAVE_SLACK) {
                return false;
            }
        }

        for (int position = 0; position < sequence.length; position++) {
            int from = sequence[position];
            int to = sequence[(position + 1) % sequence.length];
            if (from == to) {
                continue;
            }
            double[] run = BntBeltSolver.tangent(xs[from], ys[from], sides[from] * radii[from],
                xs[to], ys[to], sides[to] * radii[to]);
            if (run == null) {
                return false;
            }
            double startX = xs[from] + run[1];
            double startY = ys[from] + run[2];
            double dx = xs[to] + run[3] - startX;
            double dy = ys[to] + run[4] - startY;
            double lengthSquared = dx * dx + dy * dy;
            if (lengthSquared < 1.0E-12) {
                continue;
            }
            for (int candidate = 0; candidate < count; candidate++) {
                if (onPath[candidate]) {
                    continue;
                }
                double along = ((xs[candidate] - startX) * dx + (ys[candidate] - startY) * dy) / lengthSquared;
                if (along <= 0.0 || along >= 1.0) {
                    continue;
                }
                double offsetX = xs[candidate] - (startX + dx * along);
                double offsetY = ys[candidate] - (startY + dy * along);
                if (radii[candidate] - Math.sqrt(offsetX * offsetX + offsetY * offsetY) > -BntBeltSolver.CONTACT_REACH) {
                    return false;
                }
            }
        }
        return true;
    }

    /** How far a node reaches past its neighbours' run, or NaN when it does not sit along that run. */
    private static double clearance(double[] xs, double[] ys, double[] radii, int[] sides,
                                    int from, int to, int node) {
        double[] run = BntBeltSolver.tangent(xs[from], ys[from], sides[from] * radii[from],
            xs[to], ys[to], sides[to] * radii[to]);
        if (run == null) {
            return Double.NaN;
        }

        double startX = xs[from] + run[1];
        double startY = ys[from] + run[2];
        double dx = xs[to] + run[3] - startX;
        double dy = ys[to] + run[4] - startY;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1.0E-12) {
            return Double.NaN;
        }

        double along = ((xs[node] - startX) * dx + (ys[node] - startY) * dy) / lengthSquared;
        if (along <= 0.0 || along >= 1.0) {
            return Double.NaN;
        }

        double offsetX = xs[node] - (startX + dx * along);
        double offsetY = ys[node] - (startY + dy * along);
        return radii[node] + sides[node] * sides[from] * (offsetX * run[1] + offsetY * run[2]) / radii[from];
    }

    private static double[] planarDirection(Direction route, Axis axis) {
        Vec3 normal = Vec3.atLowerCornerOf(route.getNormal());
        return new double[]{planarX(normal, axis), planarY(normal, axis)};
    }

    private static Direction[] routes(List<PathedCogwheelNode> pathNodes) {
        Direction[] routes = null;
        for (int i = 0; i < pathNodes.size(); i++) {
            Direction route = BntChainMotion.routeSide(pathNodes.get(i).pos());
            if (route == null) {
                continue;
            }
            if (routes == null) {
                routes = new Direction[pathNodes.size()];
            }
            routes[i] = route;
        }
        return routes;
    }

    private static int[] orient(double[] xs, double[] ys, double[] radii, int[] sides,
                                List<PathedCogwheelNode> pathNodes, boolean[] touched) {
        int agreements = 0;
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] == pathNodes.get(i).side()) {
                agreements++;
            }
        }
        if (agreements * 2 >= sides.length) {
            return sides;
        }

        int[] mirrored = new int[sides.length];
        for (int i = 0; i < sides.length; i++) {
            mirrored[i] = -sides[i];
        }

        int[] order = ordering(xs, ys, radii, touched);
        if (order.length < 2) {
            return sides;
        }

        double[] cx = new double[order.length];
        double[] cy = new double[order.length];
        double[] cr = new double[order.length];
        int[] held = new int[order.length];
        int[] flipped = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            cx[i] = xs[order[i]];
            cy[i] = ys[order[i]];
            cr[i] = radii[order[i]];
            held[i] = sides[order[i]];
            flipped[i] = mirrored[order[i]];
        }

        int heldClips = BntBeltSolver.clipping(cx, cy, cr, held);
        int flippedClips = BntBeltSolver.clipping(cx, cy, cr, flipped);
        if (flippedClips != heldClips) {
            return flippedClips < heldClips ? mirrored : sides;
        }

        double[] solved = BntBeltSolver.evaluate(cx, cy, cr, held);
        double[] candidate = BntBeltSolver.evaluate(cx, cy, cr, flipped);
        if (solved == null || candidate == null || candidate[0] > solved[0]) {
            return sides;
        }
        return candidate[0] == solved[0] && candidate[2] > solved[2] + ORIENTATION_TOLERANCE ? sides : mirrored;
    }

    private static int[] ordering(double[] xs, double[] ys, double[] radii, boolean[] touched) {
        int[] order = BntBeltSolver.hullOrder(xs, ys, radii, touched);
        if (order != null) {
            return order;
        }

        int size = 0;
        for (boolean contact : touched) {
            if (contact) {
                size++;
            }
        }
        order = new int[size];
        int found = 0;
        for (int i = 0; i < touched.length; i++) {
            if (touched[i]) {
                order[found++] = i;
            }
        }
        return order;
    }

    public record Layout(int[] sequence, int[] sides) {
    }
}
