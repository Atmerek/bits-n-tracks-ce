package dev.qwxon.bitsntracks.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class BntCogwheelShafts {
    public static final EnumProperty<BntShaftEnds> SHAFTS = EnumProperty.create("shafts", BntShaftEnds.class);

    private BntCogwheelShafts() {
    }

    public static BntShaftEnds ends(BlockState state) {
        return state.hasProperty(SHAFTS) ? state.getValue(SHAFTS) : BntShaftEnds.BOTH;
    }

    public static boolean hideable(BlockState state, Direction end) {
        return state.hasProperty(SHAFTS)
            && end.getAxis() == state.getValue(RotatedPillarKineticBlock.AXIS)
            && end != BntCogwheelPairing.partnerDirection(state);
    }

    public static boolean has(BlockState state, Direction end) {
        return has(state, ends(state), end);
    }

    public static boolean has(BlockState state, BntShaftEnds ends, Direction end) {
        return !hideable(state, end) || ends.keeps(end.getAxisDirection());
    }

    public static List<Direction> outerEnds(BlockState state) {
        List<Direction> ends = new ArrayList<>(2);
        if (state.hasProperty(SHAFTS)) {
            for (AxisDirection direction : AxisDirection.values()) {
                Direction end = Direction.fromAxisAndDirection(state.getValue(RotatedPillarKineticBlock.AXIS), direction);
                if (hideable(state, end)) {
                    ends.add(end);
                }
            }
        }
        return ends;
    }

    public static BlockState withShaft(BlockState state, Direction end, boolean kept) {
        return hideable(state, end) ? state.setValue(SHAFTS, ends(state).with(end.getAxisDirection(), kept)) : state;
    }

    public static BlockState keepSeam(BlockState state) {
        Direction seam = BntCogwheelPairing.partnerDirection(state);
        return seam == null || !state.hasProperty(SHAFTS) ? state : state.setValue(SHAFTS, ends(state).with(seam.getAxisDirection(), true));
    }

    public static BlockState flipped(BlockState state) {
        return state.hasProperty(SHAFTS) ? state.setValue(SHAFTS, ends(state).flipped()) : state;
    }

    public static Result apply(Level level, Collection<BlockPos> positions, Direction end, boolean kept) {
        int changed = 0;
        int held = 0;
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!hideable(state, end) || has(state, end) == kept) {
                continue;
            }
            if (!kept && BntSuspension.mountedOn(level, pos, end)) {
                held++;
                continue;
            }
            KineticBlockEntity.switchToBlockState(level, pos, withShaft(state, end, kept));
            changed++;
        }
        return new Result(changed, held);
    }

    public record Result(int changed, int held) {
    }
}
