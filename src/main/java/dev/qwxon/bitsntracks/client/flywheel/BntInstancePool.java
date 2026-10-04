package dev.qwxon.bitsntracks.client.flywheel;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import java.util.ArrayList;
import java.util.List;

final class BntInstancePool<I extends Instance> {
    private static final int SPARE = 32;

    private final Instancer<I> instancer;
    private final List<I> instances = new ArrayList<>();
    private int used;
    private int shown;

    BntInstancePool(Instancer<I> instancer) {
        this.instancer = instancer;
    }

    void begin() {
        used = 0;
    }

    I next() {
        I instance;
        if (used < instances.size()) {
            instance = instances.get(used);
            if (used >= shown) {
                instance.setVisible(true);
            }
        } else {
            instance = instancer.createInstance();
            instances.add(instance);
        }
        used++;
        return instance;
    }

    void settle() {
        if (instances.size() > used * 2 + SPARE) {
            for (int i = instances.size() - 1; i >= used + SPARE; i--) {
                instances.remove(i).delete();
            }
        }
        for (int i = used; i < Math.min(shown, instances.size()); i++) {
            instances.get(i).setVisible(false);
        }
        shown = used;
    }

    void delete() {
        for (I instance : instances) {
            instance.delete();
        }
        instances.clear();
        used = 0;
        shown = 0;
    }
}
