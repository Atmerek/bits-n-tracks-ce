package dev.qwxon.bitsntracks.mixin;

import com.cake.azimuth.behaviour.SuperBlockEntityBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviourRenderer;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder.VertexEmitter;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.VertexShape;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.qwxon.bitsntracks.access.BntChainGeometryRefresh;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.client.BntTreadView;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import dev.qwxon.bitsntracks.content.BntCogwheels;
import dev.qwxon.bitsntracks.content.TrackModelRenderContext;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltFaces;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltLinks;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainEngagement;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainMotion;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainTextures;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntChainWrap;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTankTread;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntTrackSink;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.types.BntCogwheelChainTypes;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {CogwheelChainBehaviourRenderer.class},
    remap = false
)
public abstract class CogwheelChainBehaviourRendererMixin {
    @WrapMethod(
        method = "renderSafe"
    )
    private void bnt$aroundRenderSafe(
        SuperBlockEntityBehaviour behaviour,
        KineticBlockEntity be,
        float partialTicks,
        PoseStack ms,
        MultiBufferSource buffer,
        int light,
        int overlay,
        Operation<Void> original
    ) {
        TrackModelRenderContext.setRenderingTrack(
            be.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof TrackModelBehaviourAccess access && access.bnt$isTrackModel());
        TrackModelRenderContext.setRenderingCustomChain(BntCogwheels.isOwnCogwheel(be.getBlockState()));
        TrackModelRenderContext.setChainQuarters(BntCogwheelWidth.chainQuarters(be));
        TrackModelRenderContext.setWrapped(BntChainWrap.wrapped(be));
        BntClientCompat.beginChainLight(be);
        BntTreadView.begin(ms.last().pose(), be.getLevel());

        try {
            original.call(behaviour, be, partialTicks, ms, buffer, light, overlay);
        } finally {
            TrackModelRenderContext.setRenderingTrack(false);
            TrackModelRenderContext.setRenderingCustomChain(false);
            TrackModelRenderContext.setChainQuarters(BntCogwheelWidth.SINGLE);
            TrackModelRenderContext.setWrapped(false);
            BntBeltLinks.clearLoop();
            BntClientCompat.endChainLight();
            BntTreadView.end();
        }
    }

    @Unique
    private static double bnt$chainScroll(KineticBlockEntity be) {
        if (!(be.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof CogwheelChainBehaviour chain)) {
            return 0.0;
        }
        float rate = chain.getChainRotationFactor() * be.getSpeed() / 1200.0F;
        if (rate == 0.0F) {
            return 0.0;
        }
        float time = be.getLevel() == null
            ? AnimationTickHolder.getRenderTime()
            : AnimationTickHolder.getRenderTime(be.getLevel());
        return 2.0 * Math.PI * rate * time;
    }

