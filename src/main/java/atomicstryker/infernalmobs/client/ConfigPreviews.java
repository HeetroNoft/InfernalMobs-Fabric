package atomicstryker.infernalmobs.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.heetstudio.confee.api.PreviewContext;
import fr.heetstudio.confee.api.PreviewRenderer;
import fr.heetstudio.confee.api.Theme;
import fr.heetstudio.confee.api.client.ConfeeDraw;
import fr.heetstudio.confee.api.client.ConfeeIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Live previews of the config screen, drawn by Confee in its preview column ({@code @Preview(renderer = ...)}).
 * Client only: Confee creates them when the screen shows the preview.
 * <p>Every preview reads the pending values of the options it shows (edited, not saved yet) from the
 * {@link PreviewContext}, and draws with the colors of the screen's theme. Only the tier colors are fixed: they are
 * the colors of the outline and of the boss bar in game.</p>
 */
public final class ConfigPreviews {

    // same colors as the outline renderer and the boss bar names
    private static final int RARE = 0xFF55FF55;
    private static final int ULTRA = 0xFFFFFF55;
    private static final int INFERNAL = 0xFFFF5555;
    private static final int[] TIER_COLORS = {RARE, ULTRA, INFERNAL};
    private static final String KEY = "infernalmobs.config.preview.";

