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
    private static final double LEAVE_SLACK = 0.25;

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
            grazed[i] = clearance(xs, ys, radii, sides, previous, next, i) < 0.0;
        }
        return grazed;
    }

    public static Layout resolve(List<PathedCogwheelNode> pathNodes) {
        int count = pathNodes.size();
        Axis axis = sharedAxis(pathNodes);
        if (axis == null || count < 2) {
            return null;
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] radii = new double[count];
        fillLive(pathNodes, axis, xs, ys, radii);

        boolean[] free = BntBeltSolver.contacts(xs, ys, radii);
        int[] sides = BntBeltSolver.sides(xs, ys, radii, free);
        if (sides == null) {
            sides = new int[count];
            for (int i = 0; i < count; i++) {
                sides[i] = pathNodes.get(i).side();
            }
        } else {
            sides = orient(xs, ys, radii, sides, pathNodes, free);
        }

        Direction[] routes = routes(pathNodes);
        if (routes == null) {
            return new Layout(BntBeltSolver.contactSequence(xs, ys, radii, sides), sides);
        }

        boolean[] requested = new boolean[count];
        for (int i = 0; i < count; i++) {
            requested[i] = routes[i] != null;
        }

        int[] wrapped = wrap(xs, ys, radii, routes, axis, requested, pathNodes);
        int[] chosen = wrapped == null ? sides : wrapped;
        return new Layout(BntBeltSolver.contactSequence(xs, ys, radii, chosen, requested), chosen);
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

        for (int position = 0; position < sequence.length; position++) {
            int node = sequence[position];
            int previous = sequence[(position - 1 + sequence.length) % sequence.length];
            int next = sequence[(position + 1) % sequence.length];
            if (previous == node || next == node || previous == next || sides[node] != chirality) {
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

    private static double clearance(double[] xs, double[] ys, double[] radii, int[] sides,
                                    int from, int to, int node) {
        double[] run = BntBeltSolver.tangent(xs[from], ys[from], sides[from] * radii[from],
            xs[to], ys[to], sides[to] * radii[to]);
        if (run == null) {
            return 0.0;
        }

        double startX = xs[from] + run[1];
        double startY = ys[from] + run[2];
        double dx = xs[to] + run[3] - startX;
        double dy = ys[to] + run[4] - startY;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1.0E-12) {
            return 0.0;
        }

        double along = ((xs[node] - startX) * dx + (ys[node] - startY) * dy) / lengthSquared;
        if (along <= 0.0 || along >= 1.0) {
            return 0.0;
        }

        double offsetX = xs[node] - (startX + dx * along);
        double offsetY = ys[node] - (startY + dy * along);
        return radii[node] + (offsetX * run[1] + offsetY * run[2]) / radii[from];
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

    private static int[] wrap(double[] xs, double[] ys, double[] radii, Direction[] routes, Axis axis,
                              boolean[] requested, List<PathedCogwheelNode> pathNodes) {
        int count = requested.length;
        int[] best = null;
        int bestHonoured = -1;
        int bestAgreements = -1;

        for (int chirality = 1; chirality >= -1; chirality -= 2) {
            int[] candidate = new int[count];
            int agreements = 0;
            for (int i = 0; i < count; i++) {
                candidate[i] = requested[i] ? -chirality : chirality;
                if (candidate[i] == pathNodes.get(i).side()) {
                    agreements++;
                }
            }
            if (BntBeltSolver.evaluate(xs, ys, radii, candidate) == null) {
                continue;
            }

            int honoured = honours(xs, ys, radii, candidate, routes, axis, requested);
            if (honoured > bestHonoured || (honoured == bestHonoured && agreements > bestAgreements)) {
                bestHonoured = honoured;
                bestAgreements = agreements;
                best = candidate;
            }
        }
        return best;
    }

    private static int honours(double[] xs, double[] ys, double[] radii, int[] sides,
                               Direction[] routes, Axis axis, boolean[] requested) {
        int[] sequence = BntBeltSolver.contactSequence(xs, ys, radii, sides, requested);
        int honoured = 0;
        for (int i = 0; i < routes.length; i++) {
            if (routes[i] == null) {
                continue;
            }
            double[] contact = BntBeltSolver.contactDirection(xs, ys, radii, sides, sequence, i);
            if (contact == null) {
                continue;
            }
            double[] target = planarDirection(routes[i], axis);
            if (contact[0] * target[0] + contact[1] * target[1] > 0.0) {
                honoured++;
            }
        }
        return honoured;
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
