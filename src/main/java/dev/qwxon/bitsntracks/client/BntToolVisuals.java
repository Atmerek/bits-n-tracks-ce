package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntToolVisuals {
    public static final int HOVER = 0xA4A9AE;
    public static final int SELECTED = 0xF0F3F6;
    public static final int GOOD = 0x77DD77;
    public static final int WARNING = 0xFF6655;
    public static final float THIN = 1.0F / 64.0F;
    public static final float BOLD = 1.0F / 32.0F;

    private static final double MARKER = 1.5 / 16.0;
    private static final double MARKER_DEPTH = 1.0 / 16.0;

    private BntToolVisuals() {
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static AABB shapeBox(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape(level, pos);
        AABB box = shape.isEmpty() ? new AABB(BlockPos.ZERO) : shape.bounds();
        return box.move(pos);
    }

    public static void hover(Object slot, ClientLevel level, BlockPos pos) {
        cog(slot, level, pos, HOVER, THIN);
    }

    public static void selected(Object slot, ClientLevel level, BlockPos pos) {
        cog(slot, level, pos, SELECTED, BOLD);
    }

    public static void cog(Object slot, ClientLevel level, BlockPos pos, int colour, float width) {
        box(slot, shapeBox(level, pos).inflate(width), colour, width);
    }

    public static void box(Object slot, AABB box, int colour, float width) {
        Outliner.getInstance().chaseAABB(slot, box).colored(colour).lineWidth(width);
    }

    public static AABB marker(Vec3 centre, Axis cogAxis, double halfDepth) {
        double x = cogAxis == Axis.X ? halfDepth + MARKER_DEPTH : MARKER;
        double y = cogAxis == Axis.Y ? halfDepth + MARKER_DEPTH : MARKER;
        double z = cogAxis == Axis.Z ? halfDepth + MARKER_DEPTH : MARKER;
        return new AABB(centre.x - x, centre.y - y, centre.z - z, centre.x + x, centre.y + y, centre.z + z);
    }

    public static Vec3 drawnCentre(ClientLevel level, BlockPos pos) {
        return Vec3.atCenterOf(pos).add(HiddenCogwheelCompat.getModelTranslation(level.getBlockEntity(pos), BntClientCompat.getPartialTick()));
    }
}
