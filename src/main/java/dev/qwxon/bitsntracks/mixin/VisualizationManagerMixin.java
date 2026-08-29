package dev.qwxon.bitsntracks.mixin;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.qwxon.bitsntracks.client.BntClientConfig;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    value = {VisualizationManager.class},
    remap = false
)
public interface VisualizationManagerMixin {
    @Inject(
        method = {"supportsVisualization"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void bnt$disableFlywheelVisualization(LevelAccessor level, CallbackInfoReturnable<Boolean> cir) {
        if (BntClientConfig.isFlywheelVisualizationDisabled()) {
            cir.setReturnValue(false);
        }
    }
}
