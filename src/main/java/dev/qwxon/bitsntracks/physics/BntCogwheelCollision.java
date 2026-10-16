package dev.qwxon.bitsntracks.physics;

import com.google.common.collect.Iterators;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BntCogwheelCollision {
    private static final int REACH = 2;
    private static final Map<Level, Set<KineticBlockEntity>> HIDDEN = Collections.synchronizedMap(new WeakHashMap<>());

    private BntCogwheelCollision() {
    }

    public static void track(KineticBlockEntity be) {
        Level level = be.getLevel();
        if (level != null) {
            hidden(level).add(be);
        }
    }

    private static Set<KineticBlockEntity> hidden(Level level) {
        return HIDDEN.computeIfAbsent(level, key -> Collections.newSetFromMap(new WeakHashMap<>()));
    }

    public static Iterable<BlockPos> withDisplaced(Level level, BlockPos from, BlockPos to, Iterable<BlockPos> cells) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        List<BlockPos> extra = null;
        for (Iterator<KineticBlockEntity> iterator = hidden(level).iterator(); iterator.hasNext(); ) {
            KineticBlockEntity be = iterator.next();
            if (be.isRemoved() || !(be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock)) {
                iterator.remove();
                continue;
            }
            BlockPos pos = be.getBlockPos();
            if (!BntFlangedCogwheelBlock.physics(be.getBlockState())
                || pos.getX() < minX - REACH || pos.getX() > maxX + REACH
                || pos.getY() < minY - REACH || pos.getY() > maxY + REACH
                || pos.getZ() < minZ - REACH || pos.getZ() > maxZ + REACH
                || pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
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
        for (Iterator<KineticBlockEntity> iterator = hidden(level).iterator(); iterator.hasNext(); ) {
            KineticBlockEntity be = iterator.next();
            if (be.isRemoved() || !(be.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock cogwheel)) {
                iterator.remove();
                continue;
            }
            BlockPos pos = be.getBlockPos();
            if (!BntFlangedCogwheelBlock.physics(be.getBlockState())
                || pos.getX() + 1 + REACH < box.minX || pos.getX() - REACH > box.maxX
                || pos.getY() + 1 + REACH < box.minY || pos.getY() - REACH > box.maxY
                || pos.getZ() + 1 + REACH < box.minZ || pos.getZ() - REACH > box.maxZ) {
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
