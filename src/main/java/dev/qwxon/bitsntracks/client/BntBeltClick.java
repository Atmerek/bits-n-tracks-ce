package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.ChainCoordinateSpace;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntGrousers;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import dev.qwxon.bitsntracks.interaction.BntGrouserPayload;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntBeltClick {
    private static final double WHEEL_MARGIN = 0.25;

    private BntBeltClick() {
    }

    public static boolean wouldRetension() {
        return aimedTrack() != null;
    }

    @Nullable
    public static BlockPos aimedTrack() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return null;
        }

        BlockPos controllerPos = CogwheelChainInteractionHandler.getSelectedController();
        Vec3 baked = CogwheelChainInteractionHandler.getSelectedBakedPosition();
        return controllerPos != null && baked != null
            && BntCogwheels.isOwnChain(level, controllerPos)
            && !onAimedCogwheel(level, controllerPos, baked, minecraft.hitResult)
            ? controllerPos
            : null;
    }

    public static void showGrouserCost(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.hasInfiniteMaterials() || !player.getMainHandItem().is(BitsNTracksItems.GROUSER.get())) {
            return;
        }
        BlockPos controllerPos = aimedTrack();
        Level level = player.level();
        if (controllerPos == null || !BntGrousers.accepts(BntChains.chainAt(level, controllerPos))
            || BntGrousers.at(level, controllerPos) > 0) {
            return;
        }
        int cost = BntGrousers.cost(level, controllerPos);
        BlueprintOverlayRenderer.displayChainRequirements(BitsNTracksItems.GROUSER.get(), cost,
            ChainConveyorBlockEntity.getChainsFromInventory(player, player.getMainHandItem(), cost, true));
    }

    public static boolean fitGrousers() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().is(BitsNTracksItems.GROUSER.get())) {
            return false;
        }
        BlockPos controllerPos = aimedTrack();
        if (controllerPos == null) {
            return false;
        }
        PacketDistributor.sendToServer(new BntGrouserPayload(controllerPos));
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private static boolean onAimedCogwheel(Level level, BlockPos controllerPos, Vec3 baked, HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos aimed = blockHit.getBlockPos();
        BlockState state = level.getBlockState(aimed);
        if (!state.hasProperty(BlockStateProperties.AXIS) || !(level.getBlockEntity(aimed) instanceof KineticBlockEntity)) {
            return false;
        }

        ChainCoordinateSpace space = ChainCoordinateSpace.forRender(level, controllerPos);
        Vec3 beltPoint = space.toWorld(baked);
        Vec3 wheelCentre = space.toWorld(Vec3.atCenterOf(aimed.subtract(controllerPos)));
        double reach = CogwheelSizeHelper.getChainRadius(state.getBlock()) + WHEEL_MARGIN;
        return beltPoint.distanceToSqr(wheelCentre) <= reach * reach;
    }
}
