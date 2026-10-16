package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SmartBlockEntity.class, remap = false)
public abstract class SmartBlockEntitySchematicMixin {
    @Inject(method = "writeSafe", at = @At("TAIL"))
    private void bnt$writeCogwheelSettings(CompoundTag tag, Provider registries, CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity)(Object)this;
        if (self.getBlockState().getBlock() instanceof BntFlangedCogwheelBlock && self instanceof KineticBlockEntityPhysicsAccess access) {
            access.bnt$writeSettings(tag);
        }
    }

    @Inject(method = "getRequiredItems", at = @At("RETURN"), cancellable = true)
    private void bnt$requireSuspensionPiece(BlockState state, CallbackInfoReturnable<ItemRequirement> cir) {
        SmartBlockEntity self = (SmartBlockEntity)(Object)this;
        if (BntFlangedCogwheelBlock.physics(state)
            && self instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$getSuspensionSide() != 0
            && !BntSuspension.heldByWidePartner(self)) {
            cir.setReturnValue(cir.getReturnValue().union(new ItemRequirement(ItemUseType.CONSUME, BitsNTracksItems.SUSPENSION_PIECE.get())));
        }
    }
}
