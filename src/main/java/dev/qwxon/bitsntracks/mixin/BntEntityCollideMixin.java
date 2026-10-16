package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class BntEntityCollideMixin {
    @ModifyVariable(
        method = "collide",
        at = @At("STORE"),
        ordinal = 0
    )
    private List<VoxelShape> bnt$collideWithDisplacedCogwheels(List<VoxelShape> shapes, @Local(argsOnly = true) Vec3 movement) {
        Entity self = (Entity)(Object)this;
        AABB box = self.getBoundingBox().expandTowards(movement).expandTowards(0.0, self.maxUpStep(), 0.0);
        return BntCogwheelCollision.withDisplacedShapes(self.level(), box, shapes);
    }
}
