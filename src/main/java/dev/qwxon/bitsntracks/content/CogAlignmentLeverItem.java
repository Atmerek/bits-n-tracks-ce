package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.CogAlignmentLeverItemRenderer;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CogAlignmentLeverItem extends Item {
    public CogAlignmentLeverItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new CogAlignmentLeverItemRenderer()));
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return InteractionResult.PASS;
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KineticBlockEntity && be instanceof KineticBlockEntityPhysicsAccess access) {
                Direction clickedFace = context.getClickedFace();
                Axis blockAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                } else {
                    Player player = context.getPlayer();
                    if (player != null && player.isShiftKeyDown()) {
                        for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
                            BlockEntity nodeBe = level.getBlockEntity(nodePos);
                            if (nodeBe instanceof KineticBlockEntity kinetic && nodeBe instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                                nodeAccess.bnt$setAlignmentOffsetX(0.0F);
                                nodeAccess.bnt$setAlignmentOffsetY(0.0F);
                                nodeAccess.bnt$setAlignmentOffsetZ(0.0F);
                                nodeAccess.bnt$setHiddenByLever(false);
                                kinetic.setChanged();
                                kinetic.sendData();
                            }
                        }

                        player.displayClientMessage(shiftMessage(access), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        Vec3 hitVec = context.getClickLocation().subtract(HiddenCogwheelCompat.getModelTranslation(be, 1.0F));
                        double localX = hitVec.x - pos.getX();
                        double localY = hitVec.y - pos.getY();
                        double localZ = hitVec.z - pos.getZ();
                        double dx = localX - 0.5;
                        double dy = localY - 0.5;
                        double dz = localZ - 0.5;
                        double radius = CogwheelSizeHelper.getToolHighlightRadius(state.getBlock());
                        double centerThresh = 0.25 * radius * radius;
                        float step = 0.0625F;
                        float limit = 1.0F;
                        boolean toggledVisibility = false;
                        boolean chainShift = false;
                        if (clickedFace.getAxis() != blockAxis) {
                            chainShift = true;
                            int sign = getAxisDelta(blockAxis, dx, dy, dz) > 0.0 ? 1 : -1;

                            for (KineticBlockEntityPhysicsAccess nodeAccess : collectChainAccesses(level, pos, be)) {
                                shiftAxis(nodeAccess, blockAxis, sign * step, limit);
                            }
                        } else if (blockAxis == Axis.Z) {
                            double distSq = dx * dx + dy * dy;
                            if (distSq < centerThresh) {
                                access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                                toggledVisibility = true;
                            } else if (Math.abs(dx) > Math.abs(dy)) {
                                float newOffset = access.bnt$getAlignmentOffsetX() + (dx > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetX(Mth.clamp(newOffset, -limit, limit));
                            } else {
                                float newOffset = access.bnt$getAlignmentOffsetY() + (dy > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetY(Mth.clamp(newOffset, -limit, limit));
                            }
                        } else if (blockAxis == Axis.X) {
                            double distSq = dz * dz + dy * dy;
                            if (distSq < centerThresh) {
                                access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                                toggledVisibility = true;
                            } else if (Math.abs(dz) > Math.abs(dy)) {
                                float newOffset = access.bnt$getAlignmentOffsetZ() + (dz > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetZ(Mth.clamp(newOffset, -limit, limit));
                            } else {
                                float newOffset = access.bnt$getAlignmentOffsetY() + (dy > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetY(Mth.clamp(newOffset, -limit, limit));
                            }
                        } else if (blockAxis == Axis.Y) {
                            double distSq = dx * dx + dz * dz;
                            if (distSq < centerThresh) {
                                access.bnt$setHiddenByLever(!access.bnt$isHiddenByLever());
                                toggledVisibility = true;
                            } else if (Math.abs(dx) > Math.abs(dz)) {
                                float newOffset = access.bnt$getAlignmentOffsetX() + (dx > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetX(Mth.clamp(newOffset, -limit, limit));
                            } else {
                                float newOffset = access.bnt$getAlignmentOffsetZ() + (dz > 0.0 ? step : -step);
                                access.bnt$setAlignmentOffsetZ(Mth.clamp(newOffset, -limit, limit));
                            }
                        }

                        if (chainShift) {
                            for (BlockPos nodePosx : collectChainPositions(level, pos, be)) {
                                if (level.getBlockEntity(nodePosx) instanceof KineticBlockEntity kinetic) {
                                    kinetic.setChanged();
                                    kinetic.sendData();
                                }
                            }
                        } else {
                            be.setChanged();
                            ((KineticBlockEntity)be).sendData();
                        }

                        if (player != null) {
                            if (toggledVisibility) {
                                Component status = access.bnt$isHiddenByLever()
                                    ? Component.translatable("chat.bits_n_tracks.alignment.visibility.hidden")
                                    : Component.translatable("chat.bits_n_tracks.alignment.visibility.shown");
                                player.displayClientMessage(Component.translatable("chat.bits_n_tracks.alignment.visibility", new Object[]{status}), true);
                            } else if (chainShift) {
                                player.displayClientMessage(shiftMessage(access), true);
                            } else {
                                player.displayClientMessage(shiftMessage(access), true);
                            }
                        }

                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    private static double getAxisDelta(Axis axis, double dx, double dy, double dz) {
        return switch (axis) {
            case X -> dx;
            case Y -> dy;
            case Z -> dz;
            default -> throw new MatchException(null, null);
        };
    }

    private static void shiftAxis(KineticBlockEntityPhysicsAccess access, Axis axis, float delta, float limit) {
        switch (axis) {
            case X:
                access.bnt$setAlignmentOffsetX(Mth.clamp(access.bnt$getAlignmentOffsetX() + delta, -limit, limit));
                break;
            case Y:
                access.bnt$setAlignmentOffsetY(Mth.clamp(access.bnt$getAlignmentOffsetY() + delta, -limit, limit));
                break;
            case Z:
                access.bnt$setAlignmentOffsetZ(Mth.clamp(access.bnt$getAlignmentOffsetZ() + delta, -limit, limit));
        }
    }

    private static Component shiftMessage(KineticBlockEntityPhysicsAccess access) {
        return Component.translatable(
            "chat.bits_n_tracks.alignment.shift.3d",
            new Object[]{
                formatPixels(access.bnt$getAlignmentOffsetX()), formatPixels(access.bnt$getAlignmentOffsetY()), formatPixels(access.bnt$getAlignmentOffsetZ())
            }
        );
    }

    private static String formatPixels(float offset) {
        int px = Math.round(offset / 0.0625F);
        return (px > 0 ? "+" : "") + px + "px";
    }

    private static Set<KineticBlockEntityPhysicsAccess> collectChainAccesses(Level level, BlockPos pos, BlockEntity be) {
        Set<KineticBlockEntityPhysicsAccess> accesses = new LinkedHashSet<>();

        for (BlockPos nodePos : collectChainPositions(level, pos, be)) {
            if (level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess nodeAccess) {
                accesses.add(nodeAccess);
            }
        }

        return accesses;
    }

    private static Set<BlockPos> collectChainPositions(Level level, BlockPos pos, BlockEntity be) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(pos);
        CogwheelChainBehaviour behaviour = getChainBehaviour(be);
        if (behaviour == null) {
            return positions;
        } else {
            BlockPos controllerPos = pos;
            CogwheelChain chain = behaviour.getControlledChain();
            if (chain == null && behaviour.getControllerOffset() != null) {
                controllerPos = pos.offset(behaviour.getControllerOffset());
                BlockEntity controllerBe = level.getBlockEntity(controllerPos);
                CogwheelChainBehaviour controllerBehaviour = getChainBehaviour(controllerBe);
                if (controllerBehaviour != null) {
                    chain = controllerBehaviour.getControlledChain();
                }
            }

            if (chain == null) {
                return positions;
            } else {
                for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
                    positions.add(controllerPos.offset(node.localPos()));
                }

                return positions;
            }
        }
    }

    private static CogwheelChainBehaviour getChainBehaviour(BlockEntity be) {
        return be instanceof SmartBlockEntity smartBe ? (CogwheelChainBehaviour)smartBe.getBehaviour(CogwheelChainBehaviour.TYPE) : null;
    }
}
