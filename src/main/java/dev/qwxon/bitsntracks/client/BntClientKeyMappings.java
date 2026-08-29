package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class BntClientKeyMappings {
    private BntClientKeyMappings() {
    }

    public static void onClientTick(Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            if (minecraft.level != null) {
                ItemStack mainHand = minecraft.player.getMainHandItem();
                ItemStack offHand = minecraft.player.getOffhandItem();
                if ((mainHand.getItem() instanceof CogAlignmentLeverItem || offHand.getItem() instanceof CogAlignmentLeverItem)
                    && minecraft.hitResult instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = minecraft.level.getBlockState(pos);
                    if (state.hasProperty(BlockStateProperties.AXIS)) {
                        BlockEntity be = minecraft.level.getBlockEntity(pos);
                        if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
                            Direction face = blockHit.getDirection();
                            Axis blockAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
                            Vec3 hitVec = blockHit.getLocation();
                            double localX = hitVec.x - pos.getX();
                            double localY = hitVec.y - pos.getY();
                            double localZ = hitVec.z - pos.getZ();
                            double dx = localX - 0.5;
                            double dy = localY - 0.5;
                            double dz = localZ - 0.5;
                            double radius = CogwheelSizeHelper.getToolHighlightRadius(state.getBlock());
                            AABB regionAABB = face.getAxis() == blockAxis
                                ? BntClientOutliner.getHighlightAABB(pos, face, dx, dy, dz, blockAxis, radius)
                                : BntClientOutliner.getSideDepthHighlightAABB(pos, face, dx, dy, dz, blockAxis, radius);
                            BntClientOutliner.showFaceHighlight(pos, face, regionAABB);
                        }
                    }
                }
            }
        }
    }
}
