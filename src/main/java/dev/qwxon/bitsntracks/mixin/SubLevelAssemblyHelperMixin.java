package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.content.BntCogwheelTransform;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {SubLevelAssemblyHelper.class},
    remap = false
)
public class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private static void bnt$rotateMovedCogwheels(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        BntCogwheelTransform.afterSubLevelMove(transform, blocks);
    }
}
