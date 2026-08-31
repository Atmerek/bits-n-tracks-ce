package dev.qwxon.bitsntracks.mixin;

import com.cake.azimuth.behaviour.SuperBlockEntityBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviourRenderer;
import com.kipti.bnb.content.kinetics.cogwheel_chain.block.EmptyFlangedGearBlock;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.ChainQuadBuilder.VertexEmitter;
import com.kipti.bnb.content.kinetics.cogwheel_chain.render.CogwheelChainRenderGeometryBuilder.ChainSegment;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType.ChainRenderInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.access.TrackModelBehaviourAccess;
import dev.qwxon.bitsntracks.client.BntClientCompat;
import dev.qwxon.bitsntracks.content.BntCogwheelPairing;
import dev.qwxon.bitsntracks.content.BntFlangedCogwheelBlock;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.TrackModelRenderContext;
import dev.qwxon.bitsntracks.physics.BntRadiusProvider;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
    @Redirect(
        method = {"renderChainSlowerButWithoutGaps"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/types/CogwheelChainType;getRenderTexture()Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private static ResourceLocation bnt$redirectRenderTextureSlow(CogwheelChainType instance) {
        return TrackModelRenderContext.isRenderingTrack() && !instance.getRenderTexture().getPath().contains("industrial")
            ? BitsNTracks.asResource("textures/block/belt.png")
            : instance.getRenderTexture();
    }

    @Redirect(
        method = {"renderChainFastButWithGaps"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/types/CogwheelChainType;getRenderTexture()Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private static ResourceLocation bnt$redirectRenderTextureFast(CogwheelChainType instance) {
        return TrackModelRenderContext.isRenderingTrack() && !instance.getRenderTexture().getPath().contains("industrial")
            ? BitsNTracks.asResource("textures/block/belt.png")
            : instance.getRenderTexture();
    }

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
        if (be.getBehaviour(CogwheelChainBehaviour.TYPE) instanceof TrackModelBehaviourAccess access) {
            TrackModelRenderContext.setRenderingTrack(access.bnt$isTrackModel());
        } else {
            TrackModelRenderContext.setRenderingTrack(false);
        }

        TrackModelRenderContext.setRenderingCustomChain(TrackModelRenderContext.isCustomOrIndustrialCogwheel(be));
        TrackModelRenderContext.setRenderingWideChain(BntCogwheelPairing.isWide(be.getBlockState()));
        TrackModelRenderContext.setRenderingLevel(HiddenCogwheelCompat.getActualLevel(be));

        try {
            original.call(behaviour, be, partialTicks, ms, buffer, light, overlay);
        } finally {
            TrackModelRenderContext.setRenderingTrack(false);
            TrackModelRenderContext.setRenderingCustomChain(false);
            TrackModelRenderContext.setRenderingWideChain(false);
            TrackModelRenderContext.setRenderingLevel(null);
        }
    }

    @Redirect(
        method = {"renderSafe"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/kipti/bnb/content/kinetics/cogwheel_chain/render/CogwheelChainRenderGeometryBuilder;buildSegments(Lcom/kipti/bnb/content/kinetics/cogwheel_chain/graph/CogwheelChain;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"
        )
    )
    private List<ChainSegment> bnt$transformChainSegments(CogwheelChain chain, Vec3 origin, @Local(argsOnly = true) KineticBlockEntity be) {
        List var5;
        try {
            BntRadiusProvider.setLevel(HiddenCogwheelCompat.getActualLevel(be));
            BntRadiusProvider.setOrigin(be.getBlockPos());
            List<ChainSegment> segments = CogwheelChainRenderGeometryBuilder.buildSegments(chain, origin);
            var5 = BntClientCompat.transformChainSegments(segments, chain, be);
        } finally {
            BntRadiusProvider.clearLevel();
        }

        return var5;
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
        int light = lighter.apply(vec);
        if (light == 0 && be != null && be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        }

        return light;
    }

    @Inject(
        method = {"renderChainSlowerButWithoutGaps", "renderChainFastButWithGaps"},
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
        CallbackInfo ci
    ) {
        ChainRenderInfo chainRenderInfo = type.getRenderType();
        Vec3 relPreFrom = preFrom.subtract(from);
        Vec3 relFrom = Vec3.ZERO;
        Vec3 relTo = to.subtract(from);
        Vec3 relPostTo = postTo.subtract(from);
        List<Vec3> destinationPoints = CogwheelChainRenderGeometryBuilder.getEndPointsForChainJoint(relFrom, relTo, relPostTo, chainRenderInfo, toCogwheelAxis);
        List<Vec3> sourcePoints = CogwheelChainRenderGeometryBuilder.getEndPointsForChainJoint(relPreFrom, relFrom, relTo, chainRenderInfo, fromCogwheelAxis);
        destinationPoints = CogwheelChainRenderGeometryBuilder.getPointsInClosestOrder(destinationPoints, sourcePoints);
        float length = (float)from.distanceTo(to);
        ms.pushPose();
        boolean isCustomBeltItem = type.getRenderTexture().getNamespace().equals("bits_n_tracks");
        boolean isCustomBeltPlacement = false;
        if (chainRenderInfo == ChainRenderInfo.BELT) {
            if (!isCustomBeltItem && !TrackModelRenderContext.isRenderingCustomChain()) {
                Level level = TrackModelRenderContext.getRenderingLevel();
                if (level == null) {
                    level = Minecraft.getInstance().level;
                }

                if (level != null) {
                    BlockPos posFrom = bnt$findCogwheelBlock(level, from, fromCogwheelAxis);
                    BlockPos posTo = bnt$findCogwheelBlock(level, to, toCogwheelAxis);
                    if (bnt$isCustomOrIndustrialCogwheel(level, posFrom) && bnt$isCustomOrIndustrialCogwheel(level, posTo)) {
                        isCustomBeltPlacement = true;
                    }
                }
            } else {
                isCustomBeltPlacement = true;
            }
        }

        boolean wideBelt = isCustomBeltPlacement && TrackModelRenderContext.isRenderingWideChain();
        ResourceLocation renderTexture = type.getRenderTexture();
        if (isCustomBeltPlacement) {
            if (renderTexture.getPath().contains("industrial")) {
                renderTexture = BitsNTracks.asResource(
                    wideBelt ? "textures/block/industrial_belt_wide.png" : "textures/block/industrial_belt.png"
                );
            } else {
                renderTexture = BitsNTracks.asResource(wideBelt ? "textures/block/track_belt_wide.png" : "textures/block/track_belt.png");
            }
        } else if (TrackModelRenderContext.isRenderingTrack() && !renderTexture.getPath().contains("industrial")) {
            renderTexture = BitsNTracks.asResource("textures/block/track_belt.png");
        }

        float actualOffset = bnt$shouldInvertScroll(type, chainRenderInfo, isCustomBeltPlacement) ? offset : -offset;
        float minV = actualOffset * textureSquish;
        float maxV = length * textureSquish + minV;
        VertexConsumer vc = buffer.getBuffer(RenderTypes.chain(renderTexture));
        Matrix4f poseMatrix = ms.last().pose();
        Pose pose = ms.last();
        double segLenSq = relTo.lengthSqr();
        VertexEmitter emitter = (x, y, z, u, v, nx, ny, nz) -> {
            float t = segLenSq > 1.0E-8 ? Mth.clamp((float)(new Vec3(x, y, z).dot(relTo) / segLenSq), 0.0F, 1.0F) : 0.0F;
            int vertexLight = bnt$lerpPackedLight(lightAtSource, lightAtDest, t);
            vc.addVertex(poseMatrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(vertexLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        };
        List<Vec3> scaledSourcePoints = sourcePoints;
        List<Vec3> scaledDestinationPoints = destinationPoints;
        if (isCustomBeltPlacement) {
            double widthScale = wideBelt ? 9.333333333333334 : 4.666666666666667;
            scaledSourcePoints = bnt$scaleChainWidth(sourcePoints, widthScale);
            scaledDestinationPoints = bnt$scaleChainWidth(destinationPoints, widthScale);
        }

        if (chainRenderInfo == ChainRenderInfo.BELT && renderTexture.getNamespace().equals("bits_n_tracks")) {
            float u0Top = 0.875F;
            float u1Top = 0.4375F;
            float u0Bot = 0.4375F;
            float u1Bot = 0.0F;
            if (flipInsideOutside) {
                float temp0 = u0Top;
                float temp1 = u1Top;
                u0Top = u0Bot;
                u1Top = u1Bot;
                u0Bot = temp0;
                u1Bot = temp1;
            }

            float uEdge = wideBelt ? 0.90625F : 0.9375F;
            float uLeftFace0 = 0.875F;
            float uLeftFace1 = uEdge;
            float uRightFace0 = 0.875F;
            float uRightFace1 = uEdge;
            if (flipInsideOutside) {
                float temp = uLeftFace0;
                uLeftFace0 = uLeftFace1;
                uLeftFace1 = temp;
                temp = uRightFace0;
                uRightFace0 = uRightFace1;
                uRightFace1 = temp;
            }

            float oppositeMinVSide = -actualOffset * textureSquish;
            float oppositeMaxVSide = length * textureSquish + oppositeMinVSide;
            Vec3 posTL = scaledDestinationPoints.get(1);
            Vec3 posBL = scaledSourcePoints.get(1);
            Vec3 posBR = scaledSourcePoints.get(0);
            Vec3 posTR = scaledDestinationPoints.get(0);
            bnt$emitQuad(emitter, posTL, posBL, posBR, posTR, u0Top, u1Top, maxV, minV);
            posTL = scaledDestinationPoints.get(2);
            posBL = scaledSourcePoints.get(2);
            posBR = scaledSourcePoints.get(1);
            posTR = scaledDestinationPoints.get(1);
            bnt$emitQuad(emitter, posTL, posBL, posBR, posTR, uLeftFace1, uLeftFace0, oppositeMinVSide, oppositeMaxVSide);
            posTL = scaledDestinationPoints.get(3);
            posBL = scaledSourcePoints.get(3);
            posBR = scaledSourcePoints.get(2);
            posTR = scaledDestinationPoints.get(2);
            bnt$emitQuad(emitter, posTL, posBL, posBR, posTR, u0Bot, u1Bot, maxV, minV);
            posTL = scaledDestinationPoints.get(0);
            posBL = scaledSourcePoints.get(0);
            posBR = scaledSourcePoints.get(3);
            posTR = scaledDestinationPoints.get(3);
            bnt$emitQuad(emitter, posTL, posBL, posBR, posTR, uRightFace0, uRightFace1, oppositeMinVSide, oppositeMaxVSide);
        } else {
            ChainQuadBuilder.buildSegmentFaces(scaledDestinationPoints, scaledSourcePoints, chainRenderInfo, minV, maxV, flipInsideOutside, emitter, true);
        }

        ms.popPose();
        ci.cancel();
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
    private static void bnt$emitQuad(VertexEmitter emitter, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, float u0, float u1, float v0, float v1) {
        emitter.emit((float)p1.x, (float)p1.y, (float)p1.z, u0, v0, 0.0F, 1.0F, 0.0F);
        emitter.emit((float)p2.x, (float)p2.y, (float)p2.z, u0, v1, 0.0F, 1.0F, 0.0F);
        emitter.emit((float)p3.x, (float)p3.y, (float)p3.z, u1, v1, 0.0F, 1.0F, 0.0F);
        emitter.emit((float)p4.x, (float)p4.y, (float)p4.z, u1, v0, 0.0F, 1.0F, 0.0F);
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

    @Unique
    private static BlockPos bnt$findCogwheelBlock(Level level, Vec3 pos, Vec3 axis) {
        double ax = Math.abs(axis.x);
        double ay = Math.abs(axis.y);
        double az = Math.abs(axis.z);
        int centerX = Mth.floor(pos.x);
        int centerY = Mth.floor(pos.y);
        int centerZ = Mth.floor(pos.z);
        int rx = ax > 0.5 ? 1 : 2;
        int ry = ay > 0.5 ? 1 : 2;
        int rz = az > 0.5 ? 1 : 2;
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos p = new BlockPos(centerX + dx, centerY + dy, centerZ + dz);
                    BlockState state = level.getBlockState(p);
                    Block block = state.getBlock();
                    if (block instanceof BntFlangedCogwheelBlock || HiddenCogwheelCompat.isHiddenCogwheel(state) || block instanceof EmptyFlangedGearBlock) {
                        double distSq = pos.distanceToSqr(Vec3.atCenterOf(p));
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = p;
                        }
                    }
                }
            }
        }

        return bestPos != null ? bestPos : BlockPos.containing(pos);
    }

    @Unique
    private static boolean bnt$isCustomOrIndustrialCogwheel(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BntFlangedCogwheelBlock) {
            return true;
        } else {
            if (HiddenCogwheelCompat.isHiddenCogwheel(state) && level.getBlockEntity(pos) instanceof KineticBlockEntityPhysicsAccess access) {
                String originalBlock = access.bnt$getOriginalBlock();
                if (originalBlock != null) {
                    return HiddenCogwheelCompat.isBitsNTracksId(originalBlock);
                }
            }

            return false;
        }
    }
}
