package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionMeshes;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionParts;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntPiecePicker {
    private static final double SEARCH_MARGIN = 2.5;
    private static final double EPSILON = 1.0E-9;
    private static final int ARM_ROD = 1;
    private static final int ARM_PIVOT = 2;
    private static final int BOGIE_DROP = 1;

    private BntPiecePicker() {
    }

    @Nullable
    public static BlockPos aimed(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null || minecraft.player == null) {
            return null;
        }
        float partialTick = BntClientCompat.getPartialTick();
        double reach = minecraft.player.blockInteractionRange();
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(reach));
        HitResult previous = minecraft.hitResult;
        double limit = previous != null && previous.getType() != HitResult.Type.MISS
            ? Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, eye, previous.getLocation())) / reach
            : 1.0;

        Search search = new Search(level, partialTick, Math.min(1.0, limit + EPSILON));
        search.scan(eye, end);
        BoundingBox3d bounds = new BoundingBox3d(
            Math.min(eye.x, end.x), Math.min(eye.y, end.y), Math.min(eye.z, end.z),
            Math.max(eye.x, end.x), Math.max(eye.y, end.y), Math.max(eye.z, end.z))
            .expand(SEARCH_MARGIN);
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, bounds)) {
            if (subLevel instanceof ClientSubLevel clientSubLevel) {
                Pose3dc pose = clientSubLevel.renderPose(partialTick);
                search.scan(pose.transformPositionInverse(eye), pose.transformPositionInverse(end));
            }
        }
        return search.hit;
    }

    @Nullable
    public static AABB bounds(ClientLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) || !BntSuspension.hasPiece(kinetic)) {
            return null;
        }
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Vec3[] quad : mount(kinetic, BntClientCompat.getPartialTick())) {
            for (Vec3 corner : quad) {
                box[0] = Math.min(box[0], corner.x);
                box[1] = Math.min(box[1], corner.y);
                box[2] = Math.min(box[2], corner.z);
                box[3] = Math.max(box[3], corner.x);
                box[4] = Math.max(box[4], corner.y);
                box[5] = Math.max(box[5], corner.z);
            }
        }
        if (box[0] > box[3]) {
            return null;
        }
        return new AABB(box[0], box[1], box[2], box[3], box[4], box[5]).move(pos);
    }

    private static List<Vec3[]> mount(KineticBlockEntity kinetic, float partialTick) {
        List<Vec3[]> hinge = new ArrayList<>();
        List<Vec3[]> rod = new ArrayList<>();
        BntSuspensionParts.attached(kinetic, partialTick, (model, part, x, y, z, origin, normalX, normalY) -> {
            List<Vec3[]> into = model != BntSuspensionParts.ARM ? part <= BOGIE_DROP ? hinge : null
                : part == ARM_PIVOT ? hinge : part == ARM_ROD ? rod : null;
            if (into == null) {
                return;
            }
            BntSuspensionMeshes.Mesh mesh = BntSuspensionMeshes.mesh(model);
            float[] quads = mesh.quads();
            for (int quad : mesh.part(part)) {
                into.add(new Vec3[]{
                    corner(quads, quad, x, y, z, origin),
                    corner(quads, quad + 5, x, y, z, origin),
                    corner(quads, quad + 10, x, y, z, origin),
                    corner(quads, quad + 15, x, y, z, origin)});
            }
        });
        return hinge.isEmpty() ? rod : hinge;
    }

    private static Vec3 corner(float[] quads, int at, Vec3 x, Vec3 y, Vec3 z, Vec3 origin) {
        return origin.add(x.scale(quads[at])).add(y.scale(quads[at + 1])).add(z.scale(quads[at + 2]));
    }

    private static double triangle(Vec3 from, Vec3 ray, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 edge1 = b.subtract(a);
        Vec3 edge2 = c.subtract(a);
        Vec3 p = ray.cross(edge2);
        double det = edge1.dot(p);
        if (Math.abs(det) < EPSILON) {
            return Double.MAX_VALUE;
        }
        double inverse = 1.0 / det;
        Vec3 t = from.subtract(a);
        double u = t.dot(p) * inverse;
        if (u < 0.0 || u > 1.0) {
            return Double.MAX_VALUE;
        }
        Vec3 q = t.cross(edge1);
        double v = ray.dot(q) * inverse;
        if (v < 0.0 || u + v > 1.0) {
            return Double.MAX_VALUE;
        }
        double along = edge2.dot(q) * inverse;
        return along >= 0.0 ? along : Double.MAX_VALUE;
    }

    private static final class Search {
        private final ClientLevel level;
        private final float partialTick;
        private double best;
        private BlockPos hit;

        private Search(ClientLevel level, float partialTick, double best) {
            this.level = level;
            this.partialTick = partialTick;
            this.best = best;
        }

        private void scan(Vec3 from, Vec3 to) {
            AABB box = new AABB(from, to).inflate(SEARCH_MARGIN);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
                for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                    for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                        pos.set(x, y, z);
                        if (BntCogwheels.isOwnCogwheel(this.level.getBlockState(pos))) {
                            this.consider(pos, from, to);
                        }
                    }
                }
            }
        }

        private void consider(BlockPos pos, Vec3 from, Vec3 to) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (!(be instanceof KineticBlockEntity kinetic) || !BntSuspension.hasPiece(be)) {
                return;
            }
            Vec3 local = from.subtract(Vec3.atLowerCornerOf(pos));
            Vec3 ray = to.subtract(from);
            for (Vec3[] quad : mount(kinetic, this.partialTick)) {
                double along = Math.min(triangle(local, ray, quad[0], quad[1], quad[2]), triangle(local, ray, quad[0], quad[2], quad[3]));
                if (along < this.best) {
                    this.best = along;
                    this.hit = pos.immutable();
                }
            }
        }
    }
}
