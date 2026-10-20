package dev.qwxon.bitsntracks.client;

import com.simibubi.create.AllKeys;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.access.KineticBlockEntityPhysicsAccess;
import dev.qwxon.bitsntracks.content.BntCogwheelWidth;
import dev.qwxon.bitsntracks.content.BntLeverMode;
import dev.qwxon.bitsntracks.content.CogAlignmentLeverItem;
import dev.qwxon.bitsntracks.content.SuspensionToolItem;
import dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain.BntBeltTension;
import dev.qwxon.bitsntracks.content.suspension.BntSuspension;
import dev.qwxon.bitsntracks.interaction.BntCogwheelInteraction;
import dev.qwxon.bitsntracks.interaction.BntToolMessages;
import dev.qwxon.bitsntracks.physics.BntTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int MIN_WIDTH = 90;
    private static final int HEADER = 12;
    private static final int LINE = 10;
    private static final int CONTROL_LINE = 12;
    private static final int CONTROL_GAP = 8;
    private static final int PACK_WIDTH = 136;
    private static final int INDENT = 4;
    private static final int COLUMN = 6;
    private static final int BAR = 24;
    private static final int DOT = 3;
    private static final int DOT_SPACING = 4;
    private static final int ICON = 12;
    private static final int KEY_HEIGHT = 11;
    private static final int KEY_PADDING = 2;
    private static final int GLYPH_GAP = 1;
    private static final int LABEL_GAP = 4;
    private static final int ABOVE_STATUS_BARS = 62;

    private static final int BACKGROUND_TOP = 0xE0151719;
    private static final int BACKGROUND_BOTTOM = 0xE00C0D0E;
    private static final int BORDER_TOP = 0xA08A9096;
    private static final int BORDER_BOTTOM = 0x804A4E53;
    private static final int BORDER_FOCUSED_TOP = 0xF0D4DADF;
    private static final int BORDER_FOCUSED_BOTTOM = 0xC0868C92;
    private static final int RULE = 0x30FFFFFF;
    private static final int TRACK = 0x50000000;
    private static final int KEY_EDGE = 0xFF5C6166;
    private static final int KEY_FACE = 0xFF2A2D31;
    private static final int KEY_PRESSED_TEXT = 0xFF1A1C1E;

    private static final int ACCENT = BntToolVisuals.opaque(BntToolVisuals.SELECTED);
    private static final int TEXT = 0xFFC8CCD0;
    private static final int LABEL = 0xFF969BA0;
    private static final int DIM = 0xFF64686C;
    private static final int HINT = 0xFF8C949C;
    private static final int METER = 0xFF7C8186;
    private static final int GOOD = BntToolVisuals.opaque(BntToolVisuals.GOOD);
    private static final int WARNING = BntToolVisuals.opaque(BntToolVisuals.WARNING);

    private static final Glyph RMB = new Mouse(PonderGuiTextures.ICON_RMB);
    private static final Glyph SCROLL = new Mouse(PonderGuiTextures.ICON_SCROLL);
    private static final Glyph SHIFT = new Key(Component.translatable("bits_n_tracks.tool_panel.key.shift"), Screen::hasShiftDown);

    private BntToolHud() {
    }

    private sealed interface Glyph permits Mouse, Key {
        int width(Font font);
    }

    private record Mouse(PonderGuiTextures icon) implements Glyph {
        @Override
        public int width(Font font) {
            return ICON;
        }
    }

    private record Key(Component name, BooleanSupplier held) implements Glyph {
        @Override
        public int width(Font font) {
            return font.width(name) + 2 * KEY_PADDING;
        }
    }

    private record Control(List<Glyph> glyphs, Component label, int colour) {
        int width(Font font) {
            return glyphsWidth(font, glyphs) + LABEL_GAP + font.width(label);
        }

        boolean held() {
            boolean keyed = false;
            for (Glyph glyph : glyphs) {
                if (glyph instanceof Key key) {
                    if (!key.held().getAsBoolean()) {
                        return false;
                    }
                    keyed = true;
                }
            }
            return keyed;
        }
    }

    private record Row(Component label, @Nullable Component value, int colour, float fill, boolean marked) {
        static Row text(Component label, int colour) {
            return new Row(label, null, colour, -1.0F, false);
        }

        static Row value(Component label, Component value, int colour, boolean marked) {
            return new Row(label, value, colour, -1.0F, marked);
        }

        Row bar(float fill) {
            return new Row(label, value, colour, fill, marked);
        }

        boolean metered() {
            return fill >= 0.0F;
        }
    }

    private record Panel(Component mode, int index, int count, List<Row> rows, List<Control> controls, @Nullable Component message) {
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
        List<Control> actions = new ArrayList<>();
        BlockPos aimed = BntToolInput.aimedCog(level);
        switch (mode) {
            case OFFSET -> {
                BlockPos selected = BntToolState.leverCog;
                BlockPos shown = selected != null ? selected : aimed;
                Axis blockAxis = shown == null ? null : BntToolState.blockAxis(level, shown);
                if (blockAxis == null || !(level.getBlockEntity(shown) instanceof KineticBlockEntityPhysicsAccess access)) {
                    rows.add(none("cogwheel"));
                } else {
                    Axis[] axes = BntToolState.axes(blockAxis);
                    int current = selected == null ? -1 : BntToolState.axisRow(level, selected);
                    for (int i = 0; i < axes.length; i++) {
                        boolean allowed = BntToolState.axisAllowed(level, shown, blockAxis, axes[i]);
                        int colour = !allowed ? DIM : i == current ? ACCENT : selected == null ? LABEL : TEXT;
                        rows.add(Row.value(Component.translatable(BntToolState.axisKey(blockAxis, axes[i])), pixels(offset(access, axes[i])), colour, i == current));
                    }
                    if (selected != null) {
                        actions.add(action("move", SCROLL));
                        actions.add(action("axis", SHIFT, SCROLL));
                    } else {
                        actions.add(action("select", RMB));
                    }
                }
            }
            case ROUTE -> {
                if (aimed != null && level.getBlockEntity(aimed) instanceof KineticBlockEntityPhysicsAccess access) {
                    int side = access.bnt$getTrackRouteSide();
                    Component route = side < 0 || side >= Direction.values().length
                        ? Component.translatable("bits_n_tracks.tool_panel.route.automatic")
                        : Component.translatable("bits_n_tracks.tool_panel.side." + Direction.values()[side].getOpposite().getName());
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.routing"), route, TEXT, false));
                    actions.add(action("wrap", RMB));
                    actions.add(action("unwrap", RMB));
                } else {
                    rows.add(none("cogwheel"));
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
                    actions.add(selected ? action("tension", SCROLL) : action("select", RMB));
                } else {
                    rows.add(none("track"));
                }
            }
            case VISIBILITY -> {
                if (aimed != null && level.getBlockEntity(aimed) instanceof KineticBlockEntityPhysicsAccess access) {
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.model"), Component.translatable(access.bnt$isHiddenByLever()
                        ? "bits_n_tracks.tool_panel.model.hidden"
                        : "bits_n_tracks.tool_panel.model.shown"), TEXT, false));
                    actions.add(action("visibility", RMB));
                } else {
                    rows.add(none("cogwheel"));
                }
            }
            case WIDTH -> {
                if (aimed != null) {
                    BlockState state = level.getBlockState(aimed);
                    int pixels = BntCogwheelWidth.trackPixels(BntCogwheelWidth.quarters(state));
                    int full = BntCogwheelWidth.trackPixels(BntCogwheelWidth.full(state));
                    Component value = Component.translatable("bits_n_tracks.tool_panel.pixels", pixels);
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.width"), value, TEXT, false)
                        .bar(pixels / (float)full));
                    actions.add(action("narrow", RMB));
                    actions.add(action("widen", SHIFT, RMB));
                } else {
                    rows.add(none("cogwheel"));
                }
            }
            case PHYSICS -> {
                BlockPos target = BntToolInput.aimedBlock(level);
                if (target != null && BntCogwheelInteraction.canTogglePhysics(level, target)
                    && level.getBlockEntity(target) instanceof KineticBlockEntityPhysicsAccess access) {
                    boolean enabled = access.bnt$isPhysicsEnabled();
                    rows.add(Row.value(Component.translatable("bits_n_tracks.tool_panel.row.physics"),
                        Component.translatable(enabled ? "bits_n_tracks.tool_panel.on" : "bits_n_tracks.tool_panel.off"), enabled ? GOOD : TEXT, false));
                    actions.add(action("physics", RMB));
                } else {
                    rows.add(none("cogwheel"));
                }
            }
            case RESET -> {
                rows.add(Row.text(Component.translatable(CogAlignmentLeverItem.wholeTrack(stack)
                    ? "bits_n_tracks.tool_panel.row.reset"
                    : "bits_n_tracks.tool_panel.row.reset.single"), aimed == null ? DIM : TEXT));
                if (aimed != null && BntToolState.resetArmedFor(aimed)) {
                    actions.add(new Control(List.of(RMB), Component.translatable("bits_n_tracks.tool_panel.action.confirm"), WARNING));
                } else if (aimed != null) {
                    actions.add(action("reset", RMB, RMB));
                }
            }
        }
        return panel(Component.translatable(mode.translationKey()), mode.ordinal(), BntLeverMode.values().length, rows, actions, focused);
    }

    private static Panel tunerPanel(ClientLevel level, ItemStack stack, boolean focused) {
        BntTuning setting = SuspensionToolItem.setting(stack);
        List<Row> rows = new ArrayList<>();
        List<Control> actions = new ArrayList<>();
        BlockPos selected = BntToolState.tunerCog;
        BlockPos shown = selected;
        if (shown == null) {
            BlockPos aimed = BntToolInput.aimedBlock(level);
            shown = aimed != null && BntSuspension.hasPiece(level.getBlockEntity(aimed)) ? aimed : null;
        }
        if (shown == null) {
            rows.add(none("piece"));
        } else {
            for (BntTuning each : BntTuning.values()) {
                boolean current = each == setting;
                int colour = current ? selected != null ? ACCENT : TEXT : LABEL;
                rows.add(Row.value(Component.translatable(each.translationKey()),
                    Component.literal(String.valueOf(each.levelOf(level.getBlockEntity(shown)))), colour, current && selected != null));
            }
            actions.add(selected != null ? action("tune", SCROLL) : action("select", RMB));
        }
        return panel(Component.translatable(setting.translationKey()), setting.ordinal(), BntTuning.values().length, rows, actions, focused);
    }

    private static Panel panel(Component mode, int index, int count, List<Row> rows, List<Control> actions, boolean focused) {
        Component message = focused ? null : BntToolMessages.current();
        List<Control> controls = new ArrayList<>();
        if (focused) {
            controls.add(new Control(List.of(SCROLL), Component.translatable("bits_n_tracks.tool_panel.action.mode"), ACCENT));
        } else if (message == null) {
            controls.addAll(actions);
            if (actions.isEmpty()) {
                controls.add(new Control(List.of(new Key(Component.literal(AllKeys.TOOL_MENU.getBoundKey()), AllKeys.TOOL_MENU::isPressed), SCROLL),
                    Component.translatable("bits_n_tracks.tool_panel.action.modes"), DIM));
            }
        }
        return new Panel(mode, index, count, rows, controls, message);
    }

    private static Row none(String target) {
        return Row.text(Component.translatable("bits_n_tracks.tool_panel.none." + target), DIM);
    }

    private static Control action(String action, Glyph... glyphs) {
        return new Control(List.of(glyphs), Component.translatable("bits_n_tracks.tool_panel.action." + action), HINT);
    }


    private static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, Panel panel, boolean focused) {
        Font font = minecraft.font;
        Component mode = focused ? Component.literal("< ").append(panel.mode()).append(" >") : panel.mode();
        int dots = panel.count() * DOT_SPACING - (DOT_SPACING - DOT);
        int labels = 0;
        int values = 0;
        boolean metered = false;
        for (Row row : panel.rows()) {
            if (row.value() != null) {
                labels = Math.max(labels, font.width(row.label()));
                values = Math.max(values, font.width(row.value()));
                metered |= row.metered();
            }
        }
        int width = Math.max(MIN_WIDTH, font.width(mode) + GAP + dots);
        if (labels > 0) {
            width = Math.max(width, INDENT + labels + COLUMN + values + (metered ? GAP + BAR : 0));
        }
        for (Control control : panel.controls()) {
            width = Math.max(width, control.width(font));
        }
        List<List<Control>> controlLines = pack(font, panel.controls(), Math.max(width, PACK_WIDTH));
        for (List<Control> controlLine : controlLines) {
            width = Math.max(width, lineWidth(font, controlLine));
        }

        List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
        int rowLines = 0;
        for (Row row : panel.rows()) {
            List<FormattedCharSequence> lines = row.value() == null ? font.split(row.label(), width - INDENT) : List.of();
            wrapped.add(lines);
            rowLines += Math.max(1, lines.size());
        }
        List<FormattedCharSequence> message = panel.message() == null ? List.of() : font.split(panel.message(), width);
        int footer = panel.message() != null ? LINE * message.size() : CONTROL_LINE * controlLines.size();
        int height = HEADER + 3 + LINE * rowLines + 3 + footer - 1;

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
        int dotX = x + width - dots;
        for (int i = 0; i < panel.count(); i++) {
            int dx = dotX + i * DOT_SPACING;
            graphics.fill(dx, y + 3, dx + DOT, y + 3 + DOT, i == panel.index() ? ACCENT : DIM);
        }
        int line = y + HEADER;
        graphics.fill(x, line, x + width, line + 1, RULE);
        line += 3;

        int valueX = x + INDENT + labels + COLUMN;
        int valueRight = metered ? x + width - BAR - GAP : x + width;
        for (int r = 0; r < panel.rows().size(); r++) {
            Row row = panel.rows().get(r);
            if (row.value() == null) {
                for (FormattedCharSequence text : wrapped.get(r)) {
                    graphics.drawString(font, text, x + INDENT, line, row.colour(), false);
                    line += LINE;
                }
                continue;
            }
            if (row.marked()) {
                graphics.fill(x, line - 1, x + 2, line + 8, ACCENT);
            }
            graphics.drawString(font, row.label(), x + INDENT, line, row.marked() ? ACCENT : LABEL, false);            graphics.drawString(font, row.value(), Math.max(valueX, valueRight - font.width(row.value())), line, row.colour(), false);
            if (row.metered()) {
                int meterX = x + width - BAR;
                graphics.fill(meterX, line + 2, meterX + BAR, line + 6, TRACK);
                graphics.fill(meterX, line + 2, meterX + Math.round(BAR * Math.max(0.0F, Math.min(1.0F, row.fill()))), line + 6,
                    row.marked() ? ACCENT : METER);
            }
            line += LINE;
        }

        line += 1;
        graphics.fill(x, line, x + width, line + 1, RULE);
        line += 3;
        if (panel.message() != null) {
            for (FormattedCharSequence text : message) {
                graphics.drawString(font, text, x, line, TEXT, false);
                line += LINE;
            }
        } else {
            for (List<Control> controlLine : controlLines) {
                int cursor = x;
                for (Control control : controlLine) {
                    drawControl(graphics, font, control, cursor, line);
                    cursor += control.width(font) + CONTROL_GAP;
                }
                line += CONTROL_LINE;
            }
        }
        graphics.pose().popPose();
    }

    private static List<List<Control>> pack(Font font, List<Control> controls, int width) {
        List<List<Control>> lines = new ArrayList<>();
        List<Control> current = new ArrayList<>();
        int used = 0;
        for (Control control : controls) {
            int needed = control.width(font) + (current.isEmpty() ? 0 : CONTROL_GAP);
            if (!current.isEmpty() && used + needed > width) {
                lines.add(current);
                current = new ArrayList<>();
                used = 0;
                needed = control.width(font);
            }
            current.add(control);
            used += needed;
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines;
    }

    private static int lineWidth(Font font, List<Control> controls) {
        int width = 0;
        for (Control control : controls) {
            width += control.width(font) + CONTROL_GAP;
        }
        return width - CONTROL_GAP;
    }

    private static void drawControl(GuiGraphics graphics, Font font, Control control, int x, int y) {
        int end = drawGlyphs(graphics, font, control.glyphs(), x, y);
        graphics.drawString(font, control.label(), end + LABEL_GAP, y + 1, control.held() ? ACCENT : control.colour(), false);
    }

    private static int drawGlyphs(GuiGraphics graphics, Font font, List<Glyph> glyphs, int x, int y) {
        int cursor = x;
        for (Glyph glyph : glyphs) {
            if (glyph instanceof Mouse mouse) {
                graphics.pose().pushPose();
                graphics.pose().translate(cursor, y - 1, 0.0F);
                graphics.pose().scale(ICON / (float)mouse.icon().getWidth(), ICON / (float)mouse.icon().getHeight(), 1.0F);
                mouse.icon().render(graphics, 0, 0);
                graphics.pose().popPose();
            } else if (glyph instanceof Key key) {
                boolean held = key.held().getAsBoolean();
                int keyWidth = key.width(font);
                graphics.fill(cursor, y - 1, cursor + keyWidth, y - 1 + KEY_HEIGHT, held ? ACCENT : KEY_EDGE);
                graphics.fill(cursor + 1, y, cursor + keyWidth - 1, y - 2 + KEY_HEIGHT, held ? ACCENT : KEY_FACE);
                graphics.drawString(font, key.name(), cursor + KEY_PADDING, y + 1, held ? KEY_PRESSED_TEXT : TEXT, false);
            }
            cursor += glyph.width(font) + GLYPH_GAP;
        }
        return cursor - GLYPH_GAP;
    }

    private static int glyphsWidth(Font font, List<Glyph> glyphs) {
        int width = 0;
        for (Glyph glyph : glyphs) {
            width += glyph.width(font) + GLYPH_GAP;
        }
        return width - GLYPH_GAP;
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
