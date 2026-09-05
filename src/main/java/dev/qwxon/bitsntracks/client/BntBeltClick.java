package dev.qwxon.bitsntracks.client;

import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.ChainCoordinateSpace;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.interaction.BntBeltTensionPayload;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class BntBeltClick {
    private static final double WHEEL_MARGIN = 0.25;

    private BntBeltClick() {
    }

    /** True when a click retensions a run instead of reaching the cogwheel. */
    public static boolean wouldRetension() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return false;
        }

        BlockPos controllerPos = CogwheelChainInteractionHandler.getSelectedController();
        Vec3 baked = CogwheelChainInteractionHandler.getSelectedBakedPosition();
        return controllerPos != null && baked != null
            && !onAimedCogwheel(level, controllerPos, baked, minecraft.hitResult);
    }

    /** Sends a tension step for the highlighted chain. */
    public static boolean send(Player player) {
        if (!wouldRetension()) {
            return false;
        }

        BlockPos controllerPos = CogwheelChainInteractionHandler.getSelectedController();
        PacketDistributor.sendToServer(new BntBeltTensionPayload(controllerPos, !player.isShiftKeyDown()));
        return true;
    }

    /** Compares the chain hit and the aimed block in the chain's own space. */
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
