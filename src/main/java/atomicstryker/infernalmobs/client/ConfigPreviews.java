package atomicstryker.infernalmobs.client;

import fr.heetstudio.confee.api.PreviewContext;
import fr.heetstudio.confee.api.PreviewRenderer;
import fr.heetstudio.confee.api.Theme;
import fr.heetstudio.confee.api.client.ConfeeDraw;
import fr.heetstudio.confee.api.client.ConfeeIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

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

    private static String percent(double chance) {
        double value = chance * 100;
        return String.format(Locale.ROOT, value >= 1 ? "%.1f %%" : value >= 0.01 ? "%.2f %%" : "%.4f %%", value);
    }

    /**
     * A preview reading the options of the config from the screen. The value of the previewed option
     * ({@link PreviewContext#path()}) comes with the call, the others from the {@link PreviewContext}.
     */
    private abstract static class Scene implements PreviewRenderer {

        private Object value;
        private PreviewContext context;

        @Override
        public final void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value,
                                 PreviewContext context, float delta) {
            this.value = value;
            this.context = context;
            draw(g, x, y, width, height, context.theme());
        }

        abstract void draw(GuiGraphicsExtractor g, int x, int y, int width, int height, Theme t);

        /** Path of the option being previewed, e.g. {@code outline.rare}. */
        final String path() {
            return context.path();
        }

        /** Pending value of an option of the config. */
        final <T> T option(String optionPath, Class<T> type, T fallback) {
            if (optionPath.equals(path()) && type.isInstance(value)) {
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

        static void text(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
            ConfeeDraw.textFit(g, Component.literal(text), x, y, width, color);
        }

        static void right(GuiGraphicsExtractor g, String text, int rightX, int y, int color) {
            ConfeeDraw.text(g, Component.literal(text), rightX - font().width(text), y, color);
        }

        static void centered(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
            ConfeeDraw.text(g, Component.literal(text), centerX - font().width(text) / 2, y, color);
        }
    }

    // ------------------------------------------------------------------ rarities

    /**
     * Share of the hostile mobs of each tier: Rare 1/elite, Ultra 1/(elite x ultra), Infernal 1/(elite x ultra x inferno).
     * The tiers stack, an Infernal mob is also counted as Rare and Ultra. Shared by the three rarity options.
     */
    public static final class Rarity extends Scene {

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int width, int height, Theme t) {
            double rare = 1.0 / Math.max(1, integer("eliteRarity", 15));
            double ultra = rare / Math.max(1, integer("ultraRarity", 7));
            double infernal = ultra / Math.max(1, integer("infernoRarity", 7));
            text(g, tr("rarity.title"), x, y, width, t.textOnDark());
            tier(g, t, x, y + 14, width, tr("rare"), rare, RARE);
            tier(g, t, x, y + 40, width, tr("ultra"), ultra, ULTRA);
            tier(g, t, x, y + 66, width, tr("infernal"), infernal, INFERNAL);
        }

        private static void tier(GuiGraphicsExtractor g, Theme t, int x, int y, int width, String name, double chance, int color) {
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

    // ------------------------------------------------------------------ health

    /**
     * Max health of a zombie (20 health points) per tier: base health x number of modifiers x health factor.
     * Rare mobs get 2 to 4 modifiers, Ultra mobs 3 to 4 more, Infernal mobs 3 to 4 more again. Shared by the health
     * factor and the "health changes disabled" toggle.
     */
    public static final class Health extends Scene {

        private static final int BASE = 20;

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int width, int height, Theme t) {
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

        private static void tier(GuiGraphicsExtractor g, Theme t, int x, int y, int width, String name, double min, double max,
                                 int scale, int color) {
            text(g, name, x, y, width / 2, color);
            right(g, tr("health.range", Math.round(min), Math.round(max)), x + width, y, t.textOnDark());
            int barY = y + 11;
            g.fill(x, barY, x + width, barY + 4, t.panelBg());
            if (scale > 0) {
                int from = (int) (width * min / scale);
                int to = Math.max(from + 1, (int) (width * max / scale));
                g.fill(x, barY, x + from, barY + 4, Theme.mix(color, t.panelBg(), 0.6f));
                g.fill(x + from, barY, x + to, barY + 4, color);
            }
        }

        @Override
        public int height(int width) {
            return 88;
        }
    }

    // ------------------------------------------------------------------ outline

    private static final String ZOMBIE = "minecraft:zombie";
    private static final String BRICKS = "minecraft:block/stone_bricks";
    /**
     * "No outline" for {@link ConfeeDraw#entity}: transparent, so nothing is drawn, but not 0, so Confee keeps the front
     * view of the skin. With 0 it draws the 3D model when a world is loaded, unlike the outlined zombies next to it.
     */
    private static final int NO_OUTLINE = 0x00FFFFFF;
    // sample distances of the "max distance" scene, in blocks
    private static final int[] DISTANCES = {8, 16, 32, 64, 128};
    private static final String[] TIER_PATHS = {"outline.rare", "outline.ultra", "outline.infernal"};

    /**
     * A zombie, 16 x 32 pixels at scale 1, top left corner at x, y. Confee rounds a scale above 1 down to a whole
     * number (crisp texture).
     *
     * @param outline     color of the outline, 0 for none. Like the Glowing effect, it follows the silhouette 1 pixel
     *                    around the body, whatever the distance.
     * @param outlineOnly true to draw only the outline (the mob is hidden behind blocks)
     */
    private static void zombie(GuiGraphicsExtractor g, int x, int y, float scale, int outline, boolean outlineOnly) {
        // the box leaves 1 pixel around the body for the outline
        ConfeeDraw.entity(g, ZOMBIE, x - 1, y - 1, Math.round(16 * scale) + 2, Math.round(32 * scale) + 2,
                outline != 0 ? outline : NO_OUTLINE, outlineOnly);
    }

    private static String tierName(int tier) {
        return tr(tier == 0 ? "rare" : tier == 1 ? "ultra" : "infernal");
    }

    /**
     * The preview of the outline section: the scene of the hovered option. The tier toggles and "enabled" show one
     * zombie per tier, "through walls" a zombie behind a wall, "max distance" zombies going away.
     */
    public static final class OutlinePreview extends Scene {

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int width, int height, Theme t) {
            switch (path()) {
                case "outline.throughWalls" -> throughWalls(g, x, y, width, t);
                case "outline.maxDistance" -> distance(g, x, y, width, t);
                case "outline.rare" -> tiers(g, x, y, width, t, 0);
                case "outline.ultra" -> tiers(g, x, y, width, t, 1);
                case "outline.infernal" -> tiers(g, x, y, width, t, 2);
                default -> tiers(g, x, y, width, t, -1);
            }
        }

        @Override
        public int height(int width) {
            // the same height for every scene of the section: the tallest one
            return Math.max(tiersHeight(width), wallsHeight(width));
        }

        /** Outline color of a tier, 0 when it gets none. */
        private int color(int tier) {
            return flag("outline.enabled") && flag(TIER_PATHS[tier]) ? TIER_COLORS[tier] : 0;
        }

        /** Color of the highest tier that is outlined, 0 when none is. */
        private int topColor() {
            for (int tier = 2; tier >= 0; tier--) {
                if (color(tier) != 0) {
                    return color(tier);
                }
            }
            return 0;
        }

        // ------------------------------------------------------------------ tiers

        private static int tiersScale(int width) {
            return width / 3 >= 36 ? 2 : 1;
        }

        private static int tiersHeight(int width) {
            return 4 + 32 * tiersScale(width) + 28;
        }

        /**
         * One zombie per tier, with the outline it gets.
         *
         * @param focus -1: every tier, else the tier of the hovered toggle, put forward with the two others dimmed
         */
        private void tiers(GuiGraphicsExtractor g, int x, int y, int width, Theme t, int focus) {
            int third = width / 3;
            int scale = tiersScale(width);
            int dollW = 16 * scale;
            int dollH = 32 * scale;
            int top = y + 4;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());
            for (int tier = 0; tier < 3; tier++) {
                int cellX = x + tier * third;
                int color = color(tier);
                zombie(g, cellX + (third - dollW) / 2, top, scale, color, false);
                boolean dimmed = focus >= 0 && tier != focus;
                if (dimmed) {
                    g.fill(cellX, y, cellX + third, groundY + 2, (t.panelBgDeep() & 0x00FFFFFF) | 0xB0000000);
                }
                int labelColor = color != 0 ? color : t.textDisabled();
                // dimmed through the color: some versions draw the text above the fills
                centered(g, tierName(tier), cellX + third / 2, groundY + 4,
                        dimmed ? Theme.mix(labelColor, t.panelBgDeep(), 0.65f) : labelColor);
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

        // ------------------------------------------------------------------ through walls

        private static int wallsScale(int width) {
            return width / 2 >= 40 ? 2 : 1;
        }

        private static int wallsHeight(int width) {
            return 6 + 32 * wallsScale(width) + 28;
        }

        /**
         * Two zombies of the highest outlined tier: one in the open, one fully behind a stone brick wall. The game
         * outlines the hidden one only when the outline is visible through walls; otherwise it needs a line of sight.
         */
        private void throughWalls(GuiGraphicsExtractor g, int x, int y, int width, Theme t) {
            boolean throughWalls = flag("outline.throughWalls");
            int color = topColor();
            int half = width / 2;
            int scale = wallsScale(width);
            int dollW = 16 * scale;
            int dollH = 32 * scale;
            int top = y + 6;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());

            // in the open: outlined with or without the option
            zombie(g, x + (half - dollW) / 2, top, scale, color, false);
            centered(g, tr("outline.visible"), x + half / 2, groundY + 4, t.textDisabled());

            // behind the wall: only its outline, drawn over the wall
            int wallX0 = x + half + 4;
            ConfeeDraw.tiled(g, BRICKS, wallX0, y, x + width - wallX0, groundY - y);
            g.fill(wallX0, y, wallX0 + 1, groundY, 0x60000000);
            if (throughWalls && color != 0) {
                zombie(g, x + half + (half - dollW) / 2, top, scale, color, true);
            }
            centered(g, tr("outline.behindWall"), x + half + half / 2, groundY + 4, t.textDisabled());

            String footer = color == 0 ? tr("outline.off") : tr(throughWalls ? "outline.walls.on" : "outline.walls.off");
            text(g, footer, x, groundY + 18, width, t.textOnDark());
        }

        // ------------------------------------------------------------------ distance

        /** Smaller with the distance, 1 at 8 blocks: below 1, Confee draws any size. */
        private static float distanceScale(int distance) {
            return (float) Math.pow(8.0 / distance, 0.6);
        }

        /**
         * Zombies of the highest outlined tier going away from the player: only those within the maximum distance are
         * outlined.
         */
        private void distance(GuiGraphicsExtractor g, int x, int y, int width, Theme t) {
            int maxDistance = integer("outline.maxDistance", 32);
            int color = topColor();
            int cell = width / DISTANCES.length;
            float near = distanceScale(DISTANCES[0]);
            float far = distanceScale(DISTANCES[DISTANCES.length - 1]);
            int nearH = Math.round(32 * near);
            int groundY = y + 4 + nearH;
            int horizonY = y + 4 + nearH / 3;
            g.fill(x, groundY, x + width, groundY + 1, t.panelBg());
            int cutX = -1;
            for (int i = 0; i < DISTANCES.length; i++) {
                int distance = DISTANCES[i];
                float scale = distanceScale(distance);
                int dollW = Math.round(16 * scale);
                int dollH = Math.round(32 * scale);
                // farther mobs stand higher, towards the horizon
                int feetY = Math.round(horizonY + (groundY - horizonY) * (scale - far) / (near - far));
                int cellX = x + i * cell;
                boolean outlined = distance <= maxDistance;
                zombie(g, cellX + (cell - dollW) / 2, feetY - dollH, scale, outlined ? color : 0, false);
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
    }

    // ------------------------------------------------------------------ loot

    /** The loot as a grid of item stacks, with their count and enchantment glint (also from the title screen). */
    public static final class Loot extends Scene {

        private static final int CELL = 18;

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int width, int height, Theme t) {
            List<?> entries = ownValue() instanceof List<?> list ? list : List.of();
            text(g, tr("loot.title", entries.size()), x, y, width, t.textOnDark());
            int columns = Math.max(1, width / CELL);
            int index = 0;
            for (Object entry : entries) {
                int cx = x + (index % columns) * CELL;
                int cy = y + 14 + (index / columns) * CELL;
                if (cy + CELL > y + height) {
                    break;
                }
                g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, t.panelBg());
                // a stack that cannot be read leaves its cell to the next one
                if (ConfeeIcons.drawStack(g, String.valueOf(entry), 0, cx, cy)) {
                    index++;
                }
            }
        }

        @Override
        public int height(int width) {
            return 14 + 10 * CELL;
        }
    }
}
