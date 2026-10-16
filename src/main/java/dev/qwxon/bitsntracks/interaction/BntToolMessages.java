package dev.qwxon.bitsntracks.interaction;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BntToolMessages {
    private static final long SHOWN_MILLIS = 3000L;

    private static volatile Component latest;
    private static volatile long shownAt;

    private BntToolMessages() {
    }

    public static void send(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new BntToolMessagePayload(message));
        }
    }

    public static void receive(Component message) {
        latest = message;
        shownAt = Util.getMillis();
    }

    public static Component current() {
        Component message = latest;
        return message != null && Util.getMillis() - shownAt < SHOWN_MILLIS ? message : null;
    }

    public static void clear() {
        latest = null;
    }
}
