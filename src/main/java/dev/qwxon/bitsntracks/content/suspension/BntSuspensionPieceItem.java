package dev.qwxon.bitsntracks.content.suspension;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class BntSuspensionPieceItem extends Item {
    public BntSuspensionPieceItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new BntSuspensionPieceItemRenderer()));
    }
}
