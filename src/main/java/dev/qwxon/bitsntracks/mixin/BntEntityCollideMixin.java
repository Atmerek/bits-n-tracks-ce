package dev.qwxon.bitsntracks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.qwxon.bitsntracks.physics.BntCogwheelCollision;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class BntEntityCollideMixin {
    @WrapOperation(
        method = "collideBoundingBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collectColliders(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        )
    )
    private static List<VoxelShape> bnt$collideWithDisplacedCogwheels(
        Entity entity, Level level, List<VoxelShape> entityShapes, AABB box, Operation<List<VoxelShape>> original
    ) {
        List<VoxelShape> shapes = original.call(entity, level, entityShapes, box);
        return entity == null ? shapes : BntCogwheelCollision.withDisplacedShapes(level, box, shapes);
    }
}
