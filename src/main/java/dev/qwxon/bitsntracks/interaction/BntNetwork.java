package dev.qwxon.bitsntracks.interaction;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BntNetwork {
    private static final String VERSION = "2";

    private BntNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(
            BntRouteSidePayload.TYPE,
            BntRouteSidePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> WrenchPhysicsHandler.routeFromClient(context.player(), payload)));

        registrar.playToServer(
            BntBeltTensionPayload.TYPE,
            BntBeltTensionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntBeltTensionHandler.applyFromClient(context.player(), payload)));

        registrar.playToServer(
            BntTuningModePayload.TYPE,
            BntTuningModePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntTuningHandler.modeFromClient(context.player(), payload)));

        registrar.playToServer(
            BntAlignmentModePayload.TYPE,
            BntAlignmentModePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> WrenchPhysicsHandler.alignmentModeFromClient(context.player(), payload)));

        registrar.playToServer(
            BntSuspensionPayload.TYPE,
            BntSuspensionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntSuspensionHandler.placeFromClient(context.player(), payload)));

        registrar.playToServer(
            BntTuningPayload.TYPE,
            BntTuningPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntTuningHandler.tuneFromClient(context.player(), payload)));

        registrar.playToServer(
            BntLeverPayload.TYPE,
            BntLeverPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntLeverHandler.applyFromClient(context.player(), payload)));

        registrar.playToClient(
            BntToolMessagePayload.TYPE,
            BntToolMessagePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> BntToolMessages.receive(payload.message())));
    }
}
