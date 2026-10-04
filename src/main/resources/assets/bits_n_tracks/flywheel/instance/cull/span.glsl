void flw_transformBoundingSphere(in FlwInstance instance, inout vec3 center, inout float radius) {
    center = (instance.originFrom + instance.originTo) * 0.5;
    radius = 0.5 * length(instance.originTo - instance.originFrom)
        + max(length(instance.acrossFrom), length(instance.acrossTo))
        + max(length(instance.upFrom), length(instance.upTo))
        + length(instance.drift) * instance.timing.y;
}
