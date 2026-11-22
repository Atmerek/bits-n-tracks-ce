package dev.qwxon.bitsntracks.physics;

import com.google.common.collect.Iterators;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheelCollision {
    private static final int REACH = 2;
    private static final Map<Level, Index> HIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    private BntCogwheelCollision() {
    }

    private static final class Index {
        private final Long2ObjectOpenHashMap<Set<KineticBlockEntity>> sections = new Long2ObjectOpenHashMap<>();

        private synchronized void add(KineticBlockEntity be) {
            BlockPos pos = be.getBlockPos();
            long key = SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ()));
            this.sections.computeIfAbsent(key, ignored -> Collections.newSetFromMap(new WeakHashMap<>())).add(be);
        }

        private synchronized List<KineticBlockEntity> near(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            List<KineticBlockEntity> found = null;
            int fromX = SectionPos.blockToSectionCoord(minX);
            int fromY = SectionPos.blockToSectionCoord(minY);
            int fromZ = SectionPos.blockToSectionCoord(minZ);
            int toX = SectionPos.blockToSectionCoord(maxX);
            int toY = SectionPos.blockToSectionCoord(maxY);
            int toZ = SectionPos.blockToSectionCoord(maxZ);
            for (int x = fromX; x <= toX; x++) {
                for (int y = fromY; y <= toY; y++) {
                    for (int z = fromZ; z <= toZ; z++) {
                        long key = SectionPos.asLong(x, y, z);
                        Set<KineticBlockEntity> section = this.sections.get(key);
                        if (section == null) {
                            continue;
                        }
                        for (Iterator<KineticBlockEntity> iterator = section.iterator(); iterator.hasNext(); ) {
                            KineticBlockEntity be = iterator.next();
                            if (be.isRemoved() || !(be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock)) {
                                iterator.remove();
                                continue;
                            }
                            if (found == null) {
                                found = new ArrayList<>();
                            }
                            found.add(be);
                        }
                        if (section.isEmpty()) {
                            this.sections.remove(key);
                        }
                    }
                }
            }
            return found == null ? List.of() : found;
        }
    }

    public static void track(KineticBlockEntity be) {
        Level level = be.getLevel();
        if (level != null) {
            index(level).add(be);
        }
    }

    private static Index index(Level level) {
        return HIDDEN.computeIfAbsent(level, key -> new Index());
    }

    public static Iterable<BlockPos> withDisplaced(Level level, BlockPos from, BlockPos to, Iterable<BlockPos> cells) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        List<BlockPos> extra = null;
        for (KineticBlockEntity be : index(level).near(minX - REACH, minY - REACH, minZ - REACH, maxX + REACH, maxY + REACH, maxZ + REACH)) {
            BlockPos pos = be.getBlockPos();
            if (pos.getX() < minX - REACH || pos.getX() > maxX + REACH
                || pos.getY() < minY - REACH || pos.getY() > maxY + REACH
                || pos.getZ() < minZ - REACH || pos.getZ() > maxZ + REACH
                || pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ
                || !BntFlangedCogwheelBlock.physics(be.getBlockState())) {
                continue;
            }
            if (extra == null) {
                extra = new ArrayList<>();
            }
            extra.add(pos);
        }
        if (extra == null) {
            return cells;
        }
        List<BlockPos> added = extra;
        return () -> Iterators.concat(cells.iterator(), added.iterator());
    }

    public static List<VoxelShape> withDisplacedShapes(Level level, AABB box, List<VoxelShape> shapes) {
        List<VoxelShape> combined = null;
        int minX = (int)Math.floor(box.minX) - REACH - 1;
        int minY = (int)Math.floor(box.minY) - REACH - 1;
        int minZ = (int)Math.floor(box.minZ) - REACH - 1;
        int maxX = (int)Math.floor(box.maxX) + REACH;
        int maxY = (int)Math.floor(box.maxY) + REACH;
        int maxZ = (int)Math.floor(box.maxZ) + REACH;
        for (KineticBlockEntity be : index(level).near(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockPos pos = be.getBlockPos();
            if (pos.getX() + 1 + REACH < box.minX || pos.getX() - REACH > box.maxX
                || pos.getY() + 1 + REACH < box.minY || pos.getY() - REACH > box.maxY
                || pos.getZ() + 1 + REACH < box.minZ || pos.getZ() - REACH > box.maxZ
                || !BntFlangedCogwheelBlock.physics(be.getBlockState())
                || !(be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock cogwheel)) {
                continue;
            }
            VoxelShape shape = cogwheel.entityCollisionShape(be.getBlockState(), level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            VoxelShape placed = shape.move(pos.getX(), pos.getY(), pos.getZ());
            if (!placed.bounds().intersects(box)) {
                continue;
            }
            if (combined == null) {
                combined = new ArrayList<>(shapes);
            }
            combined.add(placed);
        }
        return combined == null ? shapes : combined;
    }
}