    private ConfigPreviews() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    /** {@code color} over {@code background}, {@code amount} of the way (0 = color, 1 = background). */
    private static int mix(int color, int background, float amount) {
        int r = Math.round(((color >> 16) & 0xFF) * (1 - amount) + ((background >> 16) & 0xFF) * amount);
        int g = Math.round(((color >> 8) & 0xFF) * (1 - amount) + ((background >> 8) & 0xFF) * amount);
        int b = Math.round((color & 0xFF) * (1 - amount) + (background & 0xFF) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static String percent(double chance) {
        double value = chance * 100;
        return String.format(Locale.ROOT, value >= 1 ? "%.1f %%" : value >= 0.01 ? "%.2f %%" : "%.4f %%", value);
    }

    /**
     * A preview reading the options of the config from the screen. The value of its own option comes with the call,
     * the others from the {@link PreviewContext}.
     */
    private abstract static class Scene implements PreviewRenderer {

        private final String path;
        private Object value;
        private PreviewContext context;

        Scene(String path) {
            this.path = path;
        }

        @Override
        public final void render(GuiGraphics g, int x, int y, int width, int height, Object value,
                                 PreviewContext context, float delta) {
            this.value = value;
            this.context = context;
            draw(g, x, y, width, height, context.theme());
        }

        abstract void draw(GuiGraphics g, int x, int y, int width, int height, Theme t);

        /** Pending value of an option of the config. */
        final <T> T option(String optionPath, Class<T> type, T fallback) {
            if (optionPath.equals(path) && type.isInstance(value)) {
                return type.cast(value);
            }
            return context.value(optionPath, type, fallback);
        }

        final boolean flag(String optionPath) {
            return option(optionPath, Boolean.class, Boolean.FALSE);
        }

        final int integer(String optionPath, int fallback) {
            return option(optionPath, Number.class, fallback).intValue();
        }

        final Object ownValue() {
            return value;
        }

        static void text(GuiGraphics g, String text, int x, int y, int width, int color) {
            ConfeeDraw.textFit(g, Component.literal(text), x, y, width, color);
        }

        static void right(GuiGraphics g, String text, int rightX, int y, int color) {
            ConfeeDraw.text(g, Component.literal(text), rightX - font().width(text), y, color);
        }

        static void centered(GuiGraphics g, String text, int centerX, int y, int color) {
            ConfeeDraw.text(g, Component.literal(text), centerX - font().width(text) / 2, y, color);
        }
    }

    // ------------------------------------------------------------------ rarities

    /**
     * Share of the hostile mobs of each tier: Rare 1/elite, Ultra 1/(elite x ultra), Infernal 1/(elite x ultra x inferno).
     * The tiers stack, an Infernal mob is also counted as Rare and Ultra.
     */
    private abstract static class Rarity extends Scene {

        Rarity(String path) {
            super(path);
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            double rare = 1.0 / Math.max(1, integer("eliteRarity", 15));
            double ultra = rare / Math.max(1, integer("ultraRarity", 7));
            double infernal = ultra / Math.max(1, integer("infernoRarity", 7));
            text(g, tr("rarity.title"), x, y, width, t.textOnDark());
            tier(g, t, x, y + 14, width, tr("rare"), rare, RARE);
            tier(g, t, x, y + 40, width, tr("ultra"), ultra, ULTRA);
            tier(g, t, x, y + 66, width, tr("infernal"), infernal, INFERNAL);
        }

        private static void tier(GuiGraphics g, Theme t, int x, int y, int width, String name, double chance, int color) {
            text(g, name, x, y, width / 2, color);
            right(g, percent(chance), x + width, y, t.textOnDark());
            text(g, tr("rarity.oneIn", Math.round(1 / chance)), x, y + 10, width, t.textDisabled());
            // bar on a log scale, 100 % to 1 in 100 000
            int barY = y + 20;
            g.fill(x, barY, x + width, barY + 2, t.panelBg());
            double fill = Math.max(0, 1 + Math.log10(chance) / 5);
            g.fill(x, barY, x + (int) (width * fill), barY + 2, color);
        }

        @Override
        public int height(int width) {
            return 92;
        }
    }

    public static final class EliteRarity extends Rarity {
        public EliteRarity() {
            super("eliteRarity");
        }
    }

    public static final class UltraRarity extends Rarity {
        public UltraRarity() {
            super("ultraRarity");
        }
    }

    public static final class InfernoRarity extends Rarity {
        public InfernoRarity() {
            super("infernoRarity");
        }
    }

    // ------------------------------------------------------------------ health

    /**
     * Max health of a zombie (20 health points) per tier: base health x number of modifiers x health factor.
     * Rare mobs get 2 to 4 modifiers, Ultra mobs 3 to 4 more, Infernal mobs 3 to 4 more again.
     */
    private abstract static class Health extends Scene {

        private static final int BASE = 20;

        Health(String path) {
            super(path);
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            text(g, tr("health.title", BASE), x, y, width, t.textOnDark());
            if (flag("healthChangesDisabled")) {
                text(g, tr("health.vanilla", BASE), x, y + 16, width, t.textDisabled());
                return;
            }
            double factor = option("modHealthFactor", Number.class, 1.0).doubleValue();
            int max = (int) Math.ceil(BASE * 12 * factor);
            tier(g, t, x, y + 16, width, tr("rare"), BASE * 2 * factor, BASE * 4 * factor, max, RARE);
            tier(g, t, x, y + 42, width, tr("ultra"), BASE * 5 * factor, BASE * 8 * factor, max, ULTRA);
            tier(g, t, x, y + 68, width, tr("infernal"), BASE * 8 * factor, BASE * 12 * factor, max, INFERNAL);
        }

        private static void tier(GuiGraphics g, Theme t, int x, int y, int width, String name, double min, double max,
                                 int scale, int color) {
            text(g, name, x, y, width / 2, color);
            right(g, tr("health.range", Math.round(min), Math.round(max)), x + width, y, t.textOnDark());
            int barY = y + 11;
            g.fill(x, barY, x + width, barY + 4, t.panelBg());
            if (scale > 0) {
                int from = (int) (width * min / scale);
                int to = Math.max(from + 1, (int) (width * max / scale));
                g.fill(x, barY, x + from, barY + 4, mix(color, t.panelBg(), 0.6f));
                g.fill(x + from, barY, x + to, barY + 4, color);
            }
        }

        @Override
        public int height(int width) {
            return 88;
        }
    }

    public static final class HealthFactor extends Health {
        public HealthFactor() {
            super("modHealthFactor");
        }
    }

    public static final class HealthDisabled extends Health {
        public HealthDisabled() {
            super("healthChangesDisabled");
        }
    }

    // ------------------------------------------------------------------ outline

    private static final Identifier ZOMBIE = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final Identifier BRICKS = Identifier.withDefaultNamespace("textures/block/stone_bricks.png");
    // sample distances of the "max distance" scene, in blocks
    private static final int[] DISTANCES = {8, 16, 32, 64, 128};

    /**
     * Front faces of a zombie, as in the inventory: x, y, width, height on a 16 x 32 pixel doll, then u, v in the
     * 64 x 64 skin. The left arm and leg use the right ones, like the zombie model.
     */
    private static final int[][] ZOMBIE_PARTS = {
            {4, 0, 8, 8, 8, 8},      // head
            {4, 8, 8, 12, 20, 20},   // body
            {0, 8, 4, 12, 44, 20},   // right arm
            {12, 8, 4, 12, 44, 20},  // left arm
            {4, 20, 4, 12, 4, 20},   // right leg
            {8, 20, 4, 12, 4, 20},   // left leg
    };

    /** Screen rectangle of a doll part: x0, y0, x1, y1. Both edges are rounded so the parts join at any scale. */
    private static int[] part(int x, int y, float scale, int[] p) {
        return new int[]{x + Math.round(p[0] * scale), y + Math.round(p[1] * scale),
                x + Math.round((p[0] + p[2]) * scale), y + Math.round((p[1] + p[3]) * scale)};
    }

    /**
     * A zombie, 16 x 32 pixels at scale 1, top left corner at x, y.
     *
     * @param outline color of the outline, 0 for none. Like the Glowing effect, it follows the silhouette 1 pixel
     *                around the body, whatever the distance.
     * @param body    false to draw only the outline (the mob is hidden behind blocks)
     */
    private static void zombie(GuiGraphics g, int x, int y, float scale, int outline, boolean body) {
        if (outline != 0) {
            for (int[] p : ZOMBIE_PARTS) {
                int[] r = part(x, y, scale, p);
                g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, outline);
            }
        }
        if (body) {
            for (int[] p : ZOMBIE_PARTS) {
                int[] r = part(x, y, scale, p);
                g.blit(RenderPipelines.GUI_TEXTURED, ZOMBIE, r[0], r[1], p[4], p[5], r[2] - r[0], r[3] - r[1], p[2], p[3], 64, 64);
            }
        }
    }

    /** Stone bricks filling a rectangle, tiles aligned on the rectangle's top left corner. */
    private static void wall(GuiGraphics g, int x0, int y0, int x1, int y1, int clipX0, int clipY0, int clipX1, int clipY1) {
        g.enableScissor(Math.max(x0, clipX0), Math.max(y0, clipY0), Math.min(x1, clipX1), Math.min(y1, clipY1));
        for (int ty = y0; ty < y1; ty += 16) {
            for (int tx = x0; tx < x1; tx += 16) {
                g.blit(RenderPipelines.GUI_TEXTURED, BRICKS, tx, ty, 0, 0, 16, 16, 16, 16);
            }
        }
        g.disableScissor();
    }

    private static String tierName(int tier) {
        return tr(tier == 0 ? "rare" : tier == 1 ? "ultra" : "infernal");
    }

    /** The outline settings being edited. */
    private abstract static class OutlineScene extends Scene {

        private static final String[] TIER_PATHS = {"outline.rare", "outline.ultra", "outline.infernal"};

        OutlineScene(String path) {
            super(path);
        }

        /** Outline color of a tier, 0 when it gets none. */
        final int color(int tier) {
            return flag("outline.enabled") && flag(TIER_PATHS[tier]) ? TIER_COLORS[tier] : 0;
        }

        /** Color of the highest tier that is outlined, 0 when none is. */
        final int topColor() {
            for (int tier = 2; tier >= 0; tier--) {
                if (color(tier) != 0) {
                    return color(tier);
                }
            }
            return 0;
        }
    }

    /**
     * One zombie per tier, with the outline it gets. The tier toggles put their tier forward and dim the two others.
     */
    private abstract static class Tiers extends OutlineScene {

        /** -1: every tier, else the tier of the option */
        private final int focus;

        Tiers(String path, int focus) {
            super(path);
            this.focus = focus;
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            int third = width / 3;
            float scale = third >= 36 ? 2 : 1.5f;
            int dollW = Math.round(16 * scale);
            int dollH = Math.round(32 * scale);
            int top = y + 4;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());
            for (int tier = 0; tier < 3; tier++) {
                int cellX = x + tier * third;
                int color = color(tier);
                zombie(g, cellX + (third - dollW) / 2, top, scale, color, true);
                boolean dimmed = focus >= 0 && tier != focus;
                if (dimmed) {
                    g.fill(cellX, y, cellX + third, groundY + 2, (t.panelBgDeep() & 0x00FFFFFF) | 0xB0000000);
                }
                int labelColor = color != 0 ? color : t.textDisabled();
                // dimmed through the color: some versions draw the text above the fills
                centered(g, tierName(tier), cellX + third / 2, groundY + 4,
                        dimmed ? mix(labelColor, t.panelBgDeep(), 0.65f) : labelColor);
            }
            String footer;
            if (!flag("outline.enabled")) {
                footer = tr("outline.off");
            } else if (focus >= 0) {
                footer = tr(color(focus) != 0 ? "outline.shown" : "outline.hidden", tierName(focus));
            } else {
                footer = tr("outline.distance", integer("outline.maxDistance", 32));
            }
            text(g, footer, x, groundY + 18, width, t.textOnDark());
        }

        @Override
        public int height(int width) {
            return 4 + Math.round(32 * (width / 3 >= 36 ? 2 : 1.5f)) + 28;
        }
    }

