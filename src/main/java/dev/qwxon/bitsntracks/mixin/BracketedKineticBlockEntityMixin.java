package dev.qwxon.bitsntracks.mixin;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BracketedKineticBlockEntity.class})
public class BracketedKineticBlockEntityMixin {
    @Inject(
        method = {"transform"},
        at = {@At("TAIL")}
    )
    private void bnt$transformControlledChain(BlockEntity be, StructureTransform transform, CallbackInfo ci) {
        HiddenCogwheelCompat.transformControlledChain((BracketedKineticBlockEntity)(Object)this, transform);
    }
}
