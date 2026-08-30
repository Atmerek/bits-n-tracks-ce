package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.physics.BntPhysicsEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BntClientCompat {
    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return be instanceof KineticBlockEntity kinetic && be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isPhysicsEnabled()
            ? BntPhysicsEvents.getClientRenderExtension(kinetic, partialTick)
            : 0.0;
    }

    public static double getVisualVerticalTranslation(BlockEntity be, float partialTick) {
        if (be == null) {
            return 0.0;
        } else {
            double drop = getVisualDrop(be, partialTick);
            return HiddenCogwheelCompat.getManualVisualVerticalOffset(be) - drop;
        }
    }

    public static VoxelShape offsetShapeForSuspension(VoxelShape shape, BlockEntity be) {
        if (!(be instanceof KineticBlockEntityPhysicsAccess access)) {
            return shape;
        }

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        double x = access.bnt$getAlignmentOffsetX();
        double y = getVisualVerticalTranslation(be, partialTick) + access.bnt$getAlignmentOffsetY();
        double z = access.bnt$getAlignmentOffsetZ();
        return x == 0.0 && y == 0.0 && z == 0.0 ? shape : shape.move(x, y, z);
    }

    public static Vec3 getTransformedPosition(BlockEntity controllerBe, Vec3 localPos, BlockPos relativePos) {
        Level level = HiddenCogwheelCompat.getActualLevel(controllerBe);
        if (level == null) {
            return localPos;
        } else {
            BlockPos nodePos = controllerBe.getBlockPos().offset(relativePos);
            Vec3 localPosWithSuspension = localPos;
            BlockEntity nodeBe = level.getBlockEntity(nodePos);
            if (nodeBe != null) {
                if (nodeBe instanceof KineticBlockEntityPhysicsAccess access) {
                    localPosWithSuspension = localPos.add(access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ());
                }

                if (level.isClientSide && HiddenCogwheelCompat.isPhysicsEnabled(nodeBe)) {
                    double manualOffset = HiddenCogwheelCompat.getManualVisualVerticalOffset(nodeBe);
                    if (manualOffset != 0.0) {
                        localPosWithSuspension = localPosWithSuspension.add(0.0, manualOffset, 0.0);
                    }

                    float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                    double drop = getVisualDrop(nodeBe, partialTick);
                    localPosWithSuspension = localPosWithSuspension.add(0.0, -drop, 0.0);
                }
            }

            return localPosWithSuspension;
        }
    }

    public static List<ChainSegment> transformChainSegments(List<ChainSegment> segments, CogwheelChain chain, KineticBlockEntity be) {
        if (chain != null && segments != null && !segments.isEmpty()) {
            List<RenderedChainPathNode> pathNodes = chain.getChainPathNodes();
            int size = pathNodes.size();
            if (size != segments.size()) {
                return segments;
            } else {
                List<ChainSegment> transformed = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
                    ChainSegment segment = segments.get(i);
                    RenderedChainPathNode prevNode = pathNodes.get((i - 1 + size) % size);
                    RenderedChainPathNode currentNode = pathNodes.get(i);
                    RenderedChainPathNode nextNode = pathNodes.get((i + 1) % size);
                    RenderedChainPathNode nextNextNode = pathNodes.get((i + 2) % size);
                    Vec3 tPreFrom = getTransformedPosition(be, segment.preFrom(), nextNextNode.relativePos());
                    Vec3 tFrom = getTransformedPosition(be, segment.from(), nextNode.relativePos());
                    Vec3 tTo = getTransformedPosition(be, segment.to(), currentNode.relativePos());
                    Vec3 tPostTo = getTransformedPosition(be, segment.postTo(), prevNode.relativePos());
                    transformed.add(
                        new ChainSegment(
                            tPreFrom, tFrom, tTo, tPostTo, segment.fromCogwheelAxis(), segment.toCogwheelAxis(), segment.uvStart(), segment.distance()
                        )
                    );
                }

                return transformed;
            }
        } else {
            return segments;
        }
    }
}
