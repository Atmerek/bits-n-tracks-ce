package dev.qwxon.bitsntracks.client;

import com.simibubi.create.AllKeys;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.interaction.BntToolMessages;
import dev.qwxon.bitsntracks.interaction.WrenchPhysicsHandler;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BntToolHud {
    private static final int HOTBAR_HALF = 91;
    private static final int SLOT = 29;
    private static final int INDICATOR = 24;
    private static final int GAP = 6;
    private static final int BORDER = 4;
    private static final int MIN_WIDTH = 96;
    private static final int HEADER = 12;
    private static final int LINE = 10;
    private static final int INDENT = 6;
    private static final int COLUMN = 8;
    private static final int BAR = 30;
    private static final int STEP_WIDTH = 2;
    private static final int STEP_SPACING = 3;
    private static final int DOT = 3;
    private static final int DOT_SPACING = 5;
    private static final int ABOVE_STATUS_BARS = 62;

    private static final int BACKGROUND_TOP = 0xE0151719;
    private static final int BACKGROUND_BOTTOM = 0xE00C0D0E;
    private static final int BORDER_TOP = 0xA08A9096;
    private static final int BORDER_BOTTOM = 0x804A4E53;
    private static final int BORDER_FOCUSED_TOP = 0xF0D4DADF;
    private static final int BORDER_FOCUSED_BOTTOM = 0xC0868C92;
    private static final int RULE = 0x30FFFFFF;
    private static final int TRACK = 0x50000000;

    private static final int ACCENT = BntToolVisuals.opaque(BntToolVisuals.SELECTED);
    private static final int TEXT = 0xFFC8CCD0;
    private static final int LABEL = 0xFF969BA0;
    private static final int DIM = 0xFF64686C;
    private static final int HINT = 0xFF8C949C;
    private static final int METER = 0xFF7C8186;
    private static final int GOOD = BntToolVisuals.opaque(BntToolVisuals.GOOD);
    private static final int WARNING = BntToolVisuals.opaque(BntToolVisuals.WARNING);


    private BntToolHud() {
    }

    private enum Meter {
        NONE,
        BAR,
        STEPS
    }

    private record Row(Component label, @Nullable Component value, int colour, Meter meter, float fill, int steps, boolean marked) {
        static Row text(Component label, int colour) {
            return new Row(label, null, colour, Meter.NONE, 0.0F, 0, false);
        }

        static Row value(Component label, Component value, int colour, boolean marked) {
            return new Row(label, value, colour, Meter.NONE, 0.0F, 0, marked);
        }

        Row bar(float fill) {
            return new Row(label, value, colour, Meter.BAR, fill, 0, marked);
        }

        Row steps(int filled, int total) {
            return new Row(label, value, colour, Meter.STEPS, filled, total, marked);
        }

        int meterWidth() {
            return switch (meter) {
                case NONE -> 0;
                case BAR -> BAR;
                case STEPS -> steps * STEP_SPACING - (STEP_SPACING - STEP_WIDTH);
            };
        }
    }

    private record Panel(Component mode, int index, int count, @Nullable Component scope, List<Row> rows, Component hint, int hintColour) {
    }

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, BitsNTracks.asResource("tool_panel"), BntToolHud::render);
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (minecraft.options.hideGui || player == null || level == null || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean focused = BntToolInput.menuKeyDown();
        Panel panel = stack.getItem() instanceof CogAlignmentLeverItem
            ? leverPanel(level, stack, focused)
            : stack.getItem() instanceof SuspensionToolItem ? tunerPanel(level, stack, focused) : null;
        if (panel != null) {
            draw(graphics, minecraft, player, panel, focused);
        }
    }

    private static Panel leverPanel(ClientLevel level, ItemStack stack, boolean focused) {
        BntLeverMode mode = CogAlignmentLeverItem.mode(stack);
        List<Row> rows = new ArrayList<>();
        BlockPos aimed = BntToolInput.aimedCog(level);
        String hint = null;
        int hintColour = HINT;
        switch (mode) {
            case OFFSET -> {
                BlockPos selected = BntToolState.leverCog;
                BlockPos shown = selected != null ? selected : aimed;
                Axis blockAxis = shown == null ? null : BntToolState.blockAxis(level, shown);
                if (blockAxis == null || !(level.getBlockEntity(shown) instanceof KineticBlockEntityPhysicsAccess access)) {
                    rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.cogwheel"), DIM));
                } else {
                    Axis[] axes = BntToolState.axes(blockAxis);
                    int current = selected == null ? -1 : BntToolState.axisRow(level, selected);
                    for (int i = 0; i < axes.length; i++) {
                        boolean allowed = BntToolState.axisAllowed(level, shown, blockAxis, axes[i]);
                        int colour = !allowed ? DIM : i == current ? ACCENT : selected == null ? LABEL : TEXT;
                        rows.add(Row.value(Component.translatable(BntToolState.axisKey(blockAxis, axes[i])), pixels(offset(access, axes[i])), colour, i == current));
                    }
                    hint = selected != null ? "bits_n_tracks.tool_panel.hint.offset" : "bits_n_tracks.tool_panel.hint.select";
                }
            }
            case ROUTE -> {
                if (aimed != null && level.getBlockEntity(aimed) instanceof KineticBlockEntityPhysicsAccess access) {
                    int side = access.bnt$getTrackRouteSide();
                    Component route = side < 0 || side >= Direction.values().length
                        ? Component.translatable("bits_n_tracks.tool_panel.route.automatic")
                        : Component.translatable("bits_n_tracks.tool_panel.route.wrapped",
                            Component.translatable("bits_n_tracks.tool_panel.side." + Direction.values()[side].getOpposite().getName()));
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.routing"), route, TEXT, false));
                    hint = "bits_n_tracks.tool_panel.hint.route";
                } else {
                    rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.cogwheel"), DIM));
                }
            }
            case TENSION -> {
                BlockPos chain = BntToolState.leverChain != null ? BntToolState.leverChain : BntToolInput.aimedChain(level, aimed);
                if (chain != null) {
                    float tension = BntBeltTension.at(level, chain);
                    Component value = tension >= BntBeltTension.MAX
                        ? Component.translatable("bits_n_tracks.tool_panel.tension.taut")
                        : tension <= BntBeltTension.MIN
                            ? Component.translatable("bits_n_tracks.tool_panel.tension.slack")
                            : Component.literal(Math.round(tension * 100.0F) + "%");
                    boolean selected = chain.equals(BntToolState.leverChain);
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.tension"), value, selected ? ACCENT : TEXT, selected).bar(tension));
                    hint = selected ? "bits_n_tracks.tool_panel.hint.tension" : "bits_n_tracks.tool_panel.hint.select";
                } else {
                    rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.track"), DIM));
                }
            }
            case VISIBILITY -> {
                if (aimed != null && level.getBlockEntity(aimed) instanceof KineticBlockEntityPhysicsAccess access) {
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.model"), Component.translatable(access.bnt$isHiddenByLever()
                        ? "bits_n_tracks.tool_panel.model.hidden"
                        : "bits_n_tracks.tool_panel.model.shown"), TEXT, false));
                    hint = "bits_n_tracks.tool_panel.hint.visibility";
                } else {
                    rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.cogwheel"), DIM));
                }
            }
            case PHYSICS -> {
                BlockPos target = BntToolInput.aimedBlock(level);
                if (target != null && WrenchPhysicsHandler.canTogglePhysics(level, target)
                    && level.getBlockEntity(target) instanceof KineticBlockEntityPhysicsAccess access) {
                    boolean enabled = access.bnt$isPhysicsEnabled();
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.physics"),
                        Component.translatable(enabled ? "bits_n_tracks.tool_panel.on" : "bits_n_tracks.tool_panel.off"), enabled ? GOOD : TEXT, false));
                    hint = "bits_n_tracks.tool_panel.hint.physics";
                } else {
                    rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.cogwheel"), DIM));
                }
            }
            case RESET -> {
                rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.row.reset"), aimed == null ? DIM : TEXT));
                if (aimed != null && BntToolState.resetArmedFor(aimed)) {
                    hint = "bits_n_tracks.tool_panel.hint.reset.confirm";
                    hintColour = WARNING;
                } else if (aimed != null) {
                    hint = "bits_n_tracks.tool_panel.hint.reset";
                }
            }
        }
        Component scope = mode == BntLeverMode.OFFSET ? CogAlignmentLeverItem.scopeName(CogAlignmentLeverItem.wholeTrack(stack)) : null;
        return panel(Component.translatable(mode.translationKey()), mode.ordinal(), BntLeverMode.values().length, scope, rows, hint, hintColour, focused);
    }

    private static Panel tunerPanel(ClientLevel level, ItemStack stack, boolean focused) {
        BntTuning setting = SuspensionToolItem.setting(stack);
        List<Row> rows = new ArrayList<>();
        BlockPos selected = BntToolState.tunerCog;
        BlockPos shown = selected;
        if (shown == null) {
            BlockPos aimed = BntToolInput.aimedBlock(level);
            shown = aimed != null && BntSuspension.hasPiece(level.getBlockEntity(aimed)) ? aimed : null;
        }
        String hint = null;
        if (shown == null) {
            rows.add(Row.text(Component.translatable("bits_n_tracks.tool_panel.none.piece"), DIM));
        } else {
            for (BntTuning each : BntTuning.values()) {
                int value = each.levelOf(level.getBlockEntity(shown));
                boolean current = each == setting;
                int colour = current ? selected != null ? ACCENT : TEXT : LABEL;
                rows.add(Row.value(Component.translatable(each.translationKey()),
                    Component.translatable("bits_n_tracks.tool_panel.level", value, BntTuning.MAX), colour, current && selected != null)
                    .steps(value, BntTuning.MAX));
            }
            hint = selected != null ? "bits_n_tracks.tool_panel.hint.tuning" : "bits_n_tracks.tool_panel.hint.select";
        }
        Component scope = SuspensionToolItem.scopeName(SuspensionToolItem.wholeTrack(stack));
        return panel(Component.translatable(setting.translationKey()), setting.ordinal(), BntTuning.values().length, scope, rows, hint, HINT, focused);
    }

    private static Panel panel(Component mode, int index, int count, @Nullable Component scope, List<Row> rows, @Nullable String hint, int hintColour, boolean focused) {
        Component message = BntToolMessages.current();
        Component footer;
        int colour = hintColour;
        if (message != null) {
            footer = message;
            colour = TEXT;
        } else if (focused) {
            footer = Component.translatable("bits_n_tracks.tool_panel.modes.focused");
            colour = ACCENT;
        } else if (hint != null) {
            footer = Component.translatable(hint);
        } else {
            footer = Component.translatable("bits_n_tracks.tool_panel.modes", AllKeys.TOOL_MENU.getBoundKey());
            colour = DIM;
        }
        return new Panel(mode, index, count, scope, rows, footer, colour);
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, Panel panel, boolean focused) {
        Font font = minecraft.font;
        Component mode = focused ? Component.literal("< ").append(panel.mode()).append(" >") : panel.mode();
        int dots = panel.count() * DOT_SPACING - (DOT_SPACING - DOT);
        int header = font.width(mode) + GAP + dots + (panel.scope() == null ? 0 : COLUMN + font.width(panel.scope()));
        int labels = 0;
        int values = 0;
        int meters = 0;
        int text = 0;
        for (Row row : panel.rows()) {
            if (row.value() == null) {
                text = Math.max(text, INDENT + font.width(row.label()));
                continue;
            }
            labels = Math.max(labels, font.width(row.label()));
            values = Math.max(values, font.width(row.value()));
            meters = Math.max(meters, row.meterWidth());
        }
        int body = Math.max(text, INDENT + labels + (values > 0 ? COLUMN + values : 0) + (meters > 0 ? GAP + meters : 0));
        int width = Math.max(MIN_WIDTH, Math.max(header, Math.max(body, font.width(panel.hint()))));
        int height = HEADER + 3 + LINE * panel.rows().size() + 3 + LINE - 1;

        int centre = graphics.guiWidth() / 2;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        boolean indicator = minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
        int right = centre + HOTBAR_HALF + GAP + BORDER + (leftHanded ? SLOT : 0) + (indicator && !leftHanded ? INDICATOR : 0);
        int left = centre - HOTBAR_HALF - GAP - BORDER - (leftHanded ? 0 : SLOT) - (indicator && leftHanded ? INDICATOR : 0) - width;
        int x;
        int y;
        if (right + width + BORDER <= graphics.guiWidth() - 2) {
            x = right;
            y = graphics.guiHeight() - BORDER - 1 - height;
        } else if (left - BORDER >= 2) {
            x = left;
            y = graphics.guiHeight() - BORDER - 1 - height;
        } else {
            x = centre - width / 2;
            y = graphics.guiHeight() - ABOVE_STATUS_BARS - height;
        }

        graphics.pose().pushPose();
        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, 0, BACKGROUND_TOP, BACKGROUND_BOTTOM,
            focused ? BORDER_FOCUSED_TOP : BORDER_TOP, focused ? BORDER_FOCUSED_BOTTOM : BORDER_BOTTOM);
        graphics.pose().translate(0.0F, 0.0F, 400.0F);

        graphics.drawString(font, mode, x, y + 1, ACCENT, false);
        int dotX = x + font.width(mode) + GAP;
        for (int i = 0; i < panel.count(); i++) {
            int dx = dotX + i * DOT_SPACING;
            graphics.fill(dx, y + 3, dx + DOT, y + 3 + DOT, i == panel.index() ? ACCENT : DIM);
        }
        if (panel.scope() != null) {
            graphics.drawString(font, panel.scope(), x + width - font.width(panel.scope()), y + 1, LABEL, false);
        }
        int line = y + HEADER;
        graphics.fill(x, line, x + width, line + 1, RULE);
        line += 3;

        int valueX = x + INDENT + labels + COLUMN;
        int meterX = x + width - meters;
        for (Row row : panel.rows()) {
            if (row.marked()) {
                graphics.fill(x, line - 1, x + 2, line + 8, ACCENT);
            }
            graphics.drawString(font, row.label(), x + INDENT, line, row.value() == null ? row.colour() : row.marked() ? ACCENT : LABEL, false);
            if (row.value() != null) {
                int valueRight = meters > 0 ? meterX - GAP : x + width;
                graphics.drawString(font, row.value(), Math.max(valueX, valueRight - font.width(row.value())), line, row.colour(), false);
            }
            int fillColour = row.marked() ? ACCENT : METER;
            if (row.meter() == Meter.BAR) {
                graphics.fill(meterX, line + 2, meterX + BAR, line + 6, TRACK);
                graphics.fill(meterX, line + 2, meterX + Math.round(BAR * Math.max(0.0F, Math.min(1.0F, row.fill()))), line + 6, fillColour);
            } else if (row.meter() == Meter.STEPS) {
                for (int i = 0; i < row.steps(); i++) {
                    int sx = meterX + i * STEP_SPACING;
                    graphics.fill(sx, line + 1, sx + STEP_WIDTH, line + 7, i < row.fill() ? fillColour : TRACK);
                }
            }
            line += LINE;
        }

        line += 1;
        graphics.fill(x, line, x + width, line + 1, RULE);
        line += 3;
        graphics.drawString(font, panel.hint(), x, line, panel.hintColour(), false);
        graphics.pose().popPose();
    }


    private static float offset(KineticBlockEntityPhysicsAccess access, Axis axis) {
        return switch (axis) {
            case X -> access.bnt$getAlignmentOffsetX();
            case Y -> access.bnt$getAlignmentOffsetY();
            case Z -> access.bnt$getAlignmentOffsetZ();
        };
    }

    private static Component pixels(float offset) {
        int px = Math.round(offset / CogAlignmentLeverItem.STEP);
        return Component.translatable("bits_n_tracks.tool_panel.pixels", (px > 0 ? "+" : "") + px);
    }
}
