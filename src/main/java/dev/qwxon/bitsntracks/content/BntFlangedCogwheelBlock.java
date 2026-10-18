package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IExclusiveCogwheelChainBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import dev.qwxon.bitsntracks.index.BitsNTracksItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BntFlangedCogwheelBlock extends RotatedPillarKineticBlock implements IBE<BntFlangedCogwheelBlockEntity>, IExclusiveCogwheelChainBlock {
    public static final BooleanProperty PHYSICS = BooleanProperty.create("physics");

    private final CogwheelSize size;
    private final CogwheelMaterial material;

    public BntFlangedCogwheelBlock(Properties properties, CogwheelSize size, CogwheelMaterial material) {
        super(properties);
        this.size = size;
        this.material = material;
        this.registerDefaultState(this.defaultBlockState().setValue(PHYSICS, false));
    }

    public CogwheelSize size() {
        return this.size;
    }

    public CogwheelMaterial material() {
        return this.material;
    }

    public static boolean physics(BlockState state) {
        return state.getBlock() instanceof BntFlangedCogwheelBlock && state.getValue(PHYSICS);
    }

    public static boolean setPhysics(Level level, BlockPos pos, boolean enabled) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BntFlangedCogwheelBlock) || state.getValue(PHYSICS) == enabled) {
            return false;
        }
        if (!enabled && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            BntSuspension.detach(level, pos, kinetic, true);
        }
        level.setBlock(pos, state.setValue(PHYSICS, enabled), Block.UPDATE_ALL);
        BntChainEngagement.rebuild(level, pos);
        return true;
    }

    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(BntCogwheelPairing.WIDE, PHYSICS));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BntCogwheelPairing.pairOnPlacement(super.getStateForPlacement(context), context);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BntCogwheelPairing.linkPartner(level, pos, state);
        BntCogwheelPairing.adoptPartnerSettings(level, pos);
    }

    protected BlockState updateShape(
        BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos
    ) {
        return BntCogwheelPairing.unlinkBrokenPair(
            super.updateShape(state, direction, neighbourState, level, pos, neighbourPos), direction, neighbourState
        );
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HiddenCogwheelCompat.offsetShapeForSuspension(this.getGridShape(state), level, pos);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(PHYSICS)) {
            return this.getGridShape(state);
        }
        return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null
            ? this.entityCollisionShape(state, level, pos)
            : Shapes.empty();
    }

    public VoxelShape entityCollisionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever()) {
            return Shapes.empty();
        }
        return HiddenCogwheelCompat.offsetShapeForSuspension(this.getGridShape(state), be);
    }

    private VoxelShape getGridShape(BlockState state) {
        return BntCogwheelShapes.get(this.size, state.getValue(AXIS));
    }

    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (state.getValue(PHYSICS)
            && be instanceof KineticBlockEntityPhysicsAccess access
            && access.bnt$getSuspensionSide() != 0
            && !BntSuspension.heldByWidePartner(be)) {
            drops.add(new ItemStack(BitsNTracksItems.SUSPENSION_PIECE.get()));
        }
        return drops;
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (state.getValue(PHYSICS) && !level.isClientSide() && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            BntSuspension.detach(level, pos, kinetic, !player.isCreative());
        }
        return result;
    }

    public Class<BntFlangedCogwheelBlockEntity> getBlockEntityClass() {
        return BntFlangedCogwheelBlockEntity.class;
    }

    public BlockEntityType<? extends BntFlangedCogwheelBlockEntity> getBlockEntityType() {
        return BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get();
    }

    public boolean isLargeCog() {
        return this.size == CogwheelSize.LARGE || this.size == CogwheelSize.MEDIUM;
    }

    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