    public static final class OutlineEnabled extends Tiers {
        public OutlineEnabled() {
            super("outline.enabled", -1);
        }
    }

    public static final class OutlineRare extends Tiers {
        public OutlineRare() {
            super("outline.rare", 0);
        }
    }

    public static final class OutlineUltra extends Tiers {
        public OutlineUltra() {
            super("outline.ultra", 1);
        }
    }

    public static final class OutlineInfernal extends Tiers {
        public OutlineInfernal() {
            super("outline.infernal", 2);
        }
    }

    /**
     * Two zombies of the highest outlined tier: one in the open, one fully behind a stone brick wall. The game
     * outlines the hidden one only when the outline is visible through walls; otherwise it needs a line of sight.
     */
    public static final class OutlineThroughWalls extends OutlineScene {

        public OutlineThroughWalls() {
            super("outline.throughWalls");
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            boolean throughWalls = flag("outline.throughWalls");
            int color = topColor();
            int half = width / 2;
            float scale = half >= 40 ? 2 : 1.5f;
            int dollW = Math.round(16 * scale);
            int dollH = Math.round(32 * scale);
            int top = y + 6;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());

            // in the open: outlined with or without the option
            zombie(g, x + (half - dollW) / 2, top, scale, color, true);
            centered(g, tr("outline.visible"), x + half / 2, groundY + 4, t.textDisabled());

