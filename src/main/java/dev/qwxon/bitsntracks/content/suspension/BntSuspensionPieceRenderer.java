package dev.qwxon.bitsntracks.content.suspension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    private BntSuspensionPieceRenderer() {
    }

    public static void renderAttached(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BntSuspensionParts.attached(be, partialTicks, BntSuspensionParts.emitter(ms.last(), buffer, false, SOLID, light, overlay));
    }

    public static void renderGhost(KineticBlockEntity be, int side, int facing, float partialTicks, PoseStack ms, MultiBufferSource buffer) {
        if (!BntSuspension.supports(be.getBlockState())) {
            return;
        }
        Vec3 cog = CENTRE.add(HiddenCogwheelCompat.getModelTranslation(be, partialTicks));
        BntSuspensionParts.arm(be, cog, side, facing,
            BntSuspensionParts.emitter(ms.last(), buffer, true, GHOST, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
    }
}
