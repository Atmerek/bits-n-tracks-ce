package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IExclusiveCogwheelChainBlock;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.qwxon.bitsntracks.index.BitsNTracksBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BntFlangedCogwheelBlock extends RotatedPillarKineticBlock implements IBE<BntFlangedCogwheelBlockEntity>, IExclusiveCogwheelChainBlock {
    private final CogwheelSize size;

    public BntFlangedCogwheelBlock(Properties properties, CogwheelSize size) {
        super(properties);
        this.size = size;
    }

    public static BntFlangedCogwheelBlock tiny(Properties properties) {
        return new BntFlangedCogwheelBlock(properties, CogwheelSize.TINY);
    }

    public static BntFlangedCogwheelBlock small(Properties properties) {
        return new BntFlangedCogwheelBlock(properties, CogwheelSize.SMALL);
    }

    public static BntFlangedCogwheelBlock medium(Properties properties) {
        return new BntFlangedCogwheelBlock(properties, CogwheelSize.MEDIUM);
    }

    public static BntFlangedCogwheelBlock large(Properties properties) {
        return new BntFlangedCogwheelBlock(properties, CogwheelSize.LARGE);
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public Axis getRotationAxis(BlockState state) {
        return (Axis)state.getValue(AXIS);
    }

    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.size == CogwheelSize.LARGE) {
            return AllShapes.LARGE_GEAR.get((Axis)state.getValue(AXIS));
        } else {
            return this.size == CogwheelSize.MEDIUM
                ? AllShapes.LARGE_GEAR.get((Axis)state.getValue(AXIS))
                : AllShapes.SMALL_GEAR.get((Axis)state.getValue(AXIS));
        }
    }

    public Class<BntFlangedCogwheelBlockEntity> getBlockEntityClass() {
        return BntFlangedCogwheelBlockEntity.class;
    }

    public BlockEntityType<? extends BntFlangedCogwheelBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends BntFlangedCogwheelBlockEntity>)BitsNTracksBlockEntityTypes.SIMPLE_KINETIC.get();
    }

    public boolean isLargeCog() {
        return this.size == CogwheelSize.LARGE || this.size == CogwheelSize.MEDIUM;
    }

    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        return AllItems.WRENCH.isIn(stack) ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