    @Redirect(
        method = {"renderSafe"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/render/CogwheelChainRenderGeometryBuilder;buildSegments(Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"
        )
    )
    private List<ChainSegment> bnt$transformChainSegments(CogwheelChain chain, Vec3 origin, @Local(argsOnly = true) KineticBlockEntity be) {
        Level level = be.getLevel();
        BlockPos controllerPos = be.getBlockPos();
        Function<BlockPos, Vec3> outerDisplacement = BntChainMotion.swapDisplacementSource(
            relativePos -> BntClientCompat.getNodeDisplacement(be, relativePos));
        Function<BlockPos, Direction> outerRoute = BntChainMotion.swapRouteSource(
            relativePos -> BntChainEngagement.routeSide(level, controllerPos.offset(relativePos)));
        try (BntRadiusProvider.Scope ignored = BntRadiusProvider.at(level, controllerPos)) {
            if (chain instanceof BntChainGeometryRefresh refreshable) {
                refreshable.bnt$refreshChainGeometry(level, controllerPos);
            }

            List<ChainSegment> segments = BntClientCompat.transformChainSegments(
                CogwheelChainRenderGeometryBuilder.buildSegments(chain, origin), chain, be);
            double belt = BntBeltLinks.length(BntBeltLinks.context()) + BntBeltLinks.slackLength(BntBeltTension.contextTension());
            BntBeltLinks.setLoop(belt, bnt$chainScroll(be));
            return segments;
        } finally {
            BntChainMotion.swapDisplacementSource(outerDisplacement);
            BntChainMotion.swapRouteSource(outerRoute);
        }
    }

    @Redirect(
        method = {"renderChain"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"
        )
    )
    private boolean bnt$keepTrackDetail(Vec3 camera, Position pos, double distance) {
        return TrackModelRenderContext.isRenderingTrack() || BntTrackSink.current() != null || camera.closerThan(pos, distance);
    }
    @Redirect(
        method = {"renderChain"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object bnt$fixLightClipping(Function<Vector3f, Integer> lighter, Object vecObj, @Local(argsOnly = true) KineticBlockEntity be) {
        Vector3f vec = (Vector3f)vecObj;
        int light = BntClientCompat.chainLight(lighter, vec);
        if (light == 0 && be != null && be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        }

        return light;
    }

    @Inject(
        method = {"renderChainSlowerButWithoutGaps"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private static void bnt$renderChainPrecisionFix(
        PoseStack ms,
        MultiBufferSource buffer,
        float offset,
        float textureSquish,
        Vec3 preFrom,
        Vec3 from,
        Vec3 to,
        Vec3 postTo,
        Vec3 fromCogwheelAxis,
        Vec3 toCogwheelAxis,
        int lightAtSource,
        int lightAtDest,
        CogwheelChainType type,
        boolean flipInsideOutside,
        Matrix3f accumulatedOrientation,
        CallbackInfo ci
    ) {
        if (BntTrackSink.current() == null && !TrackModelRenderContext.isRenderingTrack()
            && !TrackModelRenderContext.isRenderingCustomChain() && !BntCogwheelChainTypes.isOwn(type)) {
            return;
        }
        ChainRenderInfo chainRenderInfo = type.getRenderType();
        Vec3 relPreFrom = preFrom.subtract(from);
        Vec3 relFrom = Vec3.ZERO;
        Vec3 relTo = to.subtract(from);
        Vec3 relPostTo = postTo.subtract(from);
        List<Vec3> destinationPoints = bnt$endPoints(
            relFrom, relTo, relPostTo, chainRenderInfo, toCogwheelAxis, accumulatedOrientation
        );
        if (fromCogwheelAxis.dot(toCogwheelAxis) < 0.99) {
            int rotationSign = fromCogwheelAxis.cross(toCogwheelAxis).dot(relTo) > 0.0 ? 1 : -1;
            accumulatedOrientation.mul(new Matrix3f(0.0F, rotationSign, 0.0F, -rotationSign, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F));
        }

        List<Vec3> sourcePoints = bnt$endPoints(
            relPreFrom, relFrom, relTo, chainRenderInfo, fromCogwheelAxis, accumulatedOrientation
        );
        destinationPoints = bnt$closestOrder(destinationPoints, sourcePoints);
        float length = (float)from.distanceTo(to);
        ms.pushPose();
        boolean isCustomBeltItem = BntCogwheelChainTypes.isOwn(type);
        boolean industrialBelt = type == BntCogwheelChainTypes.INDUSTRIAL_BELT_CHAIN.get();
        boolean isCustomBeltPlacement = chainRenderInfo == ChainRenderInfo.BELT
            && (isCustomBeltItem || TrackModelRenderContext.isRenderingCustomChain());

        boolean tankTread = type == BntCogwheelChainTypes.TANK_TREAD_CHAIN.get();
        int quarters = isCustomBeltPlacement ? TrackModelRenderContext.chainQuarters() : BntCogwheelWidth.SINGLE;
        boolean wideBelt = quarters > BntCogwheelWidth.SINGLE;
        ResourceLocation renderTexture = type.getRenderTexture();
        boolean ownTexture = isCustomBeltItem;
        if (tankTread) {
            renderTexture = wideBelt ? BntChainTextures.TANK_TREAD_WIDE : BntChainTextures.TANK_TREAD;
        } else if (isCustomBeltPlacement) {
            ownTexture = true;
            if (industrialBelt) {
                renderTexture = wideBelt ? BntChainTextures.INDUSTRIAL_BELT_WIDE : BntChainTextures.INDUSTRIAL_BELT;
            } else {
                renderTexture = wideBelt ? BntChainTextures.TRACK_BELT_WIDE : BntChainTextures.TRACK_BELT;
            }
        } else if (TrackModelRenderContext.isRenderingTrack() && !industrialBelt) {
            ownTexture = true;
            renderTexture = BntChainTextures.TRACK_BELT;
        }

        float linkSquish = isCustomBeltPlacement ? BntBeltLinks.squish() : textureSquish;
        double[] loop = isCustomBeltPlacement
            ? BntBeltLinks.onLoop(to, offset, length, BntBeltLinks.repeatLength())
            : new double[]{BntBeltLinks.wrapScroll(offset), length};
        float scrolled = (float)loop[0];
        boolean invertScroll = bnt$shouldInvertScroll(type, chainRenderInfo, isCustomBeltPlacement);
        float actualOffset = invertScroll ? scrolled : -scrolled;
        float minV = isCustomBeltPlacement ? (actualOffset - (float)loop[1]) * linkSquish : actualOffset * linkSquish;
        float maxV = (float)loop[1] * linkSquish + minV;
        List<Vec3> scaledSourcePoints = sourcePoints;
        List<Vec3> scaledDestinationPoints = destinationPoints;
        if (isCustomBeltPlacement) {
            double widthScale = BntBeltFaces.widthScale(quarters);
            scaledSourcePoints = bnt$scaleChainWidth(sourcePoints, widthScale);
            scaledDestinationPoints = bnt$scaleChainWidth(destinationPoints, widthScale);
        }
        boolean bntBelt = chainRenderInfo == ChainRenderInfo.BELT && ownTexture;
        boolean wrapped = bntBelt && TrackModelRenderContext.isWrapped();

        BntTrackSink sink = BntTrackSink.current();
        if (sink != null) {
            sink.at(renderTexture, from);
            if (tankTread) {
                double[] tread = BntBeltLinks.onLoop(to, offset, length, BntTankTread.CELL);
                BntTankTread.Target target = new BntTankTread.Target(null, null, null, lightAtSource, lightAtDest, relTo, null, sink.links(), sink);
                BntTankTread.emitSegment(target, sourcePoints, destinationPoints, tread[0], tread[1], quarters, flipInsideOutside);
            } else {
                Object shape = bntBelt
                    ? new BntTrackSink.Belt(flipInsideOutside, quarters)
                    : new BntTrackSink.Generic(chainRenderInfo, flipInsideOutside);
                float scrollV = (float)((invertScroll ? linkSquish : -linkSquish) * sink.scrollRate());
                sink.span(shape, scaledSourcePoints, scaledDestinationPoints, minV, maxV, scrollV, lightAtSource, lightAtDest);
                if (wrapped) {
                    sink.at(BntChainTextures.WRAP_CHAIN, from);
                    double cell = BntBeltLinks.repeatLength();
                    double[] chains = BntBeltLinks.onLoop(to, offset, length, cell);
                    BntTankTread.Target target = new BntTankTread.Target(null, null, null, lightAtSource, lightAtDest, relTo, null, true, sink);
                    BntTankTread.emitChains(target, sourcePoints, destinationPoints, chains[0], chains[1], cell, quarters, flipInsideOutside);
                }
            }
            ms.popPose();
            ci.cancel();
            return;
        }

        VertexConsumer vc = buffer.getBuffer(RenderTypes.chain(renderTexture));
        Matrix4f poseMatrix = ms.last().pose();
        Pose pose = ms.last();
        double segLenSq = relTo.lengthSqr();
        VertexEmitter emitter = (x, y, z, u, v, nx, ny, nz) -> {
            float t = segLenSq > 1.0E-8 ? Mth.clamp((float)((x * relTo.x + y * relTo.y + z * relTo.z) / segLenSq), 0.0F, 1.0F) : 0.0F;
            int vertexLight = bnt$lerpPackedLight(lightAtSource, lightAtDest, t);
            vc.addVertex(poseMatrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(vertexLight)
                .setNormal(pose, nx, ny, nz);
        };

        if (tankTread) {
            double[] tread = BntBeltLinks.onLoop(to, offset, length, BntTankTread.CELL);
            BntTankTread.Target target = new BntTankTread.Target(
                vc, poseMatrix, pose.normal(), lightAtSource, lightAtDest, relTo,
                BntTreadView.eyeFrom(from), BntTreadView.links(from), null
            );
            BntTankTread.emitSegment(target, sourcePoints, destinationPoints, tread[0], tread[1], quarters, flipInsideOutside);
        } else if (bntBelt) {
            BntBeltFaces.emit(emitter, scaledSourcePoints, scaledDestinationPoints, flipInsideOutside, quarters, minV, maxV);
            if (wrapped) {
                double cell = BntBeltLinks.repeatLength();
                double[] chains = BntBeltLinks.onLoop(to, offset, length, cell);
                BntTankTread.Target target = new BntTankTread.Target(
                    buffer.getBuffer(RenderTypes.chain(BntChainTextures.WRAP_CHAIN)), poseMatrix, pose.normal(),
                    lightAtSource, lightAtDest, relTo, BntTreadView.eyeFrom(from), true, null
                );
                BntTankTread.emitChains(target, sourcePoints, destinationPoints, chains[0], chains[1], cell, quarters, flipInsideOutside);
            }
        } else {
            ChainQuadBuilder.buildSegmentFaces(scaledDestinationPoints, scaledSourcePoints, chainRenderInfo, minV, maxV, flipInsideOutside, emitter, true);
        }

        ms.popPose();
        ci.cancel();
    }

    @Unique
    private static List<Vec3> bnt$endPoints(
        Vec3 before, Vec3 point, Vec3 after, ChainRenderInfo chainRenderInfo, Vec3 cogwheelAxis, Matrix3f accumulatedOrientation
    ) {
        float radius = (float)((chainRenderInfo.getVertexShape() == VertexShape.CROSS ? Math.sqrt(2.0) / 2.0 : 1.0) * 1.0 / 16.0);
        Vec3 dirToBefore = point.subtract(before).normalize();
        Vec3 dirToAfter = after.subtract(point).normalize();
        Vec3 averagedDir = dirToBefore.add(dirToAfter).normalize();
        if (averagedDir.dot(cogwheelAxis) > 1.0E-4) {
            averagedDir = averagedDir.subtract(cogwheelAxis.multiply(averagedDir)).normalize();
        }
        if (averagedDir.lengthSqr() < 1.0E-4) {
            averagedDir = cogwheelAxis.cross(new Vec3(1.0, 0.0, 0.0));
        }
        if (averagedDir.lengthSqr() < 1.0E-4) {
            averagedDir = cogwheelAxis.cross(new Vec3(0.0, 1.0, 0.0));
        }

        Vec3 perpendicular = cogwheelAxis.cross(averagedDir);
        Matrix3f transform = new Matrix3f(
            (float)perpendicular.x, (float)perpendicular.y, (float)perpendicular.z,
            (float)cogwheelAxis.x, (float)cogwheelAxis.y, (float)cogwheelAxis.z,
            (float)averagedDir.x, (float)averagedDir.y, (float)averagedDir.z
        ).mul(accumulatedOrientation);
        Vector3f axis1 = transform.transform(1.0F, 0.0F, 0.0F, new Vector3f());
        Vec3 localAxis1Direction = new Vec3(axis1.x, axis1.y, axis1.z).normalize();
        Vec3 localAxis1 = localAxis1Direction.scale((float)chainRenderInfo.getHeight() / 2.0F);
        Vector3f axis2 = transform.transform(0.0F, 1.0F, 0.0F, new Vector3f());
        Vec3 localAxis2 = new Vec3(axis2.x, axis2.y, axis2.z).normalize().scale((float)chainRenderInfo.getWidth() / 2.0F);
        Vec3[] corners = {
            point.add(localAxis1.add(localAxis2).scale(radius)),
            point.add(localAxis1.subtract(localAxis2).scale(radius)),
            point.add(localAxis2.scale(-1.0).subtract(localAxis1).scale(radius)),
            point.add(localAxis2.subtract(localAxis1).scale(radius))
        };
        if (chainRenderInfo.getHeight() < 3) {
            Vec3 lift = localAxis1Direction.scale((3.0F - (float)chainRenderInfo.getHeight()) / 96.0F);
            for (int i = 0; i < corners.length; i++) {
                corners[i] = corners[i].add(lift);
            }
        }
        return Arrays.asList(corners);
    }

    @Unique
    private static List<Vec3> bnt$closestOrder(List<Vec3> destination, List<Vec3> source) {
        if (destination.size() != 4 || source.size() != 4) {
            return new ArrayList<>(destination);
        }

        Vec3[] d = destination.toArray(new Vec3[4]);
        Vec3[] s = source.toArray(new Vec3[4]);
        double[] distance = new double[16];
        double[] facing = new double[16];
        for (int i = 0; i < 4; i++) {
            Vec3 sourceEdge = s[i + 1 & 3].subtract(s[i]).normalize();
            for (int k = 0; k < 4; k++) {
                distance[i * 4 + k] = s[i].distanceToSqr(d[k]);
                facing[i * 4 + k] = sourceEdge.dot(d[k + 1 & 3].subtract(d[k]).normalize());
            }
        }

        double bestScore = Double.POSITIVE_INFINITY;
        int bestReversed = 0;
        int bestShift = 0;
        for (int reversed = 0; reversed <= 1; reversed++) {
            for (int shift = 0; shift < 4; shift++) {
                double pointScore = 0.0;
                for (int i = 0; i < 4; i++) {
                    pointScore += distance[i * 4 + (reversed == 0 ? i + shift & 3 : shift - i + 4 & 3)];
                }
                double edgeScore = 0.0;
                for (int i = 0; i < 4; i++) {
                    edgeScore += reversed == 0 ? 1.0 - facing[i * 4 + (i + shift & 3)] : 1.0 + facing[i * 4 + (shift - i + 3 & 3)];
                }
                double score = pointScore + edgeScore * 0.25 + (reversed == 1 ? 1.0E-4 : 0.0);
                if (score < bestScore) {
                    bestScore = score;
                    bestReversed = reversed;
                    bestShift = shift;
                }
            }
        }

        Vec3[] ordered = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            ordered[i] = d[bestReversed == 0 ? i + bestShift & 3 : bestShift - i + 4 & 3];
        }
        return Arrays.asList(ordered);
    }

    @Unique
    private static int bnt$lerpPackedLight(int light1, int light2, float t) {
        int block = (int)Mth.lerp(t, light1 & 65535, light2 & 65535);
        int sky = (int)Mth.lerp(t, light1 >> 16 & 65535, light2 >> 16 & 65535);
        return block | sky << 16;
    }

    @Unique
    private static boolean bnt$shouldInvertScroll(CogwheelChainType type, ChainRenderInfo chainRenderInfo, boolean isCustomBeltPlacement) {
        if (isCustomBeltPlacement) {
            return false;
        } else if (chainRenderInfo == ChainRenderInfo.CHAIN) {
            return true;
        } else {
            ResourceLocation texture = type.getRenderTexture();
            return chainRenderInfo == ChainRenderInfo.BELT && "bits_n_bobs".equals(texture.getNamespace());
        }
    }

    @Unique
    private static List<Vec3> bnt$scaleChainWidth(List<Vec3> pts, double scale) {
        if (pts.size() != 4) {
            return pts;
        } else {
            double extra = (scale - 1.0) / 2.0;
            Vec3 p0 = pts.get(0);
            Vec3 p1 = pts.get(1);
            Vec3 p2 = pts.get(2);
            Vec3 p3 = pts.get(3);
            Vec3 w01 = p0.subtract(p1);
            Vec3 w32 = p3.subtract(p2);
            Vec3 np0 = p0.add(w01.scale(extra));
            Vec3 np1 = p1.subtract(w01.scale(extra));
            Vec3 np2 = p2.subtract(w32.scale(extra));
            Vec3 np3 = p3.add(w32.scale(extra));
            return Arrays.asList(np0, np1, np2, np3);
        }
    }

}
