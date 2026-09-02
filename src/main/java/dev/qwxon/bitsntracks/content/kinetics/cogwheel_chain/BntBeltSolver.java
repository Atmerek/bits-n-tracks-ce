package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

public final class BntBeltSolver {
    private static final double TAU = Math.PI * 2.0;
    private static final double CONTACT_TOLERANCE = 1.0E-7;
    static final double CONTACT_REACH = 1.0 / 32.0;
    private static final double TANGENT_TOLERANCE = 1.0E-6;
    private static final int MAX_CANDIDATES = 48;
    private static final double CLIP_TOLERANCE = 1.0E-4;

    private BntBeltSolver() {
    }

    public static boolean[] contacts(double[] xs, double[] ys, double[] radii) {
        return contacts(xs, ys, radii, null);
    }

    public static boolean[] contacts(double[] xs, double[] ys, double[] radii, boolean[] force) {
        int count = xs.length;
        boolean[] touched = new boolean[count];
        if (count <= 2) {
            Arrays.fill(touched, true);
            return touched;
        }

        List<double[]> directions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (i == j) {
                    continue;
                }
                for (int side = 1; side >= -1; side -= 2) {
                    double[] run = tangent(xs[i], ys[i], side * radii[i], xs[j], ys[j], side * radii[j]);
                    if (run != null) {
                        directions.add(new double[]{side * run[1] / radii[i], side * run[2] / radii[i]});
                    }
                }
            }
        }

        for (double[] direction : directions) {
            double best = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < count; i++) {
                best = Math.max(best, xs[i] * direction[0] + ys[i] * direction[1] + radii[i]);
            }
            for (int i = 0; i < count; i++) {
                if (xs[i] * direction[0] + ys[i] * direction[1] + radii[i] >= best - CONTACT_REACH) {
                    touched[i] = true;
                }
            }
        }

        if (force != null) {
            for (int i = 0; i < count; i++) {
                if (force[i]) {
                    touched[i] = true;
                }
            }
        }

        int touchedCount = 0;
        for (boolean flag : touched) {
            if (flag) {
                touchedCount++;
            }
        }
        if (touchedCount < 2) {
            Arrays.fill(touched, true);
        }
        return touched;
    }

    public static int[] sides(double[] xs, double[] ys, double[] radii, boolean[] touched) {
        return sides(xs, ys, radii, touched, null);
    }

    public static int[] sides(double[] xs, double[] ys, double[] radii, boolean[] touched, int[] pinned) {
        int count = xs.length;
        int[] result = new int[count];
        Arrays.fill(result, 1);

        int[] index = hullOrder(xs, ys, radii, touched);
        if (index == null) {
            index = new int[count];
            int found = 0;
            for (int i = 0; i < count; i++) {
                if (touched[i]) {
                    index[found++] = i;
                }
            }
            index = Arrays.copyOf(index, found);
        }
        int size = index.length;
        if (size < 2) {
            return null;
        }

        double[] px = new double[size];
        double[] py = new double[size];
        double[] pr = new double[size];
        int[] pp = new int[size];
        for (int i = 0; i < size; i++) {
            px[i] = xs[index[i]];
            py[i] = ys[index[i]];
            pr[i] = radii[index[i]];
            pp[i] = pinned == null ? 0 : pinned[index[i]];
        }

        int[] solved = solveSides(px, py, pr, pp);
        if (solved == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            result[index[i]] = solved[i];
        }

        int orientation = 0;
        for (int side : solved) {
            orientation += side;
        }
        int fill = orientation >= 0 ? 1 : -1;
        for (int i = 0; i < count; i++) {
            if (!touched[i]) {
                result[i] = fill;
            }
        }
        return result;
    }

    static int[] hullOrder(double[] xs, double[] ys, double[] radii, boolean[] touched) {
        int count = xs.length;
        if (count < 3) {
            return null;
        }

        List<Double> boundaries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (i == j) {
                    continue;
                }
                for (int side = 1; side >= -1; side -= 2) {
                    double[] run = tangent(xs[i], ys[i], side * radii[i], xs[j], ys[j], side * radii[j]);
                    if (run != null) {
                        boundaries.add(Math.atan2(run[2] / radii[i], run[1] / radii[i]));
                    }
                }
            }
        }
        if (boundaries.size() < 2) {
            return null;
        }

        double[] angles = new double[boundaries.size()];
        for (int i = 0; i < angles.length; i++) {
            angles[i] = boundaries.get(i);
        }
        Arrays.sort(angles);

        List<Integer> owners = new ArrayList<>();
        for (int i = 0; i < angles.length; i++) {
            double from = angles[i];
            double to = i + 1 < angles.length ? angles[i + 1] : angles[0] + TAU;
            if (to - from < TANGENT_TOLERANCE) {
                continue;
            }
            double middle = (from + to) * 0.5;
            double ux = Math.cos(middle);
            double uy = Math.sin(middle);
            int owner = -1;
            double best = Double.NEGATIVE_INFINITY;
            for (int node = 0; node < count; node++) {
                double support = xs[node] * ux + ys[node] * uy + radii[node];
                if (support > best) {
                    best = support;
                    owner = node;
                }
            }
            if (owners.isEmpty() || owners.get(owners.size() - 1) != owner) {
                owners.add(owner);
            }
        }
        while (owners.size() > 1 && owners.get(0).equals(owners.get(owners.size() - 1))) {
            owners.remove(owners.size() - 1);
        }
        if (owners.size() < 2) {
            return null;
        }

        for (int node = 0; node < count; node++) {
            if (!touched[node] || owners.contains(node)) {
                continue;
            }
            int at = 0;
            double cheapest = Double.MAX_VALUE;
            for (int position = 0; position < owners.size(); position++) {
                int from = owners.get(position);
                int to = owners.get((position + 1) % owners.size());
                double detour = Math.hypot(xs[node] - xs[from], ys[node] - ys[from])
                    + Math.hypot(xs[to] - xs[node], ys[to] - ys[node])
                    - Math.hypot(xs[to] - xs[from], ys[to] - ys[from]);
                if (detour < cheapest) {
                    cheapest = detour;
                    at = position;
                }
            }
            owners.add(at + 1, node);
        }

        int[] order = new int[owners.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = owners.get(i);
        }
        return order;
    }

    public static double[] contactDirection(double[] xs, double[] ys, double[] radii, int[] sides, int[] sequence, int node) {
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
        double[] incoming = tangent(xs[previous], ys[previous], sides[previous] * radii[previous],
            xs[node], ys[node], sides[node] * radii[node]);
        double[] outgoing = tangent(xs[node], ys[node], sides[node] * radii[node],
            xs[next], ys[next], sides[next] * radii[next]);
        if (incoming == null && outgoing == null) {
            return null;
        }
        if (incoming == null) {
            return new double[]{outgoing[1], outgoing[2]};
        }
        if (outgoing == null) {
            return new double[]{incoming[3], incoming[4]};
        }
        double meanX = (incoming[3] + outgoing[1]) * 0.5;
        double meanY = (incoming[4] + outgoing[2]) * 0.5;
        return meanX * meanX + meanY * meanY < CONTACT_TOLERANCE
            ? new double[]{outgoing[1], outgoing[2]}
            : new double[]{meanX, meanY};
    }

    public static double[] evaluate(double[] xs, double[] ys, double[] radii, int[] sides) {
        long crossingCount = crossings(xs, ys, radii, sides);
        if (crossingCount == Long.MAX_VALUE) {
            return null;
        }
        double length = beltLength(xs, ys, radii, sides);
        if (length == Double.MAX_VALUE) {
            return null;
        }
        return new double[]{crossingCount, countFlips(sides), length};
    }

    public static int[] contactSequence(double[] xs, double[] ys, double[] radii, int[] sides) {
        return contactSequence(xs, ys, radii, sides, null);
    }

    public static int[] contactSequence(double[] xs, double[] ys, double[] radii, int[] sides, boolean[] force) {
        int count = xs.length;
        boolean[] touched = contacts(xs, ys, radii, force);
        List<Integer> sequence = new ArrayList<>(count);
        int[] hull = hullOrder(xs, ys, radii, touched);
        if (hull != null) {
            for (int node : hull) {
                sequence.add(node);
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (touched[i]) {
                    sequence.add(i);
                }
            }
        }
        if (sequence.size() < 2) {
            sequence.clear();
            for (int i = 0; i < count; i++) {
                sequence.add(i);
            }
        }

        int budget = count * 2 + 4;
        while (budget-- > 0 && insertClippedCog(xs, ys, radii, sides, sequence)) {
            continue;
        }

        int[] indices = new int[sequence.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = sequence.get(i);
        }
        return indices;
    }

    private static boolean insertClippedCog(double[] xs, double[] ys, double[] radii, int[] sides, List<Integer> sequence) {
        int size = sequence.size();
        for (int position = 0; position < size; position++) {
            int from = sequence.get(position);
            int to = sequence.get((position + 1) % size);
            double[] run = tangent(xs[from], ys[from], sides[from] * radii[from], xs[to], ys[to], sides[to] * radii[to]);
            if (run == null) {
                continue;
            }

            double startX = xs[from] + run[1];
            double startY = ys[from] + run[2];
            double endX = xs[to] + run[3];
            double endY = ys[to] + run[4];
            double dx = endX - startX;
            double dy = endY - startY;
            double lengthSquared = dx * dx + dy * dy;
            if (lengthSquared < 1.0E-12) {
                continue;
            }

            int deepest = -1;
            double deepestPenetration = CLIP_TOLERANCE;
            for (int candidate = 0; candidate < xs.length; candidate++) {
                if (candidate == from || candidate == to) {
                    continue;
                }
                double along = ((xs[candidate] - startX) * dx + (ys[candidate] - startY) * dy) / lengthSquared;
                if (along <= 0.0 || along >= 1.0) {
                    continue;
                }
                double offsetX = xs[candidate] - (startX + dx * along);
                double offsetY = ys[candidate] - (startY + dy * along);
                double penetration = radii[candidate] - Math.sqrt(offsetX * offsetX + offsetY * offsetY);
                if (penetration > deepestPenetration) {
                    deepest = candidate;
                    deepestPenetration = penetration;
                }
            }

            if (deepest >= 0) {
                sequence.add(position + 1, deepest);
                return true;
            }
        }
        return false;
    }

    private static boolean allowed(int[] pinned, int node, int side) {
        return pinned == null || pinned[node] == 0 || pinned[node] == side;
    }

    private static int[] solveSides(double[] xs, double[] ys, double[] radii, int[] pinned) {
        int[] uniform = solveUniformSides(xs, ys, radii, pinned);
        return uniform != null ? uniform : solveMixedSides(xs, ys, radii, pinned);
    }

    private static int[] solveUniformSides(double[] xs, double[] ys, double[] radii, int[] pinned) {
        int count = xs.length;
        int[] best = null;
        double[] bestCost = null;

        for (int chirality = 1; chirality >= -1; chirality -= 2) {
            int[] candidate = new int[count];
            for (int i = 0; i < count; i++) {
                candidate[i] = pinned != null && pinned[i] != 0 ? pinned[i] : chirality;
            }
            double[] cost = evaluate(xs, ys, radii, candidate);
            if (cost == null) {
                continue;
            }
            double[] scored = new double[]{clipping(xs, ys, radii, candidate), cost[0], cost[1], cost[2]};
            if (bestCost == null
                || scored[0] < bestCost[0]
                || (scored[0] == bestCost[0] && scored[1] < bestCost[1])
                || (scored[0] == bestCost[0] && scored[1] == bestCost[1] && scored[2] < bestCost[2])
                || (scored[0] == bestCost[0] && scored[1] == bestCost[1]
                    && scored[2] == bestCost[2] && scored[3] < bestCost[3])) {
                bestCost = scored;
                best = candidate;
            }
        }
        return best;
    }

    static int clipping(double[] xs, double[] ys, double[] radii, int[] sides) {
        int count = xs.length;
        int clipped = 0;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            double[] run = tangent(xs[i], ys[i], sides[i] * radii[i],
                xs[next], ys[next], sides[next] * radii[next]);
            if (run == null) {
                return Integer.MAX_VALUE;
            }

            double startX = xs[i] + run[1];
            double startY = ys[i] + run[2];
            double dx = xs[next] + run[3] - startX;
            double dy = ys[next] + run[4] - startY;
            double lengthSquared = dx * dx + dy * dy;
            if (lengthSquared < 1.0E-12) {
                continue;
            }

            for (int candidate = 0; candidate < count; candidate++) {
                if (candidate == i || candidate == next) {
                    continue;
                }
                double along = ((xs[candidate] - startX) * dx + (ys[candidate] - startY) * dy) / lengthSquared;
                if (along <= 0.0 || along >= 1.0) {
                    continue;
                }
                double offsetX = xs[candidate] - (startX + dx * along);
                double offsetY = ys[candidate] - (startY + dy * along);
                if (radii[candidate] - Math.sqrt(offsetX * offsetX + offsetY * offsetY) > CLIP_TOLERANCE) {
                    clipped++;
                }
            }
        }
        return clipped;
    }

    private static int[] solveMixedSides(double[] xs, double[] ys, double[] radii, int[] pinned) {
        int count = xs.length;
        int[] best = null;
        long bestCrossings = Long.MAX_VALUE;
        int bestFlips = Integer.MAX_VALUE;
        double bestLength = Double.MAX_VALUE;

        for (int firstSide = 1; firstSide >= -1; firstSide -= 2) {
            if (!allowed(pinned, 0, firstSide)) {
                continue;
            }
            for (int secondSide = 1; secondSide >= -1; secondSide -= 2) {
                if (!allowed(pinned, 1 % count, secondSide)) {
                    continue;
                }
                int[][] flipsTo = new int[count + 2][4];
                double[][] lengthTo = new double[count + 2][4];
                for (int step = 0; step <= count + 1; step++) {
                    Arrays.fill(flipsTo[step], Integer.MAX_VALUE);
                    Arrays.fill(lengthTo[step], Double.MAX_VALUE);
                }
                int goal = state(firstSide, secondSide);
                flipsTo[count + 1][goal] = 0;
                lengthTo[count + 1][goal] = 0.0;

                for (int step = count; step >= 1; step--) {
                    for (int from = 0; from < 4; from++) {
                        int previousSide = sideOfStateFirst(from);
                        int currentSide = sideOfStateSecond(from);
                        if (!allowed(pinned, (step - 1 + count) % count, previousSide)
                            || !allowed(pinned, step % count, currentSide)) {
                            continue;
                        }
                        for (int nextSide = 1; nextSide >= -1; nextSide -= 2) {
                            if (!allowed(pinned, (step + 1) % count, nextSide)) {
                                continue;
                            }
                            double[] cost = stepCost(xs, ys, radii, step, previousSide, currentSide, nextSide);
                            if (cost == null) {
                                continue;
                            }
                            int to = state(currentSide, nextSide);
                            if (flipsTo[step + 1][to] == Integer.MAX_VALUE) {
                                continue;
                            }
                            int flips = flipsTo[step + 1][to] + (int) cost[0];
                            double length = lengthTo[step + 1][to] + cost[1];
                            if (flips < flipsTo[step][from]
                                || (flips == flipsTo[step][from] && length < lengthTo[step][from])) {
                                flipsTo[step][from] = flips;
                                lengthTo[step][from] = length;
                            }
                        }
                    }
                }

                if (flipsTo[1][goal] == Integer.MAX_VALUE) {
                    continue;
                }

                PriorityQueue<Partial> queue = new PriorityQueue<>();
                int[] head = new int[]{firstSide, secondSide};
                queue.add(new Partial(flipsTo[1][goal], lengthTo[1][goal], 0, 0.0, 1, goal, head));

                int examined = 0;
                while (!queue.isEmpty() && examined < MAX_CANDIDATES) {
                    Partial partial = queue.poll();
                    if (partial.step > count) {
                        examined++;
                        long crossings = crossings(xs, ys, radii, partial.sides);
                        int flips = countFlips(partial.sides);
                        double length = beltLength(xs, ys, radii, partial.sides);
                        if (length == Double.MAX_VALUE) {
                            continue;
                        }
                        if (crossings < bestCrossings
                            || (crossings == bestCrossings && flips < bestFlips)
                            || (crossings == bestCrossings && flips == bestFlips && length < bestLength)) {
                            bestCrossings = crossings;
                            bestFlips = flips;
                            bestLength = length;
                            best = partial.sides;
                        }
                        if (crossings == 0) {
                            break;
                        }
                        continue;
                    }

                    int currentSide = sideOfStateSecond(partial.state);
                    int previousSide = sideOfStateFirst(partial.state);
                    for (int nextSide = 1; nextSide >= -1; nextSide -= 2) {
                        if (!allowed(pinned, (partial.step + 1) % count, nextSide)) {
                            continue;
                        }
                        double[] cost = stepCost(xs, ys, radii, partial.step, previousSide, currentSide, nextSide);
                        if (cost == null) {
                            continue;
                        }
                        int to = state(currentSide, nextSide);
                        if (flipsTo[partial.step + 1][to] == Integer.MAX_VALUE) {
                            continue;
                        }
                        int walkedFlips = partial.walkedFlips + (int) cost[0];
                        double walkedLength = partial.walkedLength + cost[1];
                        int[] extended = partial.sides;
                        if (extended.length < count) {
                            extended = Arrays.copyOf(partial.sides, partial.sides.length + 1);
                            extended[extended.length - 1] = nextSide;
                        }
                        queue.add(new Partial(
                            walkedFlips + flipsTo[partial.step + 1][to],
                            walkedLength + lengthTo[partial.step + 1][to],
                            walkedFlips, walkedLength, partial.step + 1, to, extended
                        ));
                    }
                }
            }
        }
        return best;
    }

    private static double[] stepCost(double[] xs, double[] ys, double[] radii,
                                     int node, int previousSide, int currentSide, int nextSide) {
        int count = xs.length;
        int previous = (node - 1 + count) % count;
        int current = node % count;
        int next = (node + 1) % count;
        double[] incoming = tangent(xs[previous], ys[previous], previousSide * radii[previous],
            xs[current], ys[current], currentSide * radii[current]);
        double[] outgoing = tangent(xs[current], ys[current], currentSide * radii[current],
            xs[next], ys[next], nextSide * radii[next]);
        if (incoming == null || outgoing == null) {
            return null;
        }
        double angle = sweep(currentSide, incoming[3], incoming[4], outgoing[1], outgoing[2]);
        return new double[]{currentSide == nextSide ? 0.0 : 1.0, angle * radii[current] + outgoing[0]};
    }

    private static double beltLength(double[] xs, double[] ys, double[] radii, int[] sides) {
        int count = xs.length;
        double total = 0.0;
        double[][] runs = new double[count][];
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            runs[i] = tangent(xs[i], ys[i], sides[i] * radii[i], xs[next], ys[next], sides[next] * radii[next]);
            if (runs[i] == null) {
                return Double.MAX_VALUE;
            }
            total += runs[i][0];
        }
        for (int i = 0; i < count; i++) {
            double[] incoming = runs[(i - 1 + count) % count];
            total += sweep(sides[i], incoming[3], incoming[4], runs[i][1], runs[i][2]) * radii[i];
        }
        return total;
    }

    private static int countFlips(int[] sides) {
        int flips = 0;
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] != sides[(i + 1) % sides.length]) {
                flips++;
            }
        }
        return flips;
    }

    static double[] tangent(double xi, double yi, double ai, double xj, double yj, double aj) {
        double dx = xj - xi;
        double dy = yj - yi;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double delta = aj - ai;
        if (distance <= Math.abs(delta) + TANGENT_TOLERANCE) {
            return null;
        }
        double ux = dx / distance;
        double uy = dy / distance;
        double cos = delta / distance;
        double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
        double mx = -cos * ux - sin * uy;
        double my = -cos * uy + sin * ux;
        return new double[]{
            Math.sqrt(distance * distance - delta * delta),
            ai * mx, ai * my, aj * mx, aj * my
        };
    }

    static double sweep(int side, double entryX, double entryY, double exitX, double exitY) {
        double angle = (Math.atan2(exitY, exitX) - Math.atan2(entryY, entryX)) * -side;
        angle %= TAU;
        return angle < 0.0 ? angle + TAU : angle;
    }

    static long crossings(double[] xs, double[] ys, double[] radii, int[] sides) {
        int count = xs.length;
        double[][] runs = new double[count][];
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            runs[i] = tangent(xs[i], ys[i], sides[i] * radii[i], xs[next], ys[next], sides[next] * radii[next]);
            if (runs[i] == null) {
                return Long.MAX_VALUE;
            }
        }

        int primitives = count * 2;
        double[][] parts = new double[primitives][];
        for (int i = 0; i < count; i++) {
            double[] incoming = runs[(i - 1 + count) % count];
            double angle = sweep(sides[i], incoming[3], incoming[4], runs[i][1], runs[i][2]);
            parts[i * 2] = new double[]{
                0.0, xs[i], ys[i], radii[i],
                Math.atan2(incoming[4], incoming[3]), angle, -sides[i]
            };
            int next = (i + 1) % count;
            parts[i * 2 + 1] = new double[]{
                1.0, xs[i] + runs[i][1], ys[i] + runs[i][2],
                xs[next] + runs[i][3], ys[next] + runs[i][4]
            };
        }

        long found = 0;
        for (int i = 0; i < primitives; i++) {
            for (int j = i + 1; j < primitives; j++) {
                if (j == i + 1 || (i == 0 && j == primitives - 1)) {
                    continue;
                }
                if (intersects(parts[i], parts[j])) {
                    found++;
                }
            }
        }
        return found;
    }

    private static boolean intersects(double[] a, double[] b) {
        if (a[0] == 1.0 && b[0] == 1.0) {
            return segmentsCross(a[1], a[2], a[3], a[4], b[1], b[2], b[3], b[4]);
        }
        if (a[0] == 1.0) {
            return segmentCrossesArc(a, b);
        }
        if (b[0] == 1.0) {
            return segmentCrossesArc(b, a);
        }
        return arcsCross(a, b);
    }

    private static double cross(double ox, double oy, double ax, double ay, double bx, double by) {
        return (ax - ox) * (by - oy) - (ay - oy) * (bx - ox);
    }

    private static boolean segmentsCross(double px, double py, double qx, double qy,
                                         double rx, double ry, double sx, double sy) {
        double d1 = cross(rx, ry, sx, sy, px, py);
        double d2 = cross(rx, ry, sx, sy, qx, qy);
        double d3 = cross(px, py, qx, qy, rx, ry);
        double d4 = cross(px, py, qx, qy, sx, sy);
        double eps = 1.0E-9;
        return ((d1 > eps && d2 < -eps) || (d1 < -eps && d2 > eps))
            && ((d3 > eps && d4 < -eps) || (d3 < -eps && d4 > eps));
    }

    private static boolean onArc(double[] arc, double angle) {
        double relative = (angle - arc[4]) * arc[6];
        relative %= TAU;
        if (relative < 0.0) {
            relative += TAU;
        }
        return relative > 1.0E-7 && relative < arc[5] - 1.0E-7;
    }

    private static boolean segmentCrossesArc(double[] segment, double[] arc) {
        double dx = segment[3] - segment[1];
        double dy = segment[4] - segment[2];
        double fx = segment[1] - arc[1];
        double fy = segment[2] - arc[2];
        double a = dx * dx + dy * dy;
        double b = 2.0 * (fx * dx + fy * dy);
        double c = fx * fx + fy * fy - arc[3] * arc[3];
        double discriminant = b * b - 4.0 * a * c;
        if (a < 1.0E-12 || discriminant <= 1.0E-12) {
            return false;
        }
        double root = Math.sqrt(discriminant);
        for (int sign = -1; sign <= 1; sign += 2) {
            double t = (-b + sign * root) / (2.0 * a);
            if (t <= 1.0E-7 || t >= 1.0 - 1.0E-7) {
                continue;
            }
            double x = segment[1] + t * dx - arc[1];
            double y = segment[2] + t * dy - arc[2];
            if (onArc(arc, Math.atan2(y, x))) {
                return true;
            }
        }
        return false;
    }

    private static boolean arcsCross(double[] first, double[] second) {
        double dx = second[1] - first[1];
        double dy = second[2] - first[2];
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1.0E-9
            || distance > first[3] + second[3] - 1.0E-9
            || distance < Math.abs(first[3] - second[3]) + 1.0E-9) {
            return false;
        }
        double along = (distance * distance + first[3] * first[3] - second[3] * second[3]) / (2.0 * distance);
        double heightSquared = first[3] * first[3] - along * along;
        if (heightSquared <= 1.0E-12) {
            return false;
        }
        double height = Math.sqrt(heightSquared);
        double midX = first[1] + along * dx / distance;
        double midY = first[2] + along * dy / distance;
        for (int sign = -1; sign <= 1; sign += 2) {
            double x = midX + sign * height * -dy / distance;
            double y = midY + sign * height * dx / distance;
            if (onArc(first, Math.atan2(y - first[2], x - first[1]))
                && onArc(second, Math.atan2(y - second[2], x - second[1]))) {
                return true;
            }
        }
        return false;
    }

    private static int state(int firstSide, int secondSide) {
        return (firstSide > 0 ? 0 : 1) * 2 + (secondSide > 0 ? 0 : 1);
    }

    private static int sideOfStateFirst(int state) {
        return state < 2 ? 1 : -1;
    }

    private static int sideOfStateSecond(int state) {
        return (state & 1) == 0 ? 1 : -1;
    }

    private static final class Partial implements Comparable<Partial> {
        private final int flips;
        private final double length;
        private final int walkedFlips;
        private final double walkedLength;
        private final int step;
        private final int state;
        private final int[] sides;

        private Partial(int flips, double length, int walkedFlips, double walkedLength,
                        int step, int state, int[] sides) {
            this.flips = flips;
            this.length = length;
            this.walkedFlips = walkedFlips;
            this.walkedLength = walkedLength;
            this.step = step;
            this.state = state;
            this.sides = sides;
        }

        @Override
        public int compareTo(Partial other) {
            if (this.flips != other.flips) {
                return Integer.compare(this.flips, other.flips);
            }
            return Double.compare(this.length, other.length);
        }
    }
}
