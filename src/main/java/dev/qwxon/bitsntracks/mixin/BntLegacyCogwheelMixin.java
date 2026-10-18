package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.BntLegacyCogwheels;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticBlockEntity.class)
public abstract class BntLegacyCogwheelMixin {
    @Unique
    private CompoundTag bnt$legacyTag;

    @Inject(method = "read", at = @At("RETURN"))
    private void bnt$rememberLegacy(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && BntLegacyCogwheels.isLegacy((KineticBlockEntity)(Object)this, tag)) {
            this.bnt$legacyTag = tag.copy();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bnt$convertLegacy(CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity)(Object)this;
        if (this.bnt$legacyTag != null || BntLegacyCogwheels.isLegacy(self, null)) {
            CompoundTag legacy = this.bnt$legacyTag;
            this.bnt$legacyTag = null;
            BntLegacyCogwheels.convert(self, legacy);
        }
    }
}
