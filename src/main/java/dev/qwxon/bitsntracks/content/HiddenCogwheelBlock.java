package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.EmptyFlangedGearBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HiddenCogwheelBlock extends EmptyFlangedGearBlock {
    private final CogwheelSize size;

    public HiddenCogwheelBlock(Properties properties, CogwheelSize size) {
        super(properties, size == CogwheelSize.LARGE || size == CogwheelSize.MEDIUM);
        this.size = size;
    }

    public CogwheelSize size() {
        return this.size;
    }

    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(new Property[]{BntCogwheelPairing.WIDE}));
    }

    public Class<KineticBlockEntity> getBlockEntityClass() {
        return KineticBlockEntity.class;
    }

    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends KineticBlockEntity>)BitsNTracksBlockEntityTypes.HIDDEN_COGWHEEL.get();
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BntCogwheelShapes.get(this.size, (Axis)state.getValue(AXIS));
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
