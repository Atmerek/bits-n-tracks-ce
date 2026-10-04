package dev.qwxon.bitsntracks.content.suspension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.BitsNTracks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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

    private BntBogieRenderer() {
    }

    public static void renderAttached(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BntSuspensionParts.bogie(be, partialTicks, BntSuspensionParts.emitter(ms.last(), buffer, false, SOLID, light, overlay));
    }

    public static void renderGhost(KineticBlockEntity be, int facing, double drop, PoseStack ms, MultiBufferSource buffer) {
        boolean wide = BntSuspension.supportsWideBogie(be.getBlockState());
        if (!wide && !BntSuspension.supportsBogie(be.getBlockState()) || be.getLevel() == null) {
            return;
        }
        Axis axis = BntSuspension.axis(be.getBlockState());
        Vec3 away = BntSuspension.across(axis).scale(-1.0);
        Vec3 shift = Vec3.atLowerCornerOf(BntSuspension.armEnd(be.getLevel(), be.getBlockPos(), axis, facing).subtract(be.getBlockPos()));
        BntSuspensionParts.bogie(be, away, facing, wide, wide ? shift : Vec3.ZERO, new BntBogiePose(0.0, 0.0, 0.0, 0.0, drop),
            BntSuspensionParts.emitter(ms.last(), buffer, true, GHOST, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
    }
}
