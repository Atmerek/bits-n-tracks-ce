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
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntBogieRenderer {
    public static final ResourceLocation TEXTURE = BitsNTracks.asResource("textures/block/bogie_suspension.png");
    private static final int SOLID = 0xFFFFFFFF;
    private static final int GHOST = 0x9066FF66;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    private BntBogieRenderer() {
    }

    public static void renderAttached(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access) || access.bnt$getSuspensionSide() != BntSuspension.BOGIE || be.getLevel() == null) {
            return;
        }
        Axis axis = BntSuspension.axis(be.getBlockState());
        Vec3 away = BntSuspension.across(axis).scale(-1.0);
        Vec3 lead = BntSuspension.displacement(be, HiddenCogwheelCompat.getHeldVisualDrop(be, partialTicks));
        KineticBlockEntity partner = BntSuspension.partner(be.getLevel(), be.getBlockPos(), be);
        Vec3 trailing = partner == null ? Vec3.ZERO
            : BntSuspension.displacement(partner, HiddenCogwheelCompat.getHeldVisualDrop(partner, partialTicks));
        double drop = Math.max(0.0, -access.bnt$getAlignmentOffsetY());
        double spread = away.x * access.bnt$getAlignmentOffsetX() + away.z * access.bnt$getAlignmentOffsetZ();
        BntBogiePose pose = new BntBogiePose(spread + lead.dot(away), lead.y, trailing.dot(away) - spread, trailing.y, drop);
        draw(ms.last(), buffer.getBuffer(RenderType.entityCutout(TEXTURE)), be, away, access.bnt$getSuspensionFacing(), pose, SOLID, light, overlay);
    }

    public static void renderGhost(KineticBlockEntity be, int facing, double drop, PoseStack ms, MultiBufferSource buffer) {
        if (!BntSuspension.supportsBogie(be.getBlockState())) {
            return;
        }
        Vec3 away = BntSuspension.across(BntSuspension.axis(be.getBlockState())).scale(-1.0);
        draw(ms.last(), buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), be, away, facing,
            new BntBogiePose(0.0, 0.0, 0.0, 0.0, drop), GHOST, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private static void draw(
        PoseStack.Pose pose, VertexConsumer consumer, KineticBlockEntity be, Vec3 away, int facing, BntBogiePose bogie,
        int color, int light, int overlay
    ) {
        Vec3 depth = BntSuspension.along(BntSuspension.axis(be.getBlockState())).scale(-facing);
        Vec3 origin = CENTRE.add(0.0, CogwheelSizeHelper.getVisualVerticalOffset(be.getBlockState().getBlock()), 0.0);
        boolean reverse = away.dot(UP.cross(depth)) < 0.0;
        float[] quads = BntBogieModel.QUADS;
        Vec3[] corners = new Vec3[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        for (int quad = 0; quad < quads.length; quad += 24) {
            int part = (int)quads[quad + 23];
            int at = part * 6;
            double a = bogie.affine[at];
            double b = bogie.affine[at + 1];
            double c = bogie.affine[at + 2];
            double d = bogie.affine[at + 3];
            double tx = bogie.affine[at + 4];
            double ty = bogie.affine[at + 5];
            for (int k = 0; k < 4; k++) {
                int vertex = quad + k * 5;
                double x = quads[vertex];
                double y = quads[vertex + 1];
                corners[k] = origin.add(away.scale(a * x + b * y + tx)).add(UP.scale(c * x + d * y + ty)).add(depth.scale(quads[vertex + 2]));
                us[k] = quads[vertex + 3];
                vs[k] = quads[vertex + 4];
            }
            double cos = bogie.turn[part * 2];
            double sin = bogie.turn[part * 2 + 1];
            double nx = quads[quad + 20];
            double ny = quads[quad + 21];
            Vec3 normal = away.scale(cos * nx - sin * ny).add(UP.scale(sin * nx + cos * ny)).add(depth.scale(quads[quad + 22]));
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
}
