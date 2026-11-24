package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.TierColors;

import fr.heetstudio.confee.api.PreviewContext;
import fr.heetstudio.confee.api.PreviewRenderer;
import fr.heetstudio.confee.api.Theme;
import fr.heetstudio.confee.api.client.ConfeeDraw;
import fr.heetstudio.confee.api.client.ConfeeIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    // options and defaults of the tier colors: the colors of the outline, the name and the boss bar in game
    private static final String[] COLOR_PATHS = {"rareColor", "ultraColor", "infernalColor"};
    private static final int[] DEFAULT_COLORS = {TierColors.DEFAULT_RARE, TierColors.DEFAULT_ULTRA, TierColors.DEFAULT_INFERNAL};
    private static final String KEY = "infernalmobs.config.preview.";

    private ConfigPreviews() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    /**
     * A number in the player's language ("1 000", "9,5" in French): whole above 10, one decimal below. The grouping
     * separator is a plain space when the language uses a no-break one, which the game font may not have.
     */
    private static String number(double value) {
        String tag = Minecraft.getInstance().options.languageCode.replace('_', '-');
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag(tag));
        if (Character.isSpaceChar(symbols.getGroupingSeparator())) {
            symbols.setGroupingSeparator(' ');
        }
        return new DecimalFormat(value >= 10 ? "#,##0" : "#,##0.#", symbols).format(value);
    }

    /** Right-aligned text whose right edge is {@code rightX}; returns its left x. */
    private static int rightText(GuiGraphics g, String text, int rightX, int y, int color) {
        int left = rightX - font().width(text);
        ConfeeDraw.text(g, Component.literal(text), left, y, color);
        return left;
    }

    /**
     * A preview reading the options of the config from the screen. The value of the previewed option
     * ({@link PreviewContext#path()}) comes with the call, the others from the {@link PreviewContext}.
     */
    private abstract static class Scene implements PreviewRenderer {

        private Object value;
        private PreviewContext context;

        @Override
        public final void render(GuiGraphics g, int x, int y, int width, int height, Object value,
                                 PreviewContext context, float delta) {
            this.value = value;
            this.context = context;
            draw(g, x, y, width, height, context.theme());
        }

        abstract void draw(GuiGraphics g, int x, int y, int width, int height, Theme t);

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

        /** Color of a tier as edited on the screen, opaque. */
        final int tierColor(int tier) {
            return 0xFF000000 | integer(COLOR_PATHS[tier], DEFAULT_COLORS[tier]);
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

        static void centered(GuiGraphics g, String text, int centerX, int y, int color) {
            ConfeeDraw.text(g, Component.literal(text), centerX - font().width(text) / 2, y, color);
        }
    }

    // ------------------------------------------------------------------ rarities

    /**
     * How many mobs of each tier spawn among N hostile mobs: Rare 1/elite, Ultra 1/(elite x ultra), Infernal
     * 1/(elite x ultra x inferno). N is the power of ten that gives at least one Infernal mob, so every count reads as
     * a real number of mobs. The tiers stack, an Infernal mob is also counted as Rare and Ultra. Shared by the three
     * rarity options: the tier of the hovered option stays bright, the two others are dimmed.
     */
    public static final class Rarity extends Scene {

        private static final String[] PATHS = {"eliteRarity", "ultraRarity", "infernoRarity"};
        private static final int ROW = 27;

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            int elite = Math.max(1, integer("eliteRarity", 15));
            int ultra = Math.max(1, integer("ultraRarity", 7));
            int inferno = Math.max(1, integer("infernoRarity", 7));
            // 1 in this many hostile mobs, per tier
            double[] oneIn = {elite, (double) elite * ultra, (double) elite * ultra * inferno};
            double mobs = Math.min(1e9, Math.max(10, Math.pow(10, Math.ceil(Math.log10(oneIn[2])))));
            text(g, tr("rarity.title", number(mobs)), x, y, width, t.textOnDark());

            int focus = List.of(PATHS).indexOf(path());
            for (int tier = 0; tier < 3; tier++) {
                int rowY = y + 16 + tier * ROW;
                boolean dimmed = focus >= 0 && tier != focus;
                int nameColor = dimmed ? t.dimmed(tierColor(tier)) : tierColor(tier);
                int valueColor = dimmed ? t.dimmed(t.textOnDark()) : t.textOnDark();
                rightText(g, number(mobs / oneIn[tier]), x + width, rowY, valueColor);
                text(g, tierName(tier), x, rowY, width / 2, nameColor);
                text(g, tr("rarity.oneIn", number(oneIn[tier])), x, rowY + 10, width,
                        dimmed ? t.dimmed(t.textDisabled()) : t.textDisabled());
            }
        }

        @Override
        public int height(int width) {
            return 16 + 3 * ROW - 6;
        }
    }

    // ------------------------------------------------------------------ health

    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");

    /** A heart of the health bar, 9 x 9; {@code tint} dims it ({@code 0xFFFFFFFF} for the real colors). */
    private static void heart(GuiGraphics g, int x, int y, int tint) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9, tint);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, x, y, 9, 9, tint);
    }

    /**
     * Max health of a zombie per tier, in hearts as on the health bar (a heart is 2 health points): base health x
     * number of modifiers x health factor. Rare mobs get 2 to 4 modifiers, Ultra mobs 3 to 4 more, Infernal mobs 3 to
     * 4 more again. The bars share one scale, with a tick at the vanilla health. Shared by the health factor and the
     * "health changes disabled" toggle.
     */
    public static final class Health extends Scene {

        /** Health points of a zombie. */
        private static final int BASE = 20;
        /** Modifier counts of each tier: min, max. */
        private static final int[][] MODIFIERS = {{2, 4}, {5, 8}, {8, 12}};
        private static final int ROW = 22;

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            boolean disabled = flag("healthChangesDisabled");
            double factor = Math.max(0, option("modHealthFactor", Number.class, 1.0).doubleValue());
            text(g, tr("health.title"), x, y, width, t.textOnDark());

            // first row: the vanilla health, or why every tier keeps it
            int rowY = y + 14;
            if (disabled) {
                text(g, tr("health.vanilla"), x, rowY + 1, width, t.textDisabled());
            } else {
                hearts(g, t, x, rowY, width, tr("health.vanillaRow"), BASE, BASE, t.textDisabled(), false);
            }

            double scale = disabled ? BASE : Math.max(BASE, BASE * MODIFIERS[2][1] * factor);
            for (int tier = 0; tier < 3; tier++) {
                int tierY = rowY + 16 + tier * ROW;
                double min = disabled ? BASE : BASE * MODIFIERS[tier][0] * factor;
                double max = disabled ? BASE : BASE * MODIFIERS[tier][1] * factor;
                int color = disabled ? t.dimmed(tierColor(tier)) : tierColor(tier);
                hearts(g, t, x, tierY, width, tierName(tier), min, max, color, disabled);
                if (!disabled) {
                    bar(g, t, x, tierY + 12, width, min / scale, max / scale, BASE / scale, color);
                }
            }
        }

        /** Tier name on the left, "20 to 40" and a heart on the right. */
        private static void hearts(GuiGraphics g, Theme t, int x, int y, int width, String name, double min,
                                   double max, int nameColor, boolean dimmed) {
            // a heart is 2 health points, shown to the half heart
            double from = Math.round(min) / 2.0;
            double to = Math.round(max) / 2.0;
            String value = from == to ? number(from) : tr("health.range", number(from), number(to));
            heart(g, x + width - 9, y, dimmed ? Theme.mix(0xFFFFFFFF, t.panelBgDeep(), 0.55f) : 0xFFFFFFFF);
            int left = rightText(g, value, x + width - 12, y + 1, dimmed ? t.textDisabled() : t.textOnDark());
            text(g, name, x, y + 1, Math.max(0, left - x - 4), nameColor);
        }

        /** The range of a tier on the shared scale (0 to 1), and the tick of the vanilla health. */
        private static void bar(GuiGraphics g, Theme t, int x, int y, int width, double from, double to,
                                double vanilla, int color) {
            g.fill(x, y, x + width, y + 3, t.panelBg());
            int x0 = x + (int) Math.round(width * Math.min(1, from));
            int x1 = Math.max(x0 + 2, x + (int) Math.round(width * Math.min(1, to)));
            g.fill(x0, y, Math.min(x + width, x1), y + 3, color);
            int tick = x + (int) Math.round(width * Math.min(1, vanilla));
            g.fill(tick, y - 1, tick + 1, y + 4, t.textOnDark());
        }

        @Override
        public int height(int width) {
            return 14 + 16 + 3 * ROW - 4;
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
    private static void zombie(GuiGraphics g, int x, int y, float scale, int outline, boolean outlineOnly) {
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
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            switch (path()) {
                case "outline.throughWalls" -> throughWalls(g, x, y, width, t);
                case "outline.maxDistance" -> distance(g, x, y, width, height, t);
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
            return flag("outline.enabled") && flag(TIER_PATHS[tier]) ? tierColor(tier) : 0;
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
        private void tiers(GuiGraphics g, int x, int y, int width, Theme t, int focus) {
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
        private void throughWalls(GuiGraphics g, int x, int y, int width, Theme t) {
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
         * Zombies of the highest outlined tier going away from the player on a field that recedes to the horizon:
         * only those within the maximum distance are outlined. The scene fills the height of the section's preview.
         */
        private void distance(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            int maxDistance = integer("outline.maxDistance", 32);
            int color = topColor();
            int cell = width / DISTANCES.length;
            float near = distanceScale(DISTANCES[0]);
            float far = distanceScale(DISTANCES[DISTANCES.length - 1]);
            // the labels and the footer below the ground, as in the other scenes
            int groundY = y + height - 28;
            int horizonY = y + Math.max(4, (groundY - y) / 3);
            // the field: lighter towards the player, a line on the horizon
            g.fillGradient(x, horizonY, x + width, groundY, t.panelBgDeep(), t.panelBg());
            g.fill(x, horizonY, x + width, horizonY + 1, t.panelBg());
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

    // ------------------------------------------------------------------ colors

    /**
     * Each tier in its color as the player sees it in game: an outlined zombie, its name, and its boss bar in the
     * closest of the 7 boss bar colors. Shared by the three color options: the tier of the hovered option stays
     * bright, the two others are dimmed.
     */
    public static final class Colors extends Scene {

        private static final int ROW = 40;
        private static final String[] CLASS_KEYS = {"translation.infernalmobs:rareClass",
                "translation.infernalmobs:ultraClass", "translation.infernalmobs:infernalClass"};

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            int focus = List.of(COLOR_PATHS).indexOf(path());
            String zombie = Component.translatable("entity.minecraft.zombie").getString();
            for (int tier = 0; tier < 3; tier++) {
                int rowY = y + 1 + tier * ROW;
                int color = tierColor(tier);
                boolean dimmed = focus >= 0 && tier != focus;
                zombie(g, x + 1, rowY, 1, color, false);
                int textX = x + 16 + 8;
                int textW = width - (textX - x);
                // the name as the boss bar shows it, e.g. "Rare Zombie"
                String name = Component.translatable("translation.infernalmobs:nameFormat",
                        Component.translatable(CLASS_KEYS[tier]), "", zombie, "").getString().replaceAll(" +", " ").trim();
                text(g, name, textX, rowY + 6, textW, dimmed ? t.dimmed(color) : color);
                BossEvent.BossBarColor bar = TierColors.bossBar(color & 0xFFFFFF);
                bossBar(g, textX, rowY + 18, textW, bar);
                if (dimmed) {
                    g.fill(x, rowY - 1, x + width, rowY + 34, (t.panelBgDeep() & 0x00FFFFFF) | 0xB0000000);
                }
            }
        }

        /** A full boss bar of a vanilla color, stretched to the width. */
        private static void bossBar(GuiGraphics g, int x, int y, int width, BossEvent.BossBarColor color) {
            String base = "boss_bar/" + color.getName();
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(base + "_background"), x, y, width, 5);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(base + "_progress"), x, y, width, 5);
        }

        @Override
        public int height(int width) {
            return 2 * ROW + 36;
        }
    }

    // ------------------------------------------------------------------ loot

    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    // colors of the vanilla container screens (chest, inventory)
    private static final int CONTAINER_BG = 0xFFC6C6C6;
    private static final int CONTAINER_LIGHT = 0xFFFFFFFF;
    private static final int CONTAINER_SHADOW = 0xFF555555;
    private static final int CONTAINER_EDGE = 0xFF000000;
    private static final int CONTAINER_LABEL = 0xFF404040;

    /**
     * The loot as the chest it drops into: a vanilla container panel with one slot per stack, with its count and
     * enchantment glint (also from the title screen). A stack that cannot be read keeps its slot, tinted with the
     * error color. Past the rows of the panel, the last slot counts the stacks left out.
     */
    public static final class Loot extends Scene {

        private static final int SLOT_SIZE = 18;
        private static final int PADDING = 7;
        private static final int LABEL_HEIGHT = 11;
        private static final int MAX_ROWS = 5;

        private static int columns(int width) {
            return Math.max(1, Math.min(9, (width - 2 * PADDING) / SLOT_SIZE));
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int width, int height, Theme t) {
            List<?> entries = ownValue() instanceof List<?> list ? list : List.of();
            int columns = columns(width);
            int rows = Math.max(1, Math.min(MAX_ROWS, (entries.size() + columns - 1) / columns));
            int panelW = columns * SLOT_SIZE + 2 * PADDING;
            int panelH = PADDING + LABEL_HEIGHT + rows * SLOT_SIZE + PADDING;
            x += (width - panelW) / 2;
            panel(g, x, y, panelW, panelH);
            ConfeeDraw.textFit(g, Component.literal(tr("loot.title", entries.size())), x + PADDING + 1, y + PADDING - 1,
                    panelW - 2 * PADDING, CONTAINER_LABEL);

            int slots = rows * columns;
            int slotsY = y + PADDING + LABEL_HEIGHT;
            for (int i = 0; i < slots; i++) {
                int sx = x + PADDING + (i % columns) * SLOT_SIZE;
                int sy = slotsY + (i / columns) * SLOT_SIZE;
                g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, sx, sy, SLOT_SIZE, SLOT_SIZE);
                if (i >= entries.size()) {
                    continue;
                }
                if (i == slots - 1 && entries.size() > slots) {
                    String more = tr("loot.more", entries.size() - slots + 1);
                    ConfeeDraw.text(g, Component.literal(more), sx + (SLOT_SIZE - font().width(more)) / 2, sy + 5, CONTAINER_LABEL);
                } else if (!ConfeeIcons.drawStack(g, String.valueOf(entries.get(i)), 0, sx + 1, sy + 1)) {
                    g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, (t.error() & 0x00FFFFFF) | 0x90000000);
                }
            }
        }

        /** A vanilla container panel: light gray, lit from the top left, rounded corners. */
        private static void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 1, y, x + w - 1, y + h, CONTAINER_EDGE);
            g.fill(x, y + 1, x + w, y + h - 1, CONTAINER_EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, CONTAINER_BG);
            g.fill(x + 1, y + 1, x + w - 2, y + 2, CONTAINER_LIGHT);
            g.fill(x + 1, y + 1, x + 2, y + h - 2, CONTAINER_LIGHT);
            g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, CONTAINER_SHADOW);
            g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, CONTAINER_SHADOW);
        }

        @Override
        public int height(int width) {
            return PADDING + LABEL_HEIGHT + MAX_ROWS * SLOT_SIZE + PADDING;
        }
    }
}
