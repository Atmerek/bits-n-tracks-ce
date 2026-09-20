package dev.qwxon.bitsntracks.physics;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PathedCogwheelNode;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainGeometry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BntDebugLog {
    public static final Logger LOG = LoggerFactory.getLogger("bits_n_tracks");

    private BntDebugLog() {
    }

    public static boolean enabled() {
        return BntPhysicsTuning.isDebugLogging();
    }

    public static String side(Level level) {
        return level != null && level.isClientSide ? "client" : "server";
    }

    public static String layout(List<PathedCogwheelNode> nodes, BntChainGeometry.Layout layout) {
        if (layout == null) {
            return "none";
        }
        StringBuilder line = new StringBuilder("belt");
        boolean[] onBelt = new boolean[nodes.size()];
        for (int index : layout.sequence()) {
            if (index < 0 || index >= nodes.size()) {
                continue;
            }
            onBelt[index] = true;
            BlockPos pos = nodes.get(index).localPos();
            line.append(' ').append(pos.toShortString()).append(layout.sides()[index] > 0 ? '+' : '-');
        }
        StringBuilder off = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (!onBelt[i]) {
                off.append(' ').append(nodes.get(i).localPos().toShortString());
            }
        }
        return off.isEmpty() ? line.toString() : line.append(" | off").append(off).toString();
    }
}
