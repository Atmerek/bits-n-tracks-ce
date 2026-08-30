package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.EmptyFlangedGearBlock;
import com.kipti.bnb.registry.content.blocks.BnbKineticBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import java.util.function.Consumer;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class HiddenCogwheelBlock extends EmptyFlangedGearBlock {
    private final CogwheelSize size;

    public HiddenCogwheelBlock(Properties properties, CogwheelSize size) {
        super(properties, size == CogwheelSize.LARGE || size == CogwheelSize.MEDIUM);
        this.size = size;
    }

    public Class<KineticBlockEntity> getBlockEntityClass() {
        return KineticBlockEntity.class;
    }

    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends KineticBlockEntity>)BitsNTracksBlockEntityTypes.HIDDEN_COGWHEEL.get();
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = BntCogwheelShapes.get(this.size, (Axis)state.getValue(AXIS));
        return HiddenCogwheelCompat.offsetShapeForSuspension(shape, level, pos);
    }

    @SuppressWarnings("deprecation")
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String orig = access.bnt$getOriginalBlock();
            if (orig != null && !orig.isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(orig);
                if (loc != null) {
                    Block origBlock = (Block)BuiltInRegistries.BLOCK.get(loc);
                    if (origBlock != null && origBlock != Blocks.AIR) {
                        return new ItemStack(origBlock);
                    }
                }
            }
        }

        if (this.size == CogwheelSize.LARGE) {
            return new ItemStack((ItemLike)BnbKineticBlocks.LARGE_FLANGED_COGWHEEL.get());
        } else if (this.size == CogwheelSize.MEDIUM) {
            return new ItemStack((ItemLike)BitsNTracksBlocks.MEDIUM_FLANGED_COGWHEEL.get());
        } else {
            return this.size == CogwheelSize.TINY
                ? new ItemStack((ItemLike)BitsNTracksBlocks.TINY_FLANGED_COGWHEEL.get())
                : new ItemStack((ItemLike)BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.get());
        }
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                BlockState originalState = HiddenCogwheelBlock.getOriginalParticleState(level, pos, state);
                if (originalState == null) {
                    return false;
                } else {
                    manager.destroy(pos, originalState);
                    return true;
                }
            }
        });
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide() && !player.isCreative() && level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String orig = access.bnt$getOriginalBlock();
            if (orig != null && !orig.isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(orig);
                if (loc != null) {
                    Block origBlock = (Block)BuiltInRegistries.BLOCK.get(loc);
                    if (origBlock != null && origBlock != Blocks.AIR) {
                        ItemStack drop = new ItemStack(origBlock);
                        Block.popResource(level, pos, drop);
                    }
                }
            }
        }

        return result;
    }

    private static BlockState getOriginalParticleState(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
            String originalBlock = access.bnt$getOriginalBlock();
            if (originalBlock != null && !originalBlock.isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(originalBlock);
                if (loc == null) {
                    return null;
                } else {
                    Block original = (Block)BuiltInRegistries.BLOCK.get(loc);
                    return original != null && original != Blocks.AIR && !(original instanceof HiddenCogwheelBlock)
                        ? copySharedProperties(state, original.defaultBlockState())
                        : null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) {
                to = copyProperty(from, to, property);
            }
        }

        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return (BlockState)to.setValue(property, from.getValue(property));
    }
}
