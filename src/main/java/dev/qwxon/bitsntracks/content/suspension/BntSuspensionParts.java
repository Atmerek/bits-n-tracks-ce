package dev.qwxon.bitsntracks.content.suspension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class BntSuspensionParts {
    public static final int ARM = 0;
    public static final int BOGIE = 1;
    public static final int WIDE_BOGIE = 2;
    private static final float[][] QUADS = {BntSuspensionPieceModel.ARM, BntBogieModel.QUADS, BntWideBogieModel.QUADS};
    private static final int[][][] PART_QUADS = {split(QUADS[ARM]), split(QUADS[BOGIE]), split(QUADS[WIDE_BOGIE])};
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    public interface Sink {
        void part(int model, int part, Vec3 x, Vec3 y, Vec3 z, Vec3 origin, Vec3 normalX, Vec3 normalY);
    }

    private BntSuspensionParts() {
    }

    public static float[] quads(int model) {
        return QUADS[model];
    }

    public static int[] quadsOf(int model, int part) {
        int[][] parts = PART_QUADS[model];
        return part < parts.length ? parts[part] : new int[0];
    }

    public static ResourceLocation texture(int model) {
        return model == ARM ? BntSuspensionPieceRenderer.TEXTURE : BntBogieRenderer.TEXTURE;
    }

    public static void attached(KineticBlockEntity be, float partialTicks, Sink sink) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || !BntSuspension.hasPiece(be)) {
            return;
        }
        if (BntSuspension.isBogie(be)) {
            bogie(be, partialTicks, sink);
            return;
        }
        if (be.getLevel() != null && !BntSuspension.armEnd(
            be.getLevel(), be.getBlockPos(), BntSuspension.axis(be.getBlockState()), access.bnt$getSuspensionFacing()).equals(be.getBlockPos())) {
            return;
        }
        Vec3 cog = CENTRE.add(HiddenCogwheelCompat.getModelTranslation(be, partialTicks));
        arm(be, cog, access.bnt$getSuspensionSide(), access.bnt$getSuspensionFacing(), sink);
    }

    static void arm(KineticBlockEntity be, Vec3 cog, int side, int facing, Sink sink) {
        Axis axis = BntSuspension.axis(be.getBlockState());
        Vec3 across = BntSuspension.across(axis);
        BlockPos end = be.getLevel() == null ? be.getBlockPos() : BntSuspension.armEnd(be.getLevel(), be.getBlockPos(), axis, facing);
        Vec3 shift = Vec3.atLowerCornerOf(end.subtract(be.getBlockPos()));
        double seat = CogwheelSizeHelper.getVisualVerticalOffset(be.getBlockState().getBlock());
        Vec3 pivot = CENTRE.add(shift).add(0.0, seat + BntSuspension.PIVOT, 0.0).add(across.scale(side * BntSuspension.PIVOT));
        boolean holder = side > 0 || be.getLevel() == null || !BntSuspension.sharesPivot(be.getLevel(), end, axis, side);
        arm(cog.add(shift), pivot, across, BntSuspension.along(axis), side, facing, holder, sink);
    }

    private static void arm(Vec3 cog, Vec3 pivot, Vec3 across, Vec3 along, int side, int facing, boolean holder, Sink sink) {
        Vec3 depth = along.scale(facing);
        Vec3 reach = pivot.subtract(cog);
        Vec3 inPlane = reach.subtract(along.scale(reach.dot(along)));
        double length = inPlane.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 armward = inPlane.scale(1.0 / length);
        double armAcross = armward.dot(across);
        Vec3 normalward = across.scale(-side * armward.y).add(UP.scale(side * armAcross));
        Vec3 offAxis = pivot.subtract(cog.add(armward.scale(length)));
        double stretch = length - BntSuspensionPieceModel.ARM_LENGTH;
        double span = BntSuspensionPieceModel.SHAFT_END - BntSuspensionPieceModel.SHAFT_START;
        double start = BntSuspensionPieceModel.SHAFT_START;
        double grow = (span + stretch) / span;

        sink.part(ARM, 0, armward, normalward, depth, cog, armward, normalward);
        sink.part(ARM, 1, armward.scale(grow).add(offAxis.scale(1.0 / span)), normalward, depth,
            cog.add(armward.scale(start - start * grow)).subtract(offAxis.scale(start / span)), armward, normalward);
        if (holder) {
            sink.part(ARM, 2, armward, normalward, depth, cog.add(armward.scale(stretch)).add(offAxis), armward, normalward);
        }
    }

    static void bogie(KineticBlockEntity be, float partialTicks, Sink sink) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || be.getLevel() == null
            || access.bnt$getSuspensionSide() != BntSuspension.BOGIE && access.bnt$getSuspensionSide() != BntSuspension.WIDE_BOGIE) {
            return;
        }
        Axis axis = BntSuspension.axis(be.getBlockState());
        boolean wide = access.bnt$getSuspensionSide() == BntSuspension.WIDE_BOGIE;
        if (wide && !BntSuspension.armEnd(be.getLevel(), be.getBlockPos(), axis, access.bnt$getSuspensionFacing()).equals(be.getBlockPos())) {
            return;
        }
        Vec3 away = BntSuspension.across(axis).scale(-1.0);
        Vec3 lead = BntSuspension.displacement(be, HiddenCogwheelCompat.getHeldVisualDrop(be, partialTicks));
        KineticBlockEntity partner = BntSuspension.partner(be.getLevel(), be.getBlockPos(), be);
        Vec3 trailing = partner == null ? Vec3.ZERO
            : BntSuspension.displacement(partner, HiddenCogwheelCompat.getHeldVisualDrop(partner, partialTicks));
        double drop = Math.max(0.0, -access.bnt$getAlignmentOffsetY());
        double spread = away.x * access.bnt$getAlignmentOffsetX() + away.z * access.bnt$getAlignmentOffsetZ();
        BntBogiePose pose = new BntBogiePose(spread + lead.dot(away), lead.y, trailing.dot(away) - spread, trailing.y, drop);
        bogie(be, away, access.bnt$getSuspensionFacing(), wide, Vec3.ZERO, pose, sink);
    }

    static void bogie(KineticBlockEntity be, Vec3 away, int facing, boolean wide, Vec3 shift, BntBogiePose bogie, Sink sink) {
        Vec3 depth = BntSuspension.along(BntSuspension.axis(be.getBlockState())).scale(wide ? facing : -facing);
        Vec3 origin = CENTRE.add(shift).add(0.0, CogwheelSizeHelper.getVisualVerticalOffset(be.getBlockState().getBlock()), 0.0);
        int model = wide ? WIDE_BOGIE : BOGIE;
        for (int part = 0; part < PART_QUADS[model].length; part++) {
            if (PART_QUADS[model][part].length == 0) {
                continue;
            }
            int at = part * 6;
            double a = bogie.affine[at];
            double b = bogie.affine[at + 1];
            double c = bogie.affine[at + 2];
            double d = bogie.affine[at + 3];
            double cos = bogie.turn[part * 2];
            double sin = bogie.turn[part * 2 + 1];
            sink.part(model, part,
                away.scale(a).add(UP.scale(c)),
                away.scale(b).add(UP.scale(d)),
                depth,
                origin.add(away.scale(bogie.affine[at + 4])).add(UP.scale(bogie.affine[at + 5])),
                away.scale(cos).add(UP.scale(sin)),
                away.scale(-sin).add(UP.scale(cos)));
        }
    }

    public static Sink emitter(PoseStack.Pose pose, MultiBufferSource buffer, boolean ghost, int color, int light, int overlay) {
        return (model, part, x, y, z, origin, normalX, normalY) -> {
            ResourceLocation texture = texture(model);
            VertexConsumer consumer = buffer.getBuffer(ghost ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture));
            boolean reverse = normalX.dot(normalY.cross(z)) < 0.0;
            float[] quads = QUADS[model];
            Vec3[] corners = new Vec3[4];
            for (int quad : PART_QUADS[model][part]) {
                for (int k = 0; k < 4; k++) {
                    int at = quad + k * 5;
                    corners[k] = origin.add(x.scale(quads[at])).add(y.scale(quads[at + 1])).add(z.scale(quads[at + 2]));
                }
                Vec3 normal = normalX.scale(quads[quad + 20]).add(normalY.scale(quads[quad + 21])).add(z.scale(quads[quad + 22]));
                for (int i = 0; i < 4; i++) {
                    int k = reverse ? 3 - i : i;
                    int at = quad + k * 5;
                    consumer.addVertex(pose, (float)corners[k].x, (float)corners[k].y, (float)corners[k].z)
                        .setColor(color)
                        .setUv(quads[at + 3], quads[at + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
                }
            }
        };
    }

    private static int[][] split(float[] quads) {
        int parts = 0;
        for (int quad = 0; quad < quads.length; quad += 24) {
            parts = Math.max(parts, (int)quads[quad + 23] + 1);
        }
        List<List<Integer>> byPart = new ArrayList<>();
        for (int part = 0; part < parts; part++) {
            byPart.add(new ArrayList<>());
        }
        for (int quad = 0; quad < quads.length; quad += 24) {
            byPart.get((int)quads[quad + 23]).add(quad);
        }
        int[][] split = new int[parts][];
        for (int part = 0; part < parts; part++) {
            split[part] = byPart.get(part).stream().mapToInt(Integer::intValue).toArray();
        }
        return split;
    }
}
