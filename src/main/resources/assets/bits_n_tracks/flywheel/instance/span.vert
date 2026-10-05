void flw_instanceVertex(in FlwInstance instance) {
    float elapsed = clamp(float(int(flw_ticks) - instance.stamp) + flw_partialTick - instance.timing.x, 0.0, instance.timing.y);
    float place = instance.along.x + instance.along.y * elapsed;
    float t = flw_vertexPos.z;
    vec3 start = instance.originFrom + flw_vertexPos.x * instance.acrossFrom + flw_vertexPos.y * instance.upFrom;
    vec3 end = instance.originTo + flw_vertexPos.x * instance.acrossTo + flw_vertexPos.y * instance.upTo;
    vec3 across = mix(instance.acrossFrom, instance.acrossTo, t);
    vec3 up = mix(instance.upFrom, instance.upTo, t);
    vec3 run = end - start;
    vec3 normal = flw_vertexNormal.x * cross(up, run) + flw_vertexNormal.y * cross(run, across) + flw_vertexNormal.z * cross(across, up);
    float size = length(normal);
    flw_vertexNormal = size > 1.0e-6 ? normal / size : vec3(0.0, 1.0, 0.0);
    flw_vertexPos = vec4(place > 0.0 && place <= 1.0 ? mix(start, end, t) + instance.drift * elapsed : instance.originFrom, 1.0);
    flw_vertexTexCoord.y = mix(instance.textureV.x, instance.textureV.y, flw_vertexTexCoord.y) + instance.timing.z * elapsed;
    flw_vertexLight = max(mix(vec2(instance.lightFrom), vec2(instance.lightTo), t) / 256.0, flw_vertexLight);
}
