package dev.qwxon.bitsntracks.access;

import dev.qwxon.bitsntracks.physics.BntTuning;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;

public interface KineticBlockEntityPhysicsAccess {
    boolean bnt$isPhysicsEnabled();

    double bnt$getExtension();

    void bnt$setExtension(double extension);

    double bnt$getLerpedExtension(float partialTick);

    double bnt$getLastTerrainExtension();

    void bnt$setLastTerrainExtension(double extension);

    long bnt$getGroundDropTick();

    double bnt$getGroundDrop();

    void bnt$setGroundDrop(long gameTime, double drop);

    long bnt$getRawDropTick();

    double bnt$getRawDrop();

    void bnt$setRawDrop(long gameTime, double drop);

    Object bnt$getTerrainCast();

    void bnt$setTerrainCast(Object cast);

    boolean bnt$isLiftedUp();

    void bnt$setLiftedUp(boolean liftedUp);

    double bnt$getMaxAirExtension();

    void bnt$setMaxAirExtension(double maxAirExtension);

    double bnt$getSpringImpulse();

    void bnt$setSpringImpulse(double springImpulse);

    int bnt$getSuspensionSide();

    int bnt$getSuspensionFacing();

    void bnt$setSuspension(int side, int facing);

    ForceTotal bnt$getForceTotal();

    void bnt$markQueuedForForceApplication();

    boolean bnt$consumeQueuedForForceApplication();

    void bnt$setPhysicalSpeed(float speed);

    float bnt$getAlignmentOffsetX();

    void bnt$setAlignmentOffsetX(float x);

    float bnt$getAlignmentOffsetY();

    void bnt$setAlignmentOffsetY(float y);

    float bnt$getAlignmentOffsetZ();

    void bnt$setAlignmentOffsetZ(float z);

    boolean bnt$isHiddenByLever();

    void bnt$setHiddenByLever(boolean hidden);

    int bnt$getTrackRouteSide();

    void bnt$setTrackRouteSide(int side);

    float bnt$getBeltTension();

    void bnt$setBeltTension(float tension);

    int bnt$getBeltLinks();

    void bnt$setBeltLinks(int links);

    int bnt$getGrousers();

    void bnt$setGrousers(int grousers);

    long bnt$getDrawnDropTick();

    void bnt$advanceDrawnDrop(long gameTime, double sample, double rate);

    double bnt$getDrawnDrop(float partialTick);

    double bnt$getBeltFit();

    void bnt$setBeltFit(double fit);

    long bnt$getTrackLiftTick();

    double bnt$getTrackLift();

    void bnt$setTrackLift(long tick, double lift);

    long bnt$getBeltHoldTick();

    double bnt$getBeltHold();

    void bnt$setBeltHold(long tick, double hold);

    int bnt$getTuning(BntTuning setting);

    void bnt$setTuning(BntTuning setting, int level);
}
