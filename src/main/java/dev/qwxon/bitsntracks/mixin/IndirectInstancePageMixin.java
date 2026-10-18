package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.engine_room.flywheel.backend.util.AtomicBitSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.engine_room.flywheel.backend.engine.indirect.IndirectInstancer$InstancePage", remap = false)
public abstract class IndirectInstancePageMixin {
    @WrapOperation(
        method = "takeFrom",
        at = @At(
            value = "INVOKE",
            target = "Ldev/engine_room/flywheel/backend/util/AtomicBitSet;clear(I)V",
            ordinal = 1
        ),
        require = 0
    )
    private void bnt$keepRemainderUpload(AtomicBitSet contentsChanged, int page, Operation<Void> original) {
    }
}
