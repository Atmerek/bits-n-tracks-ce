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

        event.registrar("1").playToServer(
            BntBeltTensionPayload.TYPE,
            BntBeltTensionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntBeltTensionHandler.applyFromClient(context.player(), payload)));

        event.registrar("1").playToServer(
            BntTuningModePayload.TYPE,
            BntTuningModePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntTuningHandler.modeFromClient(context.player(), payload)));

        event.registrar("1").playToServer(
            BntTuningPayload.TYPE,
            BntTuningPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntTuningHandler.tuneFromClient(context.player(), payload)));
    }
}
