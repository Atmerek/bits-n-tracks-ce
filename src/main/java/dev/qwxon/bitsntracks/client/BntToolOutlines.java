package dev.qwxon.bitsntracks.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.BntLeverZone;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspensionPieceItem;
import dev.qwxon.bitsntracks.interaction.WrenchPhysicsHandler;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntToolOutlines {
    private static final double MARKER_REACH = 0.75;
    private static final int MAX_OUTLINES = 64;

    private BntToolOutlines() {
    }

    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        Item item = player.getMainHandItem().getItem();
        if ((item instanceof CogAlignmentLeverItem || item instanceof SuspensionToolItem || item instanceof BntSuspensionPieceItem)
            && HiddenCogwheelCompat.isOwnCogwheel(level, event.getTarget().getBlockPos())) {
            event.setCanceled(true);
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof CogAlignmentLeverItem) {
            lever(level, CogAlignmentLeverItem.mode(stack), CogAlignmentLeverItem.wholeTrack(stack));
        } else if (stack.getItem() instanceof SuspensionToolItem) {
            tuner(level, SuspensionToolItem.wholeTrack(stack));
        }
    }

    private static void lever(ClientLevel level, BntLeverMode mode, boolean wholeTrack) {
        BlockPos aimed = BntToolInput.aimedCog(level);
        switch (mode) {
            case OFFSET -> {
                BlockPos selected = BntToolState.leverCog;
                Set<BlockPos> chosen = selected == null ? Set.of() : wholeTrack ? movedWith(level, selected) : withFollowers(level, selected);
                selected(level, chosen, BntToolVisuals.SELECTED);
                if (aimed != null && !chosen.contains(aimed)) {
                    hover(level, wholeTrack ? movedWith(level, aimed) : withFollowers(level, aimed));
                }
            }
            case ROUTE -> {
                if (aimed != null) {
                    hover(level, withPartner(level, aimed));
                    routeMarkers(level, aimed);
                }
            }
            case TENSION -> {
                BlockPos chain = BntToolState.leverChain;
                if (chain != null) {
                    BntChainOutline.showWhole(BntChainOutline.SELECTED_SLOT, chain, BntToolVisuals.SELECTED, BntToolVisuals.BOLD);
                }
                BlockPos hovered = BntToolInput.aimedChain(level, aimed);
                if (hovered != null && !hovered.equals(chain)) {
                    BntChainOutline.showWhole(BntChainOutline.HOVER_SLOT, hovered, BntToolVisuals.HOVER, BntToolVisuals.THIN);
                }
            }
            case PHYSICS -> {
                BlockPos target = BntToolInput.aimedBlock(level);
                if (target != null && WrenchPhysicsHandler.canTogglePhysics(level, target)) {
                    hover(level, withPartner(level, target));
                }
            }
            case RESET -> {
                BlockPos armed = BntToolState.resetArmed;
                Set<BlockPos> warned = armed != null && BntToolState.resetArmedFor(armed) ? trackWithPartners(level, armed) : Set.of();
                selected(level, warned, BntToolVisuals.WARNING);
                if (aimed != null && !warned.contains(aimed)) {
                    hover(level, trackWithPartners(level, aimed));
                }
            }
            case VISIBILITY -> {
                if (aimed != null) {
                    hover(level, withPartner(level, aimed));
                }
            }
        }
    }

    private static void tuner(ClientLevel level, boolean wholeTrack) {
        BlockPos selected = BntToolState.tunerCog;
        Set<BlockPos> chosen = selected == null ? Set.of() : wholeTrack ? suspendedOnTrack(level, selected) : suspendedWith(level, selected);
        selected(level, chosen, BntToolVisuals.SELECTED);
        BlockPos aimed = BntToolInput.aimedBlock(level);
        if (aimed != null && BntSuspension.hasPiece(level.getBlockEntity(aimed)) && !chosen.contains(aimed)) {
            hover(level, wholeTrack ? suspendedOnTrack(level, aimed) : suspendedWith(level, aimed));
        }
    }

    private static void selected(ClientLevel level, Set<BlockPos> positions, int colour) {
        outline(level, positions, "bnt_tool_selected_", colour, BntToolVisuals.BOLD);
    }

    private static void hover(ClientLevel level, Set<BlockPos> positions) {
        outline(level, positions, "bnt_tool_hover_", BntToolVisuals.HOVER, BntToolVisuals.THIN);
    }

    private static void outline(ClientLevel level, Set<BlockPos> positions, String slot, int colour, float width) {
        int index = 0;
        for (BlockPos pos : positions) {
            if (index >= MAX_OUTLINES) {
                return;
            }
            BntToolVisuals.cog(slot + index++, level, pos, colour, width);
        }
    }

    private static Set<BlockPos> withPartner(ClientLevel level, BlockPos pos) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(pos);
        BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, pos);
        if (partnerPos != null) {
            positions.add(partnerPos);
        }
        return positions;
    }

    private static Set<BlockPos> withFollowers(ClientLevel level, BlockPos pos) {
        Set<BlockPos> positions = withPartner(level, pos);
        for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, pos)) {
            positions.add(cog.getBlockPos());
        }
        return positions;
    }

    private static Set<BlockPos> suspendedWith(ClientLevel level, BlockPos pos) {
        Set<BlockPos> positions = withFollowers(level, pos);
        positions.removeIf(nodePos -> !BntSuspension.hasPiece(level.getBlockEntity(nodePos)));
        return positions;
    }

    private static Set<BlockPos> trackWithPartners(ClientLevel level, BlockPos pos) {
        Set<BlockPos> positions = new LinkedHashSet<>(BntBeltTension.chainPositions(level, pos));
        for (BlockPos nodePos : Set.copyOf(positions)) {
            BlockPos partnerPos = BntCogwheelPairing.partnerPos(level, nodePos);
            if (partnerPos != null) {
                positions.add(partnerPos);
            }
            for (KineticBlockEntity cog : BntSuspension.bogieCogs(level, nodePos)) {
                positions.add(cog.getBlockPos());
            }
        }
        return positions;
    }

    private static Set<BlockPos> suspendedOnTrack(ClientLevel level, BlockPos pos) {
        Set<BlockPos> positions = trackWithPartners(level, pos);
        positions.removeIf(nodePos -> !BntSuspension.hasPiece(level.getBlockEntity(nodePos)));
        return positions;
    }

    private static Set<BlockPos> movedWith(ClientLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access)) {
            return Set.of(pos);
        }
        boolean physics = access.bnt$isPhysicsEnabled();
        Set<BlockPos> positions = trackWithPartners(level, pos);
        positions.removeIf(nodePos -> !(level.getBlockEntity(nodePos) instanceof KineticBlockEntityPhysicsAccess node)
            || node.bnt$isPhysicsEnabled() != physics);
        positions.add(pos);
        return positions;
    }

    private static void routeMarkers(ClientLevel level, BlockPos pos) {
        Axis blockAxis = BntToolState.blockAxis(level, pos);
        if (blockAxis == null || !(level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access)) {
            return;
        }
        AABB box = BntToolVisuals.shapeBox(level, pos);
        double halfDepth = (box.max(blockAxis) - box.min(blockAxis)) * 0.5;
        Vec3 centre = BntToolVisuals.drawnCentre(level, pos);
        double reach = CogwheelSizeHelper.getToolHighlightRadius(level.getBlockState(pos).getBlock()) * MARKER_REACH;

        int pinned = access.bnt$getTrackRouteSide();
        Direction pinnedSide = pinned < 0 || pinned >= Direction.values().length ? null : Direction.values()[pinned];
        if (pinnedSide != null) {
            BntToolVisuals.box("bnt_tool_route_pinned", BntToolVisuals.marker(at(centre, pinnedSide, reach), blockAxis, halfDepth),
                BntToolVisuals.SELECTED, BntToolVisuals.BOLD);
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos) || blockHit.getDirection().getAxis() != blockAxis) {
            return;
        }
        Vec3 local = blockHit.getLocation().subtract(centre);
        Direction zone = BntLeverZone.of(blockAxis, local.x, local.y, local.z,
            CogwheelSizeHelper.getToolHighlightRadius(level.getBlockState(pos).getBlock()));
        Direction result = zone == pinnedSide ? null : zone;
        if (result == null && pinnedSide == null) {
            return;
        }
        BntToolVisuals.box("bnt_tool_route_aim", BntToolVisuals.marker(at(centre, result, reach), blockAxis, halfDepth),
            BntToolVisuals.HOVER, BntToolVisuals.THIN);
    }

    private static Vec3 at(Vec3 centre, @Nullable Direction side, double reach) {
        return side == null ? centre : centre.add(Vec3.atLowerCornerOf(side.getNormal()).scale(reach));
    }
}
