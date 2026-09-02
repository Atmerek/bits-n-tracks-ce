package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.content.BntLeverZone;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.interaction.BntRouteSidePayload;
import dev.qwxon.bitsntracks.interaction.WrenchPhysicsHandler;
import dev.qwxon.bitsntracks.physics.CogwheelSizeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BntClientRouteClick {
    private BntClientRouteClick() {
    }

    public static void send(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return;
        }

        Axis blockAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
        if (hit.getDirection().getAxis() != blockAxis) {
            PacketDistributor.sendToServer(new BntRouteSidePayload(pos, WrenchPhysicsHandler.FLAT_FACE));
            return;
        }

        Vec3 translation = HiddenCogwheelCompat.getModelTranslation(
            minecraft.level.getBlockEntity(pos), BntClientCompat.getPartialTick());
        Vec3 local = hit.getLocation().subtract(translation).subtract(Vec3.atLowerCornerOf(pos));
        Direction zone = BntLeverZone.of(blockAxis, local.x - 0.5, local.y - 0.5, local.z - 0.5,
            CogwheelSizeHelper.getToolHighlightRadius(state.getBlock()));
        PacketDistributor.sendToServer(new BntRouteSidePayload(pos, zone == null ? -1 : zone.ordinal()));
    }
}
