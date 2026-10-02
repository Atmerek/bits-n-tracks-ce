package dev.qwxon.bitsntracks.client.flywheel;

import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.mixin.accessor.LevelRendererAccessor;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

public final class BntFlywheel {
    private static final Set<BntCogwheelVisual> VISUALS = ConcurrentHashMap.newKeySet();
    private static final double MARGIN = 2.0;
    private static boolean warned;

    private BntFlywheel() {
    }

    static void add(BntCogwheelVisual visual) {
        VISUALS.add(visual);
    }

    static void remove(BntCogwheelVisual visual) {
        VISUALS.remove(visual);
    }

    static boolean visible(BlockEntity be, AABB local) {
        Frustum frustum = Minecraft.getInstance().levelRenderer.getFrustum();
        if (frustum == null) {
            return true;
        }
        Vec3 centre = centre(be, local);
        double reach = 0.5 * Math.sqrt(local.getXsize() * local.getXsize() + local.getYsize() * local.getYsize() + local.getZsize() * local.getZsize()) + MARGIN;
        return frustum.isVisible(new AABB(centre.x - reach, centre.y - reach, centre.z - reach, centre.x + reach, centre.y + reach, centre.z + reach));
    }

    static double distanceSqr(BlockEntity be, AABB local) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(centre(be, local));
    }

    static int ticks() {
        return ((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).bnt$getTicks();
    }

    private static Vec3 centre(BlockEntity be, AABB local) {
        Vec3 centre = local.getCenter().add(Vec3.atLowerCornerOf(be.getBlockPos()));
        if (Sable.HELPER.getContainingClient(be) instanceof ClientSubLevel subLevel) {
            centre = subLevel.logicalPose().transformPosition(centre);
        }
        return centre;
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (VISUALS.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }
        float partialTick = BntClientCompat.getPartialTick();
        for (BntCogwheelVisual visual : VISUALS) {
            try {
                visual.frame(partialTick);
            } catch (RuntimeException failure) {
                VISUALS.remove(visual);
                visual.fail();
                if (!warned) {
                    warned = true;
                    BntDebugLog.LOG.error("Flywheel drawing failed, falling back to the block entity renderer for that cogwheel", failure);
                }
            }
        }
    }
}
