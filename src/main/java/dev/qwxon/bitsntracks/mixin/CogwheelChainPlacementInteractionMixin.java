package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainInteractionFailedException;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementInteraction;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {CogwheelChainPlacementInteraction.class},
    remap = false
)
public abstract class CogwheelChainPlacementInteractionMixin {
    @Shadow
    private static PlacingCogwheelChain currentBuildingChain;

    @Inject(
        method = {"onRightClick"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )},
        cancellable = true
    )
    private static void bnt$leaveOtherBlocksAlone(InteractionKeyMappingTriggered event, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (currentBuildingChain == null && mc.level != null && mc.hitResult instanceof BlockHitResult hit
            && !(mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof CogWheelBlock)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = {"onRightClick"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/types/CogwheelChainType;getCogwheelPredicate()Ljava/util/function/Predicate;"
        )
    )
    private static Predicate<Block> bnt$redirectPredicate(CogwheelChainType type) {
        return block -> {
            boolean result = type.getCogwheelPredicate().test(block);
            if (!result) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.hitResult instanceof BlockHitResult bhr) {
                    BlockPos pos = bhr.getBlockPos();
                    BlockState state = mc.level.getBlockState(pos);
                    if (HiddenCogwheelCompat.isHiddenFlangedCogwheel(state)) {
                        BlockEntity be = mc.level.getBlockEntity(pos);
                        BlockState visibleState = HiddenCogwheelCompat.toVisibleCogwheelState(state, be);
                        if (visibleState != null && type.getCogwheelPredicate().test(visibleState.getBlock())) {
                            return true;
                        }
                    }
                }
            }

            return result;
        };
    }

    @WrapMethod(
        method = "rightClickForChain"
    )
    private static void bnt$aroundRightClickForChain(
        InteractionKeyMappingTriggered event,
        ClientLevel level,
        BlockPos hitPos,
        BlockState targetedState,
        CogwheelChainCandidate targetedCandidate,
        CogwheelChainType heldChainType,
        ItemStack chainItemInHand,
        LocalPlayer player,
        Operation<Void> original
    ) {
        String refused = BntCogwheelChainTypes.refusal(heldChainType, level, hitPos, targetedState);
        if (refused != null) {
            player.displayClientMessage(new ChainInteractionFailedException(refused).getComponent(), true);
            return;
        }

        HiddenCogwheelCompat.setPlacementLevel(level);

        try {
            original.call(event, level, hitPos, targetedState, targetedCandidate, heldChainType, chainItemInHand, player);
        } finally {
            HiddenCogwheelCompat.setPlacementLevel(null);
        }
    }
}
