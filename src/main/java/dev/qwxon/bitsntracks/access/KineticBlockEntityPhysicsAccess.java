package dev.qwxon.bitsntracks.access;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;

public interface KineticBlockEntityPhysicsAccess {
    boolean bnt$isPhysicsEnabled();

    void bnt$setPhysicsEnabled(boolean var1);

    double bnt$getExtension();

    void bnt$setExtension(double var1);

    double bnt$getLerpedExtension(float var1);

    boolean bnt$isLiftedUp();

    void bnt$setLiftedUp(boolean var1);

    double bnt$getMaxAirExtension();

    void bnt$setMaxAirExtension(double var1);


    ForceTotal bnt$getForceTotal();

    void bnt$markQueuedForForceApplication();

    boolean bnt$consumeQueuedForForceApplication();

    String bnt$getOriginalBlock();

    void bnt$setOriginalBlock(String var1);

    void bnt$setPhysicalSpeed(float var1);

    float bnt$getAlignmentOffsetX();

    void bnt$setAlignmentOffsetX(float var1);

    float bnt$getAlignmentOffsetY();

    void bnt$setAlignmentOffsetY(float var1);

    float bnt$getAlignmentOffsetZ();

    void bnt$setAlignmentOffsetZ(float var1);

    boolean bnt$isHiddenByLever();

    void bnt$setHiddenByLever(boolean var1);

    int bnt$getTrackRouteSide();

    void bnt$setTrackRouteSide(int var1);
}
