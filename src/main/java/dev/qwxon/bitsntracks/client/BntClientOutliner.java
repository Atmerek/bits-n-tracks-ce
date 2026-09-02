package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.BntLeverZone;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BntClientOutliner {
    public static void showFaceHighlight(BlockPos pos, Direction face, AABB regionAABB) {
        Outliner.getInstance().showAABB("alignment_tool_face", regionAABB).highlightFace(face).colored(-16711936).lineWidth(0.01F);
    }

    public static AABB getHighlightAABB(BlockPos pos, Direction face, double dx, double dy, double dz, Axis blockAxis, double radius) {
        double minX = 0.5 - radius;
        double maxX = 0.5 + radius;
        double minY = 0.5 - radius;
        double maxY = 0.5 + radius;
        double minZ = 0.5 - radius;
        double maxZ = 0.5 + radius;
        if (face == Direction.NORTH) {
            minZ = -0.002;
            maxZ = 0.002;
        } else if (face == Direction.SOUTH) {
            minZ = 0.998;
            maxZ = 1.002;
        } else if (face == Direction.WEST) {
            minX = -0.002;
            maxX = 0.002;
        } else if (face == Direction.EAST) {
            minX = 0.998;
            maxX = 1.002;
        } else if (face == Direction.DOWN) {
            minY = -0.002;
            maxY = 0.002;
        } else if (face == Direction.UP) {
            minY = 0.998;
            maxY = 1.002;
        }

        double centerHalf = 0.5 * radius;
        Direction zone = BntLeverZone.of(blockAxis, dx, dy, dz, radius);
        if (zone == null) {
            if (blockAxis != Axis.X) {
                minX = 0.5 - centerHalf;
                maxX = 0.5 + centerHalf;
            }
            if (blockAxis != Axis.Y) {
                minY = 0.5 - centerHalf;
                maxY = 0.5 + centerHalf;
            }
            if (blockAxis != Axis.Z) {
                minZ = 0.5 - centerHalf;
                maxZ = 0.5 + centerHalf;
            }
        } else {
            switch (zone) {
                case EAST -> {
                    minX = 0.5 + centerHalf;
                    maxX = 0.5 + radius;
                }
                case WEST -> {
                    minX = 0.5 - radius;
                    maxX = 0.5 - centerHalf;
                }
                case UP -> {
                    minY = 0.5 + centerHalf;
                    maxY = 0.5 + radius;
                }
                case DOWN -> {
                    minY = 0.5 - radius;
                    maxY = 0.5 - centerHalf;
                }
                case SOUTH -> {
                    minZ = 0.5 + centerHalf;
                    maxZ = 0.5 + radius;
                }
                case NORTH -> {
                    minZ = 0.5 - radius;
                    maxZ = 0.5 - centerHalf;
                }
            }
        }

        return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
    }

    public static AABB getSideDepthHighlightAABB(BlockPos pos, Direction face, double dx, double dy, double dz, Axis blockAxis, double radius) {
        double minX = 0.5 - radius;
        double maxX = 0.5 + radius;
        double minY = 0.5 - radius;
        double maxY = 0.5 + radius;
        double minZ = 0.5 - radius;
        double maxZ = 0.5 + radius;
        if (face == Direction.NORTH) {
            minZ = -0.002;
            maxZ = 0.002;
        } else if (face == Direction.SOUTH) {
            minZ = 0.998;
            maxZ = 1.002;
        } else if (face == Direction.WEST) {
            minX = -0.002;
            maxX = 0.002;
        } else if (face == Direction.EAST) {
            minX = 0.998;
            maxX = 1.002;
        } else if (face == Direction.DOWN) {
            minY = -0.002;
            maxY = 0.002;
        } else if (face == Direction.UP) {
            minY = 0.998;
            maxY = 1.002;
        }

        double centerHalf = 0.08;
        if (blockAxis == Axis.X) {
            if (dx > 0.0) {
                minX = 0.58;
                maxX = 0.5 + radius;
            } else {
                minX = 0.5 - radius;
                maxX = 0.42;
            }
        } else if (blockAxis == Axis.Y) {
            if (dy > 0.0) {
                minY = 0.58;
                maxY = 0.5 + radius;
            } else {
                minY = 0.5 - radius;
                maxY = 0.42;
            }
        } else if (dz > 0.0) {
            minZ = 0.58;
            maxZ = 0.5 + radius;
        } else {
            minZ = 0.5 - radius;
            maxZ = 0.42;
        }

        return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ, pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
    }
}
