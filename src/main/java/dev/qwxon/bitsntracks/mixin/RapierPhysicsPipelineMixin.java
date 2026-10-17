package dev.qwxon.bitsntracks.mixin;

import dev.qwxon.bitsntracks.physics.BntSubLevelLinks;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class RapierPhysicsPipelineMixin {
    @Inject(method = "addConstraint", at = @At("RETURN"))
    private void bnt$recordLink(
        PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB, PhysicsConstraintConfiguration<?> configuration,
        CallbackInfoReturnable<PhysicsConstraintHandle> cir
    ) {
        BntSubLevelLinks.record((PhysicsPipeline)(Object)this, bodyA, bodyB, configuration, cir.getReturnValue());
    }
}
