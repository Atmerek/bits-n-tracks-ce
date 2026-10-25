package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder.VertexEmitter;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BntTankTread {
    private static final double PX = 1.0 / 16.0;
    private static final double SHEET_U = 32.0;
    private static final double SHEET_V = 16.0;

    public static final double CELL = 4.0 * PX;
    private static final double MIN_SPAN = 1.0E-3;
    private static final double CELL_START = -2.0;
    private static final double CENTRE_Y = 1.0;
    private static final int WHITE = -1;

    private static final double EDGE_ON = 0.25;

    private static final double[][] TREAD_UV = {
        {0, 0, 14, 3, 0},
        {14, 0, 28, 3, 180},
        {14, 5, 0, 3, 0},
        {28, 5, 14, 3, 0},
        {28, 0, 30, 3, 90},
        {13, 0, 15, 3, 270}
    };

    private static final double[][] TREAD_UV_MIRRORED = {
        {14, 0, 0, 3, 0},
        {28, 0, 14, 3, 180},
        {0, 5, 14, 3, 0},
        {14, 5, 28, 3, 0},
        {30, 0, 28, 3, 90},
        {15, 0, 13, 3, 270}
    };

    private static final double[][] LINK_UV = {
        {3, 6, 6, 7, 0},
        {3, 8, 6, 9, 0},
        {3, 7, 6, 8, 0},
        {3, 7, 6, 8, 0},
        {6, 6, 7, 7, 0},
        {2, 6, 3, 7, 0}
    };

    private static final int[][] FACE_CORNERS = {
        {2, 3, 7, 6},
        {1, 0, 4, 5},
        {2, 0, 4, 6},
        {7, 5, 1, 3},
        {6, 4, 5, 7},
        {3, 1, 0, 2}
    };

    private static final Part[] NARROW = {
        Part.of(-7, 0, -2, 7, 2, 1, TREAD_UV, false),
        Part.of(-4, 0.5, 0, -1, 1.5, 3, LINK_UV, true),
        Part.of(1, 0.5, 0, 4, 1.5, 3, LINK_UV, true)
    };

    private static final Part[] WIDE = {
        Part.of(0, 0, -2, 14, 2, 1, TREAD_UV, false),
        Part.of(8, 0.5, 0, 12, 1.5, 3, LINK_UV, true),
        Part.of(2, 0.5, 0, 6, 1.5, 3, LINK_UV, true),
        Part.of(-14, 0, -2, 0, 2, 1, TREAD_UV_MIRRORED, false),
        Part.of(-11, 0.5, 0, -8, 1.5, 3, LINK_UV, true),
        Part.of(-6, 0.5, 0, -2, 1.5, 3, LINK_UV, true)
    };

    private static final double PLATE = 14.0;
    private static final double[][] NARROW_LINKS = {{-4, -1}, {1, 4}};
    private static final double[][] WIDE_LINKS = {{8, 12}, {2, 6}, {-11, -8}, {-6, -2}};
    private static final Part[][] PARTS = new Part[BntCogwheelWidth.PAIR + 1][];

    private static final double BELT_FACE = 2.0;
    private static final double MODEL_CENTRE = 8.0;
    private static final double GROUSER_MIDDLE = 3.0;
    private static final int[] ACROSS_FIRST = {0, 3, 2, 1};
    private static final int[] ACROSS_LAST = {2, 1, 0, 3};
    private static final double LINK_PLATE = 1.5 / 16.0;
    private static volatile Grousers grousers = new Grousers(null, new Part[BntCogwheelWidth.PAIR + 1][]);

    static {
        for (int quarters = 1; quarters <= BntCogwheelWidth.PAIR; quarters++) {
            PARTS[quarters] = parts(quarters);
        }
    }

    private record Grousers(BntGrouserModel.Model model, Part[][] parts) {
    }

    public static ResourceLocation grouserTexture() {
        return BntGrouserModel.model().texture();
    }

    private static Part[] grouserParts(int quarters) {
        BntGrouserModel.Model model = BntGrouserModel.model();
        Grousers built = grousers;
        if (built.model() != model) {
            Part[][] parts = new Part[BntCogwheelWidth.PAIR + 1][];
            for (int q = 1; q <= BntCogwheelWidth.PAIR; q++) {
                parts[q] = grouserParts(model, q);
            }
            built = new Grousers(model, parts);
            grousers = built;
        }
        return built.parts()[Mth.clamp(quarters, 1, BntCogwheelWidth.PAIR)];
    }

    private static Part[] grouserParts(BntGrouserModel.Model model, int quarters) {
        double scale = BntCogwheelWidth.trackPixels(quarters) / (double)BntCogwheelWidth.trackPixels(BntCogwheelWidth.SINGLE);
        List<Part> parts = new ArrayList<>();
        for (BntGrouserModel.Element element : model.elements()) {
            double low = element.from()[0] - MODEL_CENTRE;
            double high = element.to()[0] - MODEL_CENTRE;
            if (low < 0.0 && high > 0.0) {
                spanParts(parts, element, low, high, scale);
            } else {
                double shift = (low + high) * 0.5 * (scale - 1.0);
                parts.add(grouserPart(element, low + shift, high + shift, element.faces()));
            }
        }
        return parts.toArray(Part[]::new);
    }

    private static void spanParts(List<Part> parts, BntGrouserModel.Element element, double low, double high, double scale) {
        double outerLow = low * scale;
        double outerHigh = high * scale;
        double width = outerHigh - outerLow;
        double lowEnd = Math.max(0.0, -GROUSER_MIDDLE - low);
        double highEnd = Math.max(0.0, high - GROUSER_MIDDLE);
        if (width <= lowEnd + highEnd) {
            double split = outerLow + width * lowEnd / Math.max(lowEnd + highEnd, MIN_SPAN);
            parts.add(spanPart(element, low, high, outerLow, split, low, low + split - outerLow));
            parts.add(spanPart(element, low, high, split, outerHigh, high - (outerHigh - split), high));
            return;
        }
        parts.add(spanPart(element, low, high, outerLow, outerLow + lowEnd, low, low + lowEnd));
        parts.add(spanPart(element, low, high, outerHigh - highEnd, outerHigh, high - highEnd, high));
        double fill = width - lowEnd - highEnd;
        double middle = high - highEnd - (low + lowEnd);
        double centre = (low + lowEnd + high - highEnd) * 0.5;
        int tiles = middle > MIN_SPAN ? (int)Math.ceil(fill / middle - MIN_SPAN) : 1;
        double tile = fill / tiles;
        double source = Math.min(tile, middle) * 0.5;
        for (int i = 0; i < tiles; i++) {
            double at = outerLow + lowEnd + i * tile;
            parts.add(spanPart(element, low, high, at, at + tile, centre - source, centre + source));
        }
    }

    private static Part spanPart(
        BntGrouserModel.Element element, double low, double high, double fromX, double toX, double sourceFrom, double sourceTo
    ) {
        float[][] faces = element.faces();
        float[][] uv = new float[faces.length][];
        for (int face = 0; face < 4; face++) {
            uv[face] = faces[face] == null ? null : cropAcross(faces[face], face, low, high, sourceFrom, sourceTo);
        }
        uv[4] = sourceTo >= high - MIN_SPAN ? faces[4] : null;
        uv[5] = sourceFrom <= low + MIN_SPAN ? faces[5] : null;
        return grouserPart(element, fromX, toX, uv);
    }

    private static float[] cropAcross(float[] uv, int face, double low, double high, double from, double to) {
        int turn = (int)uv[4] & 3;
        int first = ACROSS_FIRST[turn];
        int last = ACROSS_LAST[turn];
        if (face == 2) {
            int swap = first;
            first = last;
            last = swap;
        }
        float[] cropped = uv.clone();
        double span = high - low;
        cropped[first] = (float)(uv[first] + (uv[last] - uv[first]) * (from - low) / span);
        cropped[last] = (float)(uv[first] + (uv[last] - uv[first]) * (to - low) / span);
        return cropped;
    }

    private static Part grouserPart(BntGrouserModel.Element element, double fromX, double toX, float[][] uv) {
        float[] from = element.from();
        float[] to = element.to();
        return Part.of(
            fromX, from[1] + BELT_FACE, from[2] - MODEL_CENTRE + CELL_START,
            toX, to[1] + BELT_FACE, to[2] - MODEL_CENTRE + CELL_START,
            uv);
    }

    private BntTankTread() {
    }

    private static Part[] parts(int quarters) {
        if (quarters == BntCogwheelWidth.SINGLE) {
            return NARROW;
        }
        if (quarters == BntCogwheelWidth.PAIR) {
            return WIDE;
        }
        boolean wide = quarters > BntCogwheelWidth.SINGLE;
        int half = BntCogwheelWidth.trackPixels(quarters) / 2;
        double fullHalf = wide ? PLATE : PLATE / 2.0;
        List<Part> parts = new ArrayList<>();
        if (wide) {
            parts.add(Part.of(0, 0, -2, half, 2, 1, crop(TREAD_UV, 0.0, half / PLATE), false));
            parts.add(Part.of(-half, 0, -2, 0, 2, 1, crop(TREAD_UV_MIRRORED, 1.0 - half / PLATE, 1.0), false));
        } else {
            parts.add(Part.of(-half, 0, -2, 0, 2, 1, crop(TREAD_UV, 1.0 - half / PLATE, 1.0), false));
            parts.add(Part.of(0, 0, -2, half, 2, 1, crop(TREAD_UV, 0.0, half / PLATE), false));
        }
        for (double[] link : wide ? WIDE_LINKS : NARROW_LINKS) {
            double from = Math.round(link[0] * half / fullHalf);
            double to = Math.round(link[1] * half / fullHalf);
            if (to > from) {
                parts.add(Part.of(from, 0.5, 0, to, 1.5, 3, LINK_UV, true));
            }
        }
        return parts.toArray(Part[]::new);
    }

    private static double[][] crop(double[][] uv, double from, double to) {
        double[][] cropped = new double[uv.length][];
        for (int face = 0; face < uv.length; face++) {
            cropped[face] = uv[face].clone();
        }
        for (int face = 0; face < 4; face++) {
            boolean lowFirst = face == 0 || face == 2;
            double low = uv[face][lowFirst ? 0 : 2];
            double high = uv[face][lowFirst ? 2 : 0];
            cropped[face][lowFirst ? 0 : 2] = low + (high - low) * from;
            cropped[face][lowFirst ? 2 : 0] = low + (high - low) * to;
        }
        return cropped;
    }

    public record Target(
        VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int lightFrom, int lightTo, Vec3 relTo, Vec3 eye,
        boolean links, BntTrackSink sink
    ) {
    }

    public static void emitSegment(
        Target target, List<Vec3> source, List<Vec3> destination, double start, double span, int quarters,
        boolean frameTopOutward
    ) {
        emit(target, source, destination, start, span, PARTS[Mth.clamp(quarters, 1, BntCogwheelWidth.PAIR)], CELL, 1, 0.0, 0.0, false,
            frameTopOutward);
    }

    public static void emitGrousers(
        Target target, List<Vec3> source, List<Vec3> destination, double start, double span, double repeat, int quarters,
        boolean frameTopOutward
    ) {
        int bars = Math.max(1, (int)Math.round(repeat / BntGrousers.spacing()));
        emit(target, source, destination, start, span, grouserParts(quarters), repeat, bars, repeat / bars, -LINK_PLATE * repeat,
            true, frameTopOutward);
    }

    private static void emit(
        Target target, List<Vec3> source, List<Vec3> destination, double start, double span, Part[] parts, double cell,
        int repeats, double spacing, double shift, boolean rigid, boolean frameTopOutward
    ) {
        if (source.size() != 4 || destination.size() != 4 || span <= MIN_SPAN || cell <= MIN_SPAN) {
            return;
        }

        double rate = target.sink() == null ? 0.0 : target.sink().scrollRate();
        double ahead = target.sink() == null ? 0.0 : Math.abs(rate) * target.sink().reach();
        Segment segment = new Segment(target, source, destination, frameTopOutward ? 1.0 : -1.0, rate / span);
        double from = rate < 0.0 ? start - ahead : start;
        double to = rate > 0.0 ? start + span + ahead : start + span;
        long first = (long)Math.floor(from / cell) - 1L;
        long last = (long)Math.ceil(to / cell) + 1L;

        for (long plate = first; plate <= last; plate++) {
            for (int repeat = 0; repeat < repeats; repeat++) {
                double base = plate * cell + shift + repeat * spacing;
                if (rigid) {
                    placeRigid(segment, start, span, from, to, base, parts);
                    continue;
                }
                for (Part part : parts) {
                    if (!part.link() || target.links()) {
                        place(segment, start, span, from, to, base + part.at(), part);
                    }
                }
            }
        }
    }

    private static void placeRigid(Segment segment, double offset, double length, double from, double to, double at, Part[] parts) {
        if (at < from || at >= to) {
            return;
        }
        double along = (offset + length - at) / length;
        Vec3[] frame = frame(segment, along);
        if (frame == null) {
            return;
        }
        for (Part part : parts) {
            Vec3 seat = frame[0].add(frame[3].scale(-part.at())).add(frame[1].scale(part.across())).add(frame[2].scale(part.up()));
            box(segment, seat, frame[1].scale(part.halfWidth()), frame[2].scale(part.halfThick()),
                frame[3].scale(part.halfLength()), part.uv(), along);
        }
    }

    private static Vec3[] frame(Segment segment, double along) {
        double inside = Mth.clamp(along, 0.0, 1.0);
        Vec3 frame0 = segment.source.get(0).lerp(segment.destination.get(0), inside);
        Vec3 frame1 = segment.source.get(1).lerp(segment.destination.get(1), inside);
        Vec3 frame2 = segment.source.get(2).lerp(segment.destination.get(2), inside);
        Vec3 across = unit(frame0.subtract(frame1)).scale(segment.facing);
        Vec3 thick = unit(frame1.subtract(frame2)).scale(segment.facing);
        if (across.lengthSqr() < 0.5 || thick.lengthSqr() < 0.5) {
            return null;
        }
        Vec3 forward = segment.forward;
        if (forward.lengthSqr() < 0.5) {
            forward = unit(across.cross(thick));
            if (forward.lengthSqr() < 0.5) {
                return null;
            }
        }
        return new Vec3[]{segment.origin.add(segment.run.scale(along)), across, thick, forward};
    }

    private static void place(Segment segment, double offset, double length, double from, double to, double at, Part part) {
        if (at < from || at >= to) {
            return;
        }

        double along = (offset + length - at) / length;
        Vec3[] frame = frame(segment, along);
        if (frame == null) {
            return;
        }
        Vec3 seat = frame[0].add(frame[1].scale(part.across())).add(frame[2].scale(part.up()));
        box(segment, seat, frame[1].scale(part.halfWidth()), frame[2].scale(part.halfThick()),
            frame[3].scale(part.halfLength()), part.uv(), along);
    }

    private static Vec3 sourceCentre(List<Vec3> points) {
        return points.get(0).add(points.get(1)).add(points.get(2)).add(points.get(3)).scale(0.25);
    }

    private static Vec3 unit(Vec3 v) {
        double length = v.length();
        return length < 1.0E-9 ? Vec3.ZERO : v.scale(1.0 / length);
    }

    private static void box(Segment segment, Vec3 c, Vec3 w, Vec3 t, Vec3 l, float[][] uv, double along) {
        Target target = segment.target;
        if (target.sink() != null) {
            target.sink().box(uv, c, w, t, l, segment.lightAt(c), segment.drift, along, segment.alongRate);
            return;
        }
        int visible = visibleFaces(target.eye(), c, w, t, l);
        if (visible == 0) {
            return;
        }

        double[] cx = segment.cx;
        double[] cy = segment.cy;
        double[] cz = segment.cz;
        for (int corner = 0; corner < 8; corner++) {
            double sw = (corner & 4) != 0 ? 1.0 : -1.0;
            double st = (corner & 2) != 0 ? 1.0 : -1.0;
            double sl = (corner & 1) != 0 ? 1.0 : -1.0;
            cx[corner] = c.x + sw * w.x + st * t.x + sl * l.x;
            cy[corner] = c.y + sw * w.y + st * t.y + sl * l.y;
            cz[corner] = c.z + sw * w.z + st * t.z + sl * l.z;
        }

        float[] px = segment.px;
        float[] py = segment.py;
        float[] pz = segment.pz;
        Vector3f scratch = segment.scratch;
        for (int corner = 0; corner < 8; corner++) {
            target.pose().transformPosition((float)cx[corner], (float)cy[corner], (float)cz[corner], scratch);
            px[corner] = scratch.x;
            py[corner] = scratch.y;
            pz[corner] = scratch.z;
        }

        int light = segment.lightAt(c);
        for (int face = 0; face < 6; face++) {
            if ((visible & (1 << face)) != 0 && uv[face] != null) {
                face(target, c, cx, cy, cz, px, py, pz, FACE_CORNERS[face], uv[face], light, scratch);
            }
        }
    }

    private static int visibleFaces(Vec3 eye, Vec3 c, Vec3 w, Vec3 t, Vec3 l) {
        if (eye == null) {
            return 0b111111;
        }

        Vec3 toEye = eye.subtract(c);
        Vec3 up = outward(w.cross(l), t);
        Vec3 ahead = outward(w.cross(t), l);
        Vec3 side = outward(t.cross(l), w);
        int visible = 0;
        if (facesEye(toEye, t, up)) {
            visible |= 1;
        }
        if (facesEye(toEye, t.scale(-1.0), up.scale(-1.0))) {
            visible |= 1 << 1;
        }
        if (facesEye(toEye, l.scale(-1.0), ahead.scale(-1.0))) {
            visible |= 1 << 2;
        }
        if (facesEye(toEye, l, ahead)) {
            visible |= 1 << 3;
        }
        if (facesEye(toEye, w, side)) {
            visible |= 1 << 4;
        }
        if (facesEye(toEye, w.scale(-1.0), side.scale(-1.0))) {
            visible |= 1 << 5;
        }
        return visible;
    }

    private static Vec3 outward(Vec3 normal, Vec3 offset) {
        return normal.dot(offset) < 0.0 ? normal.scale(-1.0) : normal;
    }

    private static boolean facesEye(Vec3 toEye, Vec3 offset, Vec3 normal) {
        double length = normal.length();
        return length < 1.0E-12 || toEye.subtract(offset).dot(normal) > -EDGE_ON * length;
    }

    private static void face(
        Target target, Vec3 middle, double[] cx, double[] cy, double[] cz, float[] px, float[] py, float[] pz,
        int[] corners, float[] uv, int light, Vector3f normal
    ) {
        int a = corners[0];
        int b = corners[1];
        int d = corners[2];
        int e = corners[3];
        double abx = cx[b] - cx[a];
        double aby = cy[b] - cy[a];
        double abz = cz[b] - cz[a];
        double aex = cx[e] - cx[a];
        double aey = cy[e] - cy[a];
        double aez = cz[e] - cz[a];
        double nx = aby * aez - abz * aey;
        double ny = abz * aex - abx * aez;
        double nz = abx * aey - aby * aex;
        double ox = (cx[a] + cx[d]) * 0.5 - middle.x;
        double oy = (cy[a] + cy[d]) * 0.5 - middle.y;
        double oz = (cz[a] + cz[d]) * 0.5 - middle.z;
        boolean flip = nx * ox + ny * oy + nz * oz < 0.0;

        int turn = (int)uv[4];
        double outward = flip ? -1.0 : 1.0;
        target.normal().transform((float)(nx * outward), (float)(ny * outward), (float)(nz * outward), normal);
        if (normal.lengthSquared() < 1.0E-12F) {
            target.normal().transform(0.0F, 1.0F, 0.0F, normal);
        }
        normal.normalize();
        for (int i = 0; i < 4; i++) {
            int corner = flip ? corners[3 - i] : corners[i];
            int st = (i + turn) & 3;
            float u = st < 2 ? uv[0] : uv[2];
            float v = st == 0 || st == 3 ? uv[1] : uv[3];
            target.consumer().addVertex(
                px[corner], py[corner], pz[corner], WHITE, u, v, OverlayTexture.NO_OVERLAY, light,
                normal.x, normal.y, normal.z);
        }
    }

    public static void unitBox(float[][] uv, boolean mirrored, VertexEmitter out) {
        double[] cx = new double[8];
        double[] cy = new double[8];
        double[] cz = new double[8];
        for (int corner = 0; corner < 8; corner++) {
            cx[corner] = (corner & 4) != 0 ? 1.0 : -1.0;
            cy[corner] = (corner & 2) != 0 ? 1.0 : -1.0;
            cz[corner] = (corner & 1) != 0 ? 1.0 : -1.0;
        }
        for (int face = 0; face < 6; face++) {
            if (uv[face] == null) {
                continue;
            }
            int[] corners = FACE_CORNERS[face];
            int a = corners[0];
            int b = corners[1];
            int d = corners[2];
            int e = corners[3];
            double nx = (cy[b] - cy[a]) * (cz[e] - cz[a]) - (cz[b] - cz[a]) * (cy[e] - cy[a]);
            double ny = (cz[b] - cz[a]) * (cx[e] - cx[a]) - (cx[b] - cx[a]) * (cz[e] - cz[a]);
            double nz = (cx[b] - cx[a]) * (cy[e] - cy[a]) - (cy[b] - cy[a]) * (cx[e] - cx[a]);
            double ox = (cx[a] + cx[d]) * 0.5;
            double oy = (cy[a] + cy[d]) * 0.5;
            double oz = (cz[a] + cz[d]) * 0.5;
            boolean flip = (nx * ox + ny * oy + nz * oz < 0.0) != mirrored;
            double wound = (flip ? -1.0 : 1.0) / Math.sqrt(nx * nx + ny * ny + nz * nz);
            float fx = (float)(nx * wound);
            float fy = (float)(ny * wound);
            float fz = (float)(nz * wound);
            float[] faceUv = uv[face];
            int turn = (int)faceUv[4];
            for (int i = 0; i < 4; i++) {
                int corner = flip ? corners[3 - i] : corners[i];
                int st = (i + turn) & 3;
                float u = st < 2 ? faceUv[0] : faceUv[2];
                float v = st == 0 || st == 3 ? faceUv[1] : faceUv[3];
                out.emit((float)cx[corner], (float)cy[corner], (float)(cz[corner] * 0.5 + 0.5), u, v, fx, fy, fz);
            }
        }
    }

    private static int lerpPackedLight(int from, int to, float t) {
        int block = (int)Mth.lerp(t, from & 65535, to & 65535);
        int sky = (int)Mth.lerp(t, from >> 16 & 65535, to >> 16 & 65535);
        return block | sky << 16;
    }

    private static float[][] sheet(double[][] uv) {
        float[][] faces = new float[uv.length][];
        for (int face = 0; face < uv.length; face++) {
            faces[face] = new float[]{
                (float)(uv[face][0] / SHEET_U),
                (float)(uv[face][1] / SHEET_V),
                (float)(uv[face][2] / SHEET_U),
                (float)(uv[face][3] / SHEET_V),
                ((int)uv[face][4] / 90) & 3
            };
        }
        return faces;
    }

    private static final class Segment {
        private final Target target;
        private final List<Vec3> source;
        private final List<Vec3> destination;
        private final double facing;
        private final Vec3 origin;
        private final Vec3 run;
        private final Vec3 forward;
        private final double lengthSqr;
        private final double alongRate;
        private final Vec3 drift;
        private final Vector3f scratch = new Vector3f();
        private final double[] cx = new double[8];
        private final double[] cy = new double[8];
        private final double[] cz = new double[8];
        private final float[] px = new float[8];
        private final float[] py = new float[8];
        private final float[] pz = new float[8];

        private Segment(Target target, List<Vec3> source, List<Vec3> destination, double facing, double alongRate) {
            this.target = target;
            this.source = source;
            this.destination = destination;
            this.facing = facing;
            this.origin = sourceCentre(source);
            Vec3 run = sourceCentre(destination).subtract(this.origin);
            this.run = run;
            this.forward = unit(run);
            this.lengthSqr = target.relTo().lengthSqr();
            this.alongRate = alongRate;
            this.drift = run.scale(alongRate);
        }

        private int lightAt(Vec3 middle) {
            float t = this.lengthSqr > 1.0E-8
                ? Mth.clamp((float)(middle.dot(this.target.relTo()) / this.lengthSqr), 0.0F, 1.0F)
                : 0.0F;
            return lerpPackedLight(this.target.lightFrom(), this.target.lightTo(), t);
        }
    }

    private record Part(
        double across, double up, double at, double halfWidth, double halfThick, double halfLength, float[][] uv,
        boolean link
    ) {
        static Part of(
            double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double[][] uv, boolean link
        ) {
            return new Part(
                (fromX + toX) * 0.5 * PX,
                ((fromY + toY) * 0.5 - CENTRE_Y) * PX,
                ((fromZ + toZ) * 0.5 - CELL_START) * PX,
                (toX - fromX) * 0.5 * PX,
                (toY - fromY) * 0.5 * PX,
                (toZ - fromZ) * 0.5 * PX,
                sheet(uv),
                link);
        }

        static Part of(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, float[][] uv) {
            return new Part(
                (fromX + toX) * 0.5 * PX,
                ((fromY + toY) * 0.5 - CENTRE_Y) * PX,
                ((fromZ + toZ) * 0.5 - CELL_START) * PX,
                (toX - fromX) * 0.5 * PX,
                (toY - fromY) * 0.5 * PX,
                (toZ - fromZ) * 0.5 * PX,
                uv,
                false);
        }
    }
}
