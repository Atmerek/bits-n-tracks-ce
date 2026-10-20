package dev.qwxon.bitsntracks.content;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChains;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class BntCogwheelWidth {
    public static final IntegerProperty TRIM = IntegerProperty.create("trim", 0, 3);
    public static final int SINGLE = 4;
    public static final int PAIR = 8;
    public static final double FLANGE_INNER = 4.5;
    private static final double SINGLE_RIM = 8.0 - FLANGE_INNER;
    private static final double WIDE_RIM = 16.0 - FLANGE_INNER;
    private static final double MIN_RIM = 0.5;

    private BntCogwheelWidth() {
    }

    public static int trim(BlockState state) {
        return state.hasProperty(TRIM) ? state.getValue(TRIM) : 0;
    }

    public static int maxTrim(BlockState state) {
        return BntCogwheelPairing.isWide(state) ? 3 : 2;
    }

    public static int full(BlockState state) {
        return BntCogwheelPairing.isWide(state) ? PAIR : SINGLE;
    }

    public static int quarters(BlockState state) {
        return full(state) - trim(state);
    }

    public static double scale(BlockState state) {
        return quarters(state) / (double)full(state);
    }

    public static int trackPixels(int quarters) {
        return quarters > SINGLE ? 4 * quarters - 4 : 4 * quarters - 2;
    }

    public static double flangeShift(boolean wide, int trim) {
        return Math.min(2.0 * trim, (wide ? WIDE_RIM : SINGLE_RIM) - MIN_RIM);
    }

    public static BlockState withTrim(BlockState state, int trim) {
        return state.hasProperty(TRIM) ? state.setValue(TRIM, Math.max(0, Math.min(trim, maxTrim(state)))) : state;
    }

    public static int stepped(BlockState state, int direction) {
        return Math.floorMod(trim(state) + Integer.signum(direction), maxTrim(state) + 1);
    }

    public static boolean apply(Level level, Collection<BlockPos> positions, int trim) {
        boolean changed = false;
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            BlockState trimmed = withTrim(state, trim);
            if (trimmed != state) {
                level.setBlock(pos, trimmed, Block.UPDATE_ALL);
                changed = true;
            }
        }
        return changed;
    }

    public static int chainQuarters(KineticBlockEntity be) {
        Level level = be.getLevel();
        CogwheelChain chain = level == null ? null : BntChains.chainAt(level, be.getBlockPos());
        if (chain == null) {
            return quarters(be.getBlockState());
        }
        int widest = 0;
        BlockPos origin = be.getBlockPos();
        for (PathedCogwheelNode node : chain.getChainPathCogwheelNodes()) {
            BlockState state = level.getBlockState(origin.offset(node.localPos()));
            if (BntCogwheelPairing.supports(state)) {
                widest = Math.max(widest, quarters(state));
            }
        }
        return widest == 0 ? SINGLE : widest;
    }
}
