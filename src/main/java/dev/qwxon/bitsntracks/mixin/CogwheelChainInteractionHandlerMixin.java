package dev.qwxon.bitsntracks.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.qwxon.bitsntracks.client.BntBeltClick;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CogwheelChainInteractionHandler.class, remap = false)
public class CogwheelChainInteractionHandlerMixin {
    @Unique
    private static boolean bnt$grousersOnly;

    @Shadow
    private static BlockPos selectedController;

    @Shadow
    private static void clearSelection() {
    }

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private static void bnt$activateForLever(LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        bnt$grousersOnly = false;
        if (bnt$tensionLever(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isActive", at = @At("RETURN"), cancellable = true)
    private static void bnt$activateForGrousers(LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && player != null && player.getMainHandItem().is(BitsNTracksItems.GROUSER.get())) {
            bnt$grousersOnly = true;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void bnt$selectOwnTracksForGrousers(CallbackInfo ci) {
        if (bnt$grousersOnly && selectedController != null
            && !BntCogwheels.isOwnChain(Minecraft.getInstance().level, selectedController)) {
            clearSelection();
        }
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private static void bnt$fitGrousers(CallbackInfoReturnable<Boolean> cir) {
        if (BntBeltClick.fitGrousers()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawCustomBlockSelection", at = @At("HEAD"), cancellable = true)
    private static void bnt$leaveOutlineToLever(PoseStack ms, MultiBufferSource buffer, Vec3 camera, CallbackInfo ci) {
        if (bnt$tensionLever(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean bnt$tensionLever(LocalPlayer player) {
        return player != null && player.getMainHandItem().getItem() instanceof CogAlignmentLeverItem
            && CogAlignmentLeverItem.mode(player.getMainHandItem()) == BntLeverMode.TENSION;
    }
}
