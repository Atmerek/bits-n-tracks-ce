package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntCogwheelPicker {
    private static final double SEARCH_MARGIN = 2.0;

    private BntCogwheelPicker() {
    }

    public static void correctPick(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        if (level == null || camera == null || minecraft.player == null) {
            return;
        }

        double reach = minecraft.player.blockInteractionRange();
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(reach));

        Search search = new Search(level, eye, partialTick);
        HitResult previous = minecraft.hitResult;
        search.best = previous != null && previous.getType() != HitResult.Type.MISS
            ? distanceSqr(level, eye, previous.getLocation())
            : reach * reach;
        search.scan(eye, end);

        BoundingBox3d bounds = new BoundingBox3d(
            Math.min(eye.x, end.x),
            Math.min(eye.y, end.y),
            Math.min(eye.z, end.z),
            Math.max(eye.x, end.x),
            Math.max(eye.y, end.y),
            Math.max(eye.z, end.z)
        )
            .expand(SEARCH_MARGIN);

        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, bounds)) {
            if (subLevel instanceof ClientSubLevel clientSubLevel) {
                Pose3dc pose = clientSubLevel.renderPose(partialTick);
                search.scan(pose.transformPositionInverse(eye), pose.transformPositionInverse(end));
            }
        }

        if (search.hit != null) {
            minecraft.hitResult = search.hit;
            minecraft.crosshairPickEntity = null;
        }
    }

    private static double distanceSqr(Level level, Vec3 eye, Vec3 target) {
        return Sable.HELPER.distanceSquaredWithSubLevels(level, eye, target);
    }

    private static final class Search {
        private final Level level;
        private final Vec3 eye;
        private final float partialTick;
        private double best;
        private BlockHitResult hit;

        private Search(Level level, Vec3 eye, float partialTick) {
            this.level = level;
            this.eye = eye;
            this.partialTick = partialTick;
        }

        private void scan(Vec3 from, Vec3 to) {
            AABB box = new AABB(from, to).inflate(SEARCH_MARGIN);
            int minX = Mth.floor(box.minX);
            int minY = Mth.floor(box.minY);
            int minZ = Mth.floor(box.minZ);
            int maxX = Mth.floor(box.maxX);
            int maxY = Mth.floor(box.maxY);
            int maxZ = Mth.floor(box.maxZ);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                    ChunkAccess chunk = this.level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }

                    for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                        int index = chunk.getSectionIndexFromSectionY(sectionY);
                        if (index < 0 || index >= chunk.getSectionsCount()) {
                            continue;
                        }
                        LevelChunkSection section = chunk.getSection(index);
                        if (section.hasOnlyAir() || !section.maybeHas(HiddenCogwheelCompat::isFlangedCogwheelBlock)) {
                            continue;
                        }

                        for (int x = Math.max(minX, chunkX << 4); x <= Math.min(maxX, (chunkX << 4) + 15); x++) {
                            for (int y = Math.max(minY, sectionY << 4); y <= Math.min(maxY, (sectionY << 4) + 15); y++) {
                                for (int z = Math.max(minZ, chunkZ << 4); z <= Math.min(maxZ, (chunkZ << 4) + 15); z++) {
                                    BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                    if (HiddenCogwheelCompat.isFlangedCogwheelBlock(state)) {
                                        this.consider(pos.set(x, y, z), state, from, to);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void consider(BlockPos pos, BlockState state, Vec3 from, Vec3 to) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (HiddenCogwheelCompat.getModelTranslation(be, this.partialTick).equals(Vec3.ZERO)) {
                return;
            }

            VoxelShape shape = state.getShape(this.level, pos);
            BlockHitResult candidate = shape.clip(from, to, pos);
            if (candidate == null) {
                return;
            }

            double distance = distanceSqr(this.level, this.eye, candidate.getLocation());
            if (distance < this.best) {
                this.best = distance;
                this.hit = new BlockHitResult(
                    candidate.getLocation(), candidate.getDirection(), pos.immutable(), candidate.isInside()
                );
            }
        }
    }
}
