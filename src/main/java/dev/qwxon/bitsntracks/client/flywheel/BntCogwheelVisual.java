package dev.qwxon.bitsntracks.client.flywheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.BlockMaterialFunction;
import dev.engine_room.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.client.BntClientConfig;
import dev.qwxon.bitsntracks.content.BntVisualized;
import dev.qwxon.bitsntracks.content.HiddenCogwheelCompat;
import dev.qwxon.bitsntracks.content.HiddenCogwheelRenderer;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.index.BitsNTracksBlocks;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BntCogwheelVisual extends KineticBlockEntityVisual<KineticBlockEntity> {
    private static final int STRESS_TICKS = 20;
    private static final double SWITCH_MARGIN = 1.0;
    private static final AABB PIECE_BOUNDS = new AABB(-1.0, -1.0, -1.0, 2.0, 2.0, 2.0);
    private static final BlockMaterialFunction CUTOUT = (type, shaded, ambientOcclusion) -> ModelUtil.getMaterial(RenderType.cutout(), shaded, ambientOcclusion);
    private static final RendererReloadCache<CogModel, Model> MODELS = new RendererReloadCache<>(BntCogwheelVisual::bake);

    private final BntTrackVisual track;
    private final BntPieceVisual pieces;
    private final boolean hidden;
    @Nullable
    private RotatingInstance cog;
    @Nullable
    private BlockState modelState;
    private volatile boolean dirty = true;
    private volatile boolean relight = true;
    private boolean deleted;
    private boolean drawing;
    private boolean resting;
    private double shiftX = Double.NaN;
    private double shiftY;
    private double shiftZ;
    private long stressTick = Long.MIN_VALUE;
    private int stressLeft;
    private int packedLight;

    public BntCogwheelVisual(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        this.track = new BntTrackVisual(context, blockEntity, getVisualPosition());
        this.pieces = new BntPieceVisual(context, blockEntity, getVisualPosition());
        this.hidden = HiddenCogwheelCompat.isHiddenCogwheel(blockEntity.getBlockState());
        BntFlywheel.add(this);
    }

    @Override
    public void update(float partialTick) {
        dirty = true;
        track.invalidate();
    }

    @Override
    public void updateLight(float partialTick) {
        relight = true;
        track.relight();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        RotatingInstance instance = cog;
        if (instance != null) {
            consumer.accept(instance);
        }
    }

    @Override
    protected synchronized void _delete() {
        deleted = true;
        BntFlywheel.remove(this);
        BntVisualized.mark(blockEntity, false);
        clearCog();
        track.delete();
        pieces.delete();
    }

    synchronized void fail() {
        deleted = true;
        BntVisualized.mark(blockEntity, false);
        clearCog();
        track.delete();
        pieces.delete();
    }

    synchronized void frame(float partialTick) {
        if (deleted) {
            return;
        }
        if (!BntClientConfig.isFlywheelRenderingEnabled()) {
            if (drawing) {
                drawing = false;
                BntVisualized.mark(blockEntity, false);
                clearCog();
                track.delete();
                pieces.delete();
                dirty = true;
            }
            return;
        }

        boolean far = far();
        if (far != resting) {
            resting = far;
            dirty = true;
        }
        HiddenCogwheelCompat.drawResting(resting);
        try {
            boolean fresh = dirty || relight;
            if (dirty) {
                dirty = false;
                refresh();
            }
            if (relight) {
                packedLight = LevelRenderer.getLightColor(level, pos);
            }
            if (cog != null) {
                drawCog(partialTick);
            }
            relight = false;
            if (hidden && !leverHidden() && BntSuspension.hasPiece(blockEntity)) {
                if (fresh || !pieces.showing() || !resting && BntFlywheel.visible(blockEntity, PIECE_BOUNDS)) {
                    pieces.frame(partialTick, packedLight);
                }
            } else {
                pieces.hide();
            }
            track.frame(partialTick, resting);
        } finally {
            HiddenCogwheelCompat.drawResting(false);
        }
        if (!drawing) {
            drawing = true;
            BntVisualized.mark(blockEntity, true);
        }
    }

    private void refresh() {
        BlockState state = modelState();
        if (state != modelState || cog == null && state != null) {
            clearCog();
            modelState = state;
            if (state != null) {
                cog = instancerProvider().instancer(AllInstanceTypes.ROTATING, MODELS.get(new CogModel(state, cutout(state)))).createInstance();
                relight = true;
            }
        }
        shiftX = Double.NaN;
        if (cog != null) {
            cog.setup(blockEntity).setChanged();
        }
    }

    private void drawCog(float partialTick) {
        RotatingInstance instance = cog;
        boolean changed = false;
        if (hidden && !resting || Double.isNaN(shiftX)) {
            Vec3 shift = HiddenCogwheelCompat.getModelTranslation(blockEntity, partialTick);
            if (shift.x != shiftX || shift.y != shiftY || shift.z != shiftZ) {
                shiftX = shift.x;
                shiftY = shift.y;
                shiftZ = shift.z;
                instance.setPosition(getVisualPosition()).nudge((float)shiftX, (float)shiftY, (float)shiftZ);
                changed = true;
            }
        }
        if (relight) {
            relight(new FlatLit[]{instance});
            packedLight = instance.light;
            changed = true;
        }
        if (changed) {
            instance.setChanged();
        }

        long tick = level.getGameTime();
        if (tick != stressTick) {
            stressTick = tick;
            if (blockEntity.isOverStressed()) {
                stressLeft = STRESS_TICKS;
            }
            if (stressLeft > 0) {
                stressLeft--;
                applyOverstressEffect(blockEntity, new RotatingInstance[]{instance});
            }
        }
    }

    private boolean far() {
        int distance = BntClientConfig.animationDistance();
        if (distance <= 0) {
            return false;
        }
        double edge = resting ? distance - SWITCH_MARGIN : distance + SWITCH_MARGIN;
        return BntFlywheel.vehicleDistanceSqr(blockEntity) > edge * edge;
    }

    private void clearCog() {
        if (cog != null) {
            cog.delete();
            cog = null;
        }
        modelState = null;
    }

    private boolean leverHidden() {
        return blockEntity instanceof KineticBlockEntityPhysicsAccess access && access.bnt$isHiddenByLever();
    }

    @Nullable
    private BlockState modelState() {
        if (leverHidden()) {
            return null;
        }
        return hidden ? HiddenCogwheelRenderer.modelState(blockEntity) : blockEntity.getBlockState();
    }

    private boolean cutout(BlockState state) {
        return hidden
            ? HiddenCogwheelRenderer.isIndustrial(state)
            : HiddenCogwheelRenderer.isIndustrial(state) && !state.is((Block)BitsNTracksBlocks.INDUSTRIAL_TINY_FLANGED_COGWHEEL.get());
    }

    private static Model bake(CogModel key) {
        BakedModelBuilder builder = new BakedModelBuilder(Minecraft.getInstance().getBlockRenderer().getBlockModel(key.state()))
            .level(SinglePosVirtualBlockGetter.createFullDark().blockState(key.state()));
        if (key.cutout()) {
            builder.materialFunc(CUTOUT);
        }
        return builder.build();
    }

    private record CogModel(BlockState state, boolean cutout) {
    }
}
