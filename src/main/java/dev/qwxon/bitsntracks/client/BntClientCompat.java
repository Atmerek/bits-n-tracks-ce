package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.RenderedChainPathNode;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class BntClientCompat {
    public static double getVisualDrop(BlockEntity be, float partialTick) {
        return HiddenCogwheelCompat.getVisualDrop(be, partialTick);
    }

    public static double getVisualVerticalTranslation(BlockEntity be, float partialTick) {
        return HiddenCogwheelCompat.getVisualVerticalTranslation(be, partialTick);
    }

    public static float getPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static Vec3 getTransformedPosition(BlockEntity controllerBe, Vec3 localPos, BlockPos relativePos) {
        Level level = HiddenCogwheelCompat.getActualLevel(controllerBe);
        if (level == null) {
            return localPos;
        }
        BlockPos nodePos = controllerBe.getBlockPos().offset(relativePos);
        return localPos
            .add(BntCogwheelPairing.seamOffset(level.getBlockState(nodePos)))
            .add(getNodeDisplacement(controllerBe, relativePos));
    }

    public static Vec3 getNodeDisplacement(BlockEntity controllerBe, BlockPos relativePos) {
        Level level = HiddenCogwheelCompat.getActualLevel(controllerBe);
        if (level == null) {
            return Vec3.ZERO;
        }
        BlockEntity nodeBe = level.getBlockEntity(controllerBe.getBlockPos().offset(relativePos));
        if (nodeBe == null) {
            return Vec3.ZERO;
        }

        Vec3 displacement = Vec3.ZERO;
        if (nodeBe instanceof KineticBlockEntityPhysicsAccess access) {
            displacement = new Vec3(
                access.bnt$getAlignmentOffsetX(), access.bnt$getAlignmentOffsetY(), access.bnt$getAlignmentOffsetZ()
            );
        }

        if (level.isClientSide && HiddenCogwheelCompat.isPhysicsEnabled(nodeBe)) {
            double manualOffset = HiddenCogwheelCompat.getManualVisualVerticalOffset(nodeBe);
            if (manualOffset != 0.0) {
                displacement = displacement.add(0.0, manualOffset, 0.0);
            }

            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            displacement = displacement.add(0.0, -getVisualDrop(nodeBe, partialTick), 0.0);
        }

        return displacement;
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
