package dev.qwxon.bitsntracks.interaction;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BntNetwork {
    private BntNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
            BntRouteSidePayload.TYPE,
            BntRouteSidePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> WrenchPhysicsHandler.routeFromClient(context.player(), payload)));
    }
}
