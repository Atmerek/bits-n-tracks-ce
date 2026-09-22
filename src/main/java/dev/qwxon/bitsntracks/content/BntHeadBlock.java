package dev.qwxon.bitsntracks.content;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BntHeadBlock extends Block implements Equipable, EntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");

    private static final int ROTATIONS = 16;
    private static final VoxelShape FLOOR_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Map.of(
        Direction.NORTH, Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0),
        Direction.SOUTH, Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0),
        Direction.EAST, Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0),
        Direction.WEST, Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0)
    );

    public BntHeadBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, WALL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BitsNTracksBlockEntityTypes.HEAD.create(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WALL)
            ? WALL_SHAPES.get(RotationSegment.convertToDirection(state.getValue(ROTATION)).orElse(Direction.NORTH))
            : FLOOR_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        return clicked.getAxis().isHorizontal()
            ? this.defaultBlockState().setValue(WALL, true).setValue(ROTATION, RotationSegment.convertToSegment(clicked))
            : this.defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}
