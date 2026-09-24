package dev.qwxon.bitsntracks.content.suspension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntSuspensionPieceRenderer {
    public static final ResourceLocation TEXTURE = BitsNTracks.asResource("textures/block/suspension_piece.png");
    private static final int SOLID = 0xFFFFFFFF;
    private static final int GHOST = 0x9066FF66;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);
    private static final float ITEM_SCALE = 0.55F;

    private BntSuspensionPieceRenderer() {
    }

    public static void renderAttached(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || !BntSuspension.hasPiece(be)) {
            return;
        }
        if (BntSuspension.isBogie(be)) {
            BntBogieRenderer.renderAttached(be, partialTicks, ms, buffer, light, overlay);
            return;
        }
        Vec3 cog = CENTRE.add(HiddenCogwheelCompat.getModelTranslation(be, partialTicks));
        draw(ms.last(), buffer.getBuffer(RenderType.entityCutout(TEXTURE)), be, cog,
            access.bnt$getSuspensionSide(), access.bnt$getSuspensionFacing(), SOLID, light, overlay);
    }

    public static void renderGhost(KineticBlockEntity be, int side, int facing, float partialTicks, PoseStack ms, MultiBufferSource buffer) {
        if (!BntSuspension.supports(be.getBlockState())) {
            return;
        }
        Vec3 cog = CENTRE.add(HiddenCogwheelCompat.getModelTranslation(be, partialTicks));
        draw(ms.last(), buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), be, cog, side, facing, GHOST,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    public static void renderItem(PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        ms.translate(-0.625, -0.625, 0.0);
        draw(ms.last(), buffer.getBuffer(RenderType.entityCutout(TEXTURE)), Vec3.ZERO, new Vec3(1.0, 1.0, 0.0),
            new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1, 1, true, SOLID, light, overlay);
        ms.popPose();
    }

    private static void draw(
        PoseStack.Pose pose, VertexConsumer consumer, KineticBlockEntity be, Vec3 cog, int side, int facing, int color, int light, int overlay
    ) {
        var axis = BntSuspension.axis(be.getBlockState());
        Vec3 across = BntSuspension.across(axis);
        double seat = CogwheelSizeHelper.getVisualVerticalOffset(be.getBlockState().getBlock());
        Vec3 pivot = CENTRE.add(0.0, seat + 1.0, 0.0).add(across.scale(side));
        boolean holder = side > 0 || be.getLevel() == null || !BntSuspension.sharesPivot(be.getLevel(), be.getBlockPos(), axis, side);
        draw(pose, consumer, cog, pivot, across, BntSuspension.along(axis), side, facing, holder, color, light, overlay);
    }

    private static void draw(
        PoseStack.Pose pose, VertexConsumer consumer, Vec3 cog, Vec3 pivot, Vec3 across, Vec3 along,
        int side, int facing, boolean holder, int color, int light, int overlay
    ) {
        Vec3 depth = along.scale(facing);
        Vec3 sideways = across.scale(side);
        boolean reverse = side * facing < 0;

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
        double shaftSpan = BntSuspensionPieceModel.SHAFT_END - BntSuspensionPieceModel.SHAFT_START;

        float[] arm = BntSuspensionPieceModel.ARM;
        Vec3[] corners = new Vec3[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        for (int quad = 0; quad < arm.length; quad += 24) {
            int part = (int)arm[quad + 23];
            if (part == 2 && !holder) {
                continue;
            }
            for (int k = 0; k < 4; k++) {
                int at = quad + k * 5;
                double a = arm[at];
                double shift = 0.0;
                if (part == 2) {
                    a += stretch;
                    shift = 1.0;
                } else if (part == 1) {
                    double t = Math.max(0.0, Math.min(1.0, (a - BntSuspensionPieceModel.SHAFT_START) / shaftSpan));
                    a = BntSuspensionPieceModel.SHAFT_START + t * (shaftSpan + stretch);
                    shift = t;
                }
                corners[k] = cog.add(armward.scale(a)).add(normalward.scale(arm[at + 1])).add(depth.scale(arm[at + 2])).add(offAxis.scale(shift));
                us[k] = arm[at + 3];
                vs[k] = arm[at + 4];
            }
            Vec3 normal = armward.scale(arm[quad + 20]).add(normalward.scale(arm[quad + 21])).add(depth.scale(arm[quad + 22]));
            quad(pose, consumer, corners, us, vs, normal, reverse, color, light, overlay);
        }

        float[] housing = BntSuspensionPieceModel.HOUSING;
        for (int quad = 0; holder && quad < housing.length; quad += 23) {
            for (int k = 0; k < 4; k++) {
                int at = quad + k * 5;
                corners[k] = pivot.add(sideways.scale(housing[at])).add(UP.scale(housing[at + 1])).add(depth.scale(housing[at + 2]));
                us[k] = housing[at + 3];
                vs[k] = housing[at + 4];
            }
            Vec3 normal = sideways.scale(housing[quad + 20]).add(UP.scale(housing[quad + 21])).add(depth.scale(housing[quad + 22]));
            quad(pose, consumer, corners, us, vs, normal, reverse, color, light, overlay);
        }
    }

    private static void quad(
        PoseStack.Pose pose, VertexConsumer consumer, Vec3[] corners, float[] us, float[] vs, Vec3 normal,
        boolean reverse, int color, int light, int overlay
    ) {
        for (int i = 0; i < 4; i++) {
            int k = reverse ? 3 - i : i;
            consumer.addVertex(pose, (float)corners[k].x, (float)corners[k].y, (float)corners[k].z)
                .setColor(color)
                .setUv(us[k], vs[k])
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
        }
    }
}