            // behind the wall: the wall is drawn over the zombie, then its outline over the wall
            int hiddenX = x + half + (half - dollW) / 2;
            int wallX0 = x + half + 4;
            int wallX1 = x + width;
            wall(g, wallX0, y, wallX1, groundY, x, y, x + width, groundY);
            g.fill(wallX0, y, wallX0 + 1, groundY, 0x60000000);
            if (throughWalls && color != 0) {
                zombie(g, hiddenX, top, scale, color, false);
                // the body stays hidden: the wall is drawn again inside the silhouette
                for (int[] p : ZOMBIE_PARTS) {
                    int[] r = part(hiddenX, top, scale, p);
                    wall(g, wallX0, y, wallX1, groundY, r[0], r[1], r[2], r[3]);
                }
            }
            centered(g, tr("outline.behindWall"), x + half + half / 2, groundY + 4, t.textDisabled());

            String footer = color == 0 ? tr("outline.off") : tr(throughWalls ? "outline.walls.on" : "outline.walls.off");
            text(g, footer, x, groundY + 18, width, t.textOnDark());
        }

        @Override
        public int height(int width) {
            return 6 + Math.round(32 * (width / 2 >= 40 ? 2 : 1.5f)) + 28;
        }
    }

    /**
     * Zombies of the highest outlined tier going away from the player, smaller with the distance: only those within
     * the maximum distance are outlined.
     */
    public static final class OutlineDistance extends OutlineScene {

        public OutlineDistance() {
            super("outline.maxDistance");
        }

        private static float scale(int distance) {
            return 2.2f / (1 + distance / 16f);
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            int maxDistance = integer("outline.maxDistance", 32);
            int color = topColor();
            int cell = width / DISTANCES.length;
            int nearH = Math.round(32 * scale(DISTANCES[0]));
            int groundY = y + 4 + nearH;
            int horizonY = y + 4 + nearH / 3;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());
            float near = scale(DISTANCES[0]);
            float far = scale(DISTANCES[DISTANCES.length - 1]);
            int cutX = -1;
            for (int i = 0; i < DISTANCES.length; i++) {
                int distance = DISTANCES[i];
                float scale = scale(distance);
                int dollW = Math.round(16 * scale);
                int dollH = Math.round(32 * scale);
                // farther mobs stand higher, towards the horizon
                int feetY = Math.round(horizonY + (groundY - horizonY) * (scale - far) / (near - far));
                int cellX = x + i * cell;
                boolean outlined = distance <= maxDistance;
                zombie(g, cellX + (cell - dollW) / 2, feetY - dollH, scale, outlined ? color : 0, true);
                centered(g, String.valueOf(distance), cellX + cell / 2, groundY + 4,
                        outlined && color != 0 ? color : t.textDisabled());
                if (!outlined && cutX < 0 && i > 0) {
                    cutX = cellX;
                }
            }
            // the limit, between the last outlined zombie and the first one without outline
            if (cutX > 0 && color != 0) {
                for (int dy = horizonY - 4; dy < groundY + 12; dy += 3) {
                    g.fill(cutX, dy, cutX + 1, dy + 2, (color & 0x00FFFFFF) | 0xA0000000);
                }
            }
            String footer = color == 0 ? tr("outline.off") : tr("outline.distance", maxDistance);
            text(g, footer, x, groundY + 18, width, t.textOnDark());
        }

        @Override
        public int height(int width) {
            return 4 + Math.round(32 * scale(DISTANCES[0])) + 28;
        }
    }

    // ------------------------------------------------------------------ loot

    /** The loot as a grid of item icons with their count (icons from {@code ConfeeIcons}, also from the title screen). */
    public static final class Loot extends Scene {

        private static final int CELL = 18;

        public Loot() {
            // the three loot lists share this preview: its own value is always the edited list
            super("");
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            List<?> entries = ownValue() instanceof List<?> list ? list : List.of();
            text(g, tr("loot.title", entries.size()), x, y, width, t.textOnDark());
            int columns = Math.max(1, width / CELL);
            int index = 0;
            for (Object entry : entries) {
                Stack stack = Stack.parse(String.valueOf(entry));
                if (stack == null) {
                    continue;
                }
                int cx = x + (index % columns) * CELL;
                int cy = y + 14 + (index / columns) * CELL;
                if (cy + CELL > y + height) {
                    break;
                }
                g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, t.panelBg());
                if (!ConfeeIcons.drawItem(g, stack.id(), cx, cy, 16)) {
                    continue;
                }
                if (stack.count() > 1) {
                    right(g, String.valueOf(stack.count()), cx + CELL - 1, cy + CELL - 9, t.textOnDark());
                }
                index++;
            }
        }

        @Override
        public int height(int width) {
            return 14 + 10 * CELL;
        }

        private record Stack(String id, int count) {

            static Stack parse(String snbt) {
                try {
                    CompoundTag tag = TagParser.parseCompoundFully(snbt);
                    return new Stack(tag.getStringOr("id", ""), tag.getIntOr("count", 1));
                } catch (CommandSyntaxException e) {
                    return null;
                }
            }
        }
    }
}
