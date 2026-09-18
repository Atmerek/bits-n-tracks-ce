package dev.qwxon.bitsntracks.client.ponder;

import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public final class BntLabelSequenceInstruction extends TickingInstruction {
    private final List<TextWindowElement> labels;
    private final int[] ends;

    public BntLabelSequenceInstruction(List<TextWindowElement> labels, int[] durations) {
        super(false, total(durations));
        this.labels = labels;
        this.ends = new int[durations.length];
        int end = 0;
        for (int i = 0; i < durations.length; i++) {
            end += durations[i];
            this.ends[i] = end;
        }
    }

    private static int total(int[] durations) {
        int sum = 0;
        for (int duration : durations) {
            sum += duration;
        }
        return sum;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        super.firstTick(scene);
        for (TextWindowElement label : this.labels) {
            scene.addElement(label);
            label.setFade(1.0F);
            label.setFade(1.0F);
            label.setVisible(false);
        }
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        int elapsed = this.totalTicks - this.remainingTicks;
        int shown = this.remainingTicks <= 0 ? -1 : 0;
        while (shown >= 0 && shown < this.ends.length - 1 && elapsed > this.ends[shown]) {
            shown++;
        }
        for (int i = 0; i < this.labels.size(); i++) {
            this.labels.get(i).setVisible(i == shown);
        }
    }
}
