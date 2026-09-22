package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Tread plates joined by links, one plate and its links per link pitch. */
public final class BntTankTread {
    private static final double PX = 1.0 / 16.0;
    private static final double SHEET_U = 32.0;
    private static final double SHEET_V = 16.0;

    public static final double CELL = 4.0 * PX;
    private static final double CELL_START = -2.0;
    private static final double CENTRE_Y = 1.0;
    private static final int WHITE = -1;

    /** A face is still drawn while the eye sits this close behind its plane, which covers view bobbing. */
    private static final double EDGE_ON = 0.25;

    /** up, down, north, south, east, west. Each entry is u0, v0, u1, v1, rotation. */
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

    /** Corners of each face, as bits of w, t and l, in the order the faces were first wound. */
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
        Part.of(-4, 0.5, 1, -1, 1.5, 2, LINK_UV, true),
        Part.of(1, 0.5, 1, 4, 1.5, 2, LINK_UV, true)
    };

    private static final Part[] WIDE = {
        Part.of(0, 0, -2, 14, 2, 1, TREAD_UV, false),
        Part.of(8, 0.5, 1, 12, 1.5, 2, LINK_UV, true),
        Part.of(2, 0.5, 1, 6, 1.5, 2, LINK_UV, true),
        Part.of(-14, 0, -2, 0, 2, 1, TREAD_UV_MIRRORED, false),
        Part.of(-11, 0.5, 1, -8, 1.5, 2, LINK_UV, true),
        Part.of(-6, 0.5, 1, -2, 1.5, 2, LINK_UV, true)
    };

    private BntTankTread() {
    }

    /**
     * Where one segment's boxes are written and how they are lit. The eye is relative to the segment's start and null
     * when every face has to be drawn, and links are left out where they would be under a pixel.
     */
    public record Target(
        VertexConsumer consumer, Matrix4f pose, Vector3f normal, int lightFrom, int lightTo, Vec3 relTo, Vec3 eye,
        boolean links
    ) {
    }

    /** Emits every box whose middle falls inside this segment, given where it starts and runs on the belt. */
    public static void emitSegment(
        Target target, List<Vec3> source, List<Vec3> destination, double start, double span, boolean wide,
        boolean frameTopOutward
    ) {
        if (source.size() != 4 || destination.size() != 4 || span <= 1.0E-6) {
            return;
        }

        Segment segment = new Segment(target, source, destination, frameTopOutward ? 1.0 : -1.0);
        double end = start + span;
        long first = (long)Math.floor(start / CELL) - 1L;
        long last = (long)Math.ceil(end / CELL) + 1L;

        for (long plate = first; plate <= last; plate++) {
            double base = plate * CELL;
            for (Part part : wide ? WIDE : NARROW) {
                if (!part.link() || target.links()) {
                    place(segment, start, span, base + part.at(), part);
                }
            }
        }
    }

    private static void place(Segment segment, double offset, double length, double at, Part part) {
        if (at < offset || at >= offset + length) {
            return;
        }

        double along = (offset + length - at) / length;
        Vec3 frame0 = segment.source.get(0).lerp(segment.destination.get(0), along);
        Vec3 frame1 = segment.source.get(1).lerp(segment.destination.get(1), along);
        Vec3 frame2 = segment.source.get(2).lerp(segment.destination.get(2), along);
        Vec3 frame3 = segment.source.get(3).lerp(segment.destination.get(3), along);

        Vec3 centre = frame0.add(frame1).add(frame2).add(frame3).scale(0.25);
        Vec3 across = unit(frame0.subtract(frame1)).scale(segment.facing);
        Vec3 thick = unit(frame1.subtract(frame2)).scale(segment.facing);
        if (across.lengthSqr() < 0.5 || thick.lengthSqr() < 0.5) {
            return;
        }

        Vec3 forward = segment.forward;
        if (forward.lengthSqr() < 0.5) {
            forward = unit(across.cross(thick));
            if (forward.lengthSqr() < 0.5) {
                return;
            }
        }

        Vec3 seat = centre.add(across.scale(part.across())).add(thick.scale(part.up()));
        box(segment, seat, across.scale(part.halfWidth()), thick.scale(part.halfThick()),
            forward.scale(part.halfLength()), part.uv());
    }

    private static Vec3 sourceCentre(List<Vec3> points) {
        return points.get(0).add(points.get(1)).add(points.get(2)).add(points.get(3)).scale(0.25);
    }

    private static Vec3 unit(Vec3 v) {
        double length = v.length();
        return length < 1.0E-9 ? Vec3.ZERO : v.scale(1.0 / length);
    }

    private static void box(Segment segment, Vec3 c, Vec3 w, Vec3 t, Vec3 l, float[][] uv) {
        Target target = segment.target;
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
            if ((visible & (1 << face)) != 0) {
                face(target, c, cx, cy, cz, px, py, pz, FACE_CORNERS[face], uv[face], light);
            }
        }
    }

    /** Bit per face that looks toward the eye: up, down, north, south, east, west. */
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

    /** A plate on a bend is sheared, so a face looks along the cross of its edges rather than along its offset. */
    private static Vec3 outward(Vec3 normal, Vec3 offset) {
        return normal.dot(offset) < 0.0 ? normal.scale(-1.0) : normal;
    }

    /** Whether the eye is in front of the face whose middle sits at offset from the box's. */
    private static boolean facesEye(Vec3 toEye, Vec3 offset, Vec3 normal) {
        double length = normal.length();
        return length < 1.0E-12 || toEye.subtract(offset).dot(normal) > -EDGE_ON * length;
    }

    /** One face, wound so it looks away from the middle of the box. */
    private static void face(
        Target target, Vec3 middle, double[] cx, double[] cy, double[] cz, float[] px, float[] py, float[] pz,
        int[] corners, float[] uv, int light
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
        Vector3f normal = target.normal();
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
        private final Vec3 forward;
        private final double lengthSqr;
        private final Vector3f scratch = new Vector3f();
        private final double[] cx = new double[8];
        private final double[] cy = new double[8];
        private final double[] cz = new double[8];
        private final float[] px = new float[8];
        private final float[] py = new float[8];
        private final float[] pz = new float[8];

        private Segment(Target target, List<Vec3> source, List<Vec3> destination, double facing) {
            this.target = target;
            this.source = source;
            this.destination = destination;
            this.facing = facing;
            this.forward = unit(sourceCentre(destination).subtract(sourceCentre(source)));
            this.lengthSqr = target.relTo().lengthSqr();
        }

        /** Light blended along the segment at the box's middle, as the vertices each were before. */
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
    }
}
