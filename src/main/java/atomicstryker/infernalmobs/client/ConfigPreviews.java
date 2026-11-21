package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.Config;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.heetstudio.confee.api.PreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Live previews of the config screen, drawn by Confee in its preview column ({@code @Preview(renderer = ...)}).
 * Client only: Confee creates them when the screen shows the preview.
 * <p>A renderer only gets the value of its own option being edited; the other options come from the saved config.</p>
 */
public final class ConfigPreviews {

    // same colors as the outline renderer and the boss bar names
    private static final int RARE = 0xFF55FF55;
    private static final int ULTRA = 0xFFFFFF55;
    private static final int INFERNAL = 0xFFFF5555;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF8A8A8D;
    private static final String KEY = "infernalmobs.config.preview.";

    private ConfigPreviews() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    private static void line(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        g.text(font(), font().plainSubstrByWidth(text, width), x, y, color);
    }

    private static String percent(double chance) {
        double value = chance * 100;
        return String.format(Locale.ROOT, value >= 1 ? "%.1f %%" : value >= 0.01 ? "%.2f %%" : "%.4f %%", value);
    }

    // ------------------------------------------------------------------ rarities

    /**
     * Share of the hostile mobs of each tier: Rare 1/elite, Ultra 1/(elite x ultra), Infernal 1/(elite x ultra x inferno).
     * The tiers stack, an Infernal mob is also counted as Rare and Ultra.
     */
    public abstract static class Rarity implements PreviewRenderer {

        abstract int[] rarities(int edited);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            int[] r = rarities(((Number) value).intValue());
            int elite = Math.max(1, r[0]);
            int ultra = Math.max(1, r[1]);
            int inferno = Math.max(1, r[2]);
            double rare = 1.0 / elite;
            double ultraChance = rare / ultra;
            double infernalChance = ultraChance / inferno;
            line(g, tr("rarity.title"), x, y, width, TEXT);
            tier(g, x, y + 14, width, tr("rare"), rare, RARE);
            tier(g, x, y + 40, width, tr("ultra"), ultraChance, ULTRA);
            tier(g, x, y + 66, width, tr("infernal"), infernalChance, INFERNAL);
        }

        private static void tier(GuiGraphicsExtractor g, int x, int y, int width, String name, double chance, int color) {
            line(g, name, x, y, width / 2, color);
            String share = percent(chance);
            g.text(font(), share, x + width - font().width(share), y, TEXT);
            long oneIn = Math.round(1 / chance);
            line(g, tr("rarity.oneIn", oneIn), x, y + 10, width, DIM);
            // bar on a log scale, 100 % to 1 in 100 000
            int barY = y + 20;
            g.fill(x, barY, x + width, barY + 2, 0xFF1A1A1B);
            double fill = Math.max(0, 1 + Math.log10(chance) / 5);
            g.fill(x, barY, x + (int) (width * fill), barY + 2, color);
        }

        @Override
        public int height(int width) {
            return 92;
        }
    }

    public static final class EliteRarity extends Rarity {
        @Override
        int[] rarities(int edited) {
            return new int[]{edited, Config.get().ultraRarity, Config.get().infernoRarity};
        }
    }

    public static final class UltraRarity extends Rarity {
        @Override
        int[] rarities(int edited) {
            return new int[]{Config.get().eliteRarity, edited, Config.get().infernoRarity};
        }
    }

    public static final class InfernoRarity extends Rarity {
        @Override
        int[] rarities(int edited) {
            return new int[]{Config.get().eliteRarity, Config.get().ultraRarity, edited};
        }
    }

    // ------------------------------------------------------------------ health

    /**
     * Max health of a zombie (20 health points) per tier: base health x number of modifiers x health factor.
     * Rare mobs get 2 to 4 modifiers, Ultra mobs 3 to 4 more, Infernal mobs 3 to 4 more again.
     */
    public abstract static class Health implements PreviewRenderer {

        private static final int BASE = 20;

        abstract double factor(Object value);

        abstract boolean disabled(Object value);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            line(g, tr("health.title", BASE), x, y, width, TEXT);
            if (disabled(value)) {
                line(g, tr("health.vanilla", BASE), x, y + 16, width, DIM);
                return;
            }
            double factor = factor(value);
            int max = (int) Math.ceil(BASE * 12 * factor);
            tier(g, x, y + 16, width, tr("rare"), BASE * 2 * factor, BASE * 4 * factor, max, RARE);
            tier(g, x, y + 42, width, tr("ultra"), BASE * 5 * factor, BASE * 8 * factor, max, ULTRA);
            tier(g, x, y + 68, width, tr("infernal"), BASE * 8 * factor, BASE * 12 * factor, max, INFERNAL);
        }

        private static void tier(GuiGraphicsExtractor g, int x, int y, int width, String name, double min, double max,
                                 int scale, int color) {
            line(g, name, x, y, width / 2, color);
            String range = tr("health.range", Math.round(min), Math.round(max));
            g.text(font(), range, x + width - font().width(range), y, TEXT);
            int barY = y + 11;
            g.fill(x, barY, x + width, barY + 4, 0xFF1A1A1B);
            if (scale > 0) {
                int from = (int) (width * min / scale);
                int to = Math.max(from + 1, (int) (width * max / scale));
                g.fill(x, barY, x + from, barY + 4, (color & 0x00FFFFFF) | 0x60000000);
                g.fill(x + from, barY, x + to, barY + 4, color);
            }
        }

        @Override
        public int height(int width) {
            return 88;
        }
    }

    public static final class HealthFactor extends Health {
        @Override
        double factor(Object value) {
            return ((Number) value).doubleValue();
        }

        @Override
        boolean disabled(Object value) {
            return Config.get().healthChangesDisabled;
        }
    }

    public static final class HealthDisabled extends Health {
        @Override
        double factor(Object value) {
            return Config.get().modHealthFactor;
        }

        @Override
        boolean disabled(Object value) {
            return (Boolean) value;
        }
    }

    // ------------------------------------------------------------------ outline

    private static final Identifier ZOMBIE = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final Identifier BRICKS = Identifier.withDefaultNamespace("textures/block/stone_bricks.png");
    private static final int GROUND = 0xFF3A3A3C;
    private static final int DIMMED = 0xB01A1A1B;
    private static final int DIMMED_TEXT = 0xFF4A4A4C;
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
    private static void zombie(GuiGraphicsExtractor g, int x, int y, float scale, int outline, boolean body) {
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
    private static void wall(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int clipX0, int clipY0, int clipX1, int clipY1) {
        g.enableScissor(Math.max(x0, clipX0), Math.max(y0, clipY0), Math.min(x1, clipX1), Math.min(y1, clipY1));
        for (int ty = y0; ty < y1; ty += 16) {
            for (int tx = x0; tx < x1; tx += 16) {
                g.blit(RenderPipelines.GUI_TEXTURED, BRICKS, tx, ty, 0, 0, 16, 16, 16, 16);
            }
        }
        g.disableScissor();
    }

    /** The settings of the outline, with the option being edited applied. */
    private record OutlineState(boolean enabled, boolean throughWalls, int maxDistance, boolean[] tiers) {

        static OutlineState saved() {
            Config.Outline o = Config.get().outline;
            return new OutlineState(o.enabled, o.throughWalls, o.maxDistance, new boolean[]{o.rare, o.ultra, o.infernal});
        }

        int color(int tier) {
            return enabled && tiers[tier] ? TIER_COLORS[tier] : 0;
        }

        /** Color of the highest tier that is outlined, 0 when none is. */
        int topColor() {
            for (int tier = 2; tier >= 0; tier--) {
                if (color(tier) != 0) {
                    return color(tier);
                }
            }
            return 0;
        }
    }

    private static final int[] TIER_COLORS = {RARE, ULTRA, INFERNAL};

    private static String tierName(int tier) {
        return tr(tier == 0 ? "rare" : tier == 1 ? "ultra" : "infernal");
    }

    private static void centered(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
        g.text(font(), text, centerX - font().width(text) / 2, y, color);
    }

    /**
     * One zombie per tier, with the outline it gets. The tier toggles put their tier forward and dim the two others.
     */
    private abstract static class Tiers implements PreviewRenderer {

        /** -1: every tier, else the tier of the option */
        private final int focus;

        Tiers(int focus) {
            this.focus = focus;
        }

        abstract OutlineState state(Object value);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            OutlineState state = state(value);
            int third = width / 3;
            float scale = third >= 36 ? 2 : 1.5f;
            int dollW = Math.round(16 * scale);
            int dollH = Math.round(32 * scale);
            int top = y + 4;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, GROUND);
            for (int tier = 0; tier < 3; tier++) {
                int cellX = x + tier * third;
                int color = state.color(tier);
                zombie(g, cellX + (third - dollW) / 2, top, scale, color, true);
                boolean dimmed = focus >= 0 && tier != focus;
                if (dimmed) {
                    g.fill(cellX, y, cellX + third, groundY + 2, DIMMED);
                }
                // dimmed through the color: some versions draw the text above the fills
                int labelColor = dimmed ? DIMMED_TEXT : color != 0 ? color : DIM;
                centered(g, tierName(tier), cellX + third / 2, groundY + 4, labelColor);
            }
            String footer;
            if (!state.enabled()) {
                footer = tr("outline.off");
            } else if (focus >= 0) {
                footer = tr(state.color(focus) != 0 ? "outline.shown" : "outline.hidden", tierName(focus));
            } else {
                footer = tr("outline.distance", state.maxDistance());
            }
            line(g, footer, x, groundY + 18, width, TEXT);
        }

        @Override
        public int height(int width) {
            return 4 + Math.round(32 * (width / 3 >= 36 ? 2 : 1.5f)) + 28;
        }
    }

    private static Tiers tiers(int focus, int index) {
        return new Tiers(focus) {
            @Override
            OutlineState state(Object value) {
                OutlineState saved = OutlineState.saved();
                boolean edited = (Boolean) value;
                if (index < 0) {
                    return new OutlineState(edited, saved.throughWalls(), saved.maxDistance(), saved.tiers());
                }
                boolean[] tiers = saved.tiers().clone();
                tiers[index] = edited;
                return new OutlineState(saved.enabled(), saved.throughWalls(), saved.maxDistance(), tiers);
            }
        };
    }

    public static final class OutlineEnabled implements PreviewRenderer {
        private final Tiers scene = tiers(-1, -1);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            scene.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return scene.height(width);
        }
    }

    public static final class OutlineRare implements PreviewRenderer {
        private final Tiers scene = tiers(0, 0);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            scene.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return scene.height(width);
        }
    }

    public static final class OutlineUltra implements PreviewRenderer {
        private final Tiers scene = tiers(1, 1);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            scene.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return scene.height(width);
        }
    }

    public static final class OutlineInfernal implements PreviewRenderer {
        private final Tiers scene = tiers(2, 2);

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            scene.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return scene.height(width);
        }
    }

    /**
     * Two zombies of the highest outlined tier: one in the open, one fully behind a stone brick wall. The game
     * outlines the hidden one only when the outline is visible through walls; otherwise it needs a line of sight.
     */
    public static final class OutlineThroughWalls implements PreviewRenderer {

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            OutlineState saved = OutlineState.saved();
            boolean throughWalls = (Boolean) value;
            int color = saved.topColor();
            int half = width / 2;
            float scale = half >= 40 ? 2 : 1.5f;
            int dollW = Math.round(16 * scale);
            int dollH = Math.round(32 * scale);
            int top = y + 6;
            int groundY = top + dollH;
            g.fill(x, groundY, x + width, groundY + 1, GROUND);

            // in the open: outlined with or without the option
            int openX = x + (half - dollW) / 2;
            zombie(g, openX, top, scale, color, true);
            centered(g, tr("outline.visible"), x + half / 2, groundY + 4, DIM);

            // behind the wall: the wall is drawn over the zombie, then its outline over the wall
            int hiddenX = x + half + (half - dollW) / 2;
            int wallX0 = x + half + 4;
            int wallX1 = x + width;
            int wallY0 = y;
            int clipX1 = x + width;
            int clipY1 = groundY;
            wall(g, wallX0, wallY0, wallX1, groundY, x, y, clipX1, clipY1);
            g.fill(wallX0, wallY0, wallX0 + 1, groundY, 0x60000000);
            if (throughWalls && color != 0) {
                zombie(g, hiddenX, top, scale, color, false);
                // the body stays hidden: the wall is drawn again inside the silhouette
                for (int[] p : ZOMBIE_PARTS) {
                    int[] r = part(hiddenX, top, scale, p);
                    wall(g, wallX0, wallY0, wallX1, groundY, r[0], r[1], r[2], r[3]);
                }
            }
            centered(g, tr("outline.behindWall"), x + half + half / 2, groundY + 4, DIM);

            String footer = color == 0 ? tr("outline.off") : tr(throughWalls ? "outline.walls.on" : "outline.walls.off");
            line(g, footer, x, groundY + 18, width, TEXT);
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
    public static final class OutlineDistance implements PreviewRenderer {

        private static float scale(int distance) {
            return 2.2f / (1 + distance / 16f);
        }

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            int maxDistance = ((Number) value).intValue();
            int color = OutlineState.saved().topColor();
            int cell = width / DISTANCES.length;
            int nearH = Math.round(32 * scale(DISTANCES[0]));
            int groundY = y + 4 + nearH;
            int horizonY = y + 4 + nearH / 3;
            g.fill(x, groundY, x + width, groundY + 1, GROUND);
            int cutX = -1;
            for (int i = 0; i < DISTANCES.length; i++) {
                int distance = DISTANCES[i];
                float scale = scale(distance);
                int dollW = Math.round(16 * scale);
                int dollH = Math.round(32 * scale);
                // farther mobs stand higher, towards the horizon
                float depth = (scale - scale(DISTANCES[DISTANCES.length - 1])) / (scale(DISTANCES[0]) - scale(DISTANCES[DISTANCES.length - 1]));
                int feetY = Math.round(horizonY + (groundY - horizonY) * depth);
                int cellX = x + i * cell;
                boolean outlined = distance <= maxDistance;
                zombie(g, cellX + (cell - dollW) / 2, feetY - dollH, scale, outlined ? color : 0, true);
                centered(g, String.valueOf(distance), cellX + cell / 2, groundY + 4, outlined && color != 0 ? color : DIM);
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
            line(g, footer, x, groundY + 18, width, TEXT);
        }

        @Override
        public int height(int width) {
            return 4 + Math.round(32 * scale(DISTANCES[0])) + 28;
        }
    }

    // ------------------------------------------------------------------ loot

    /**
     * The loot as a grid of item icons with their count. Before a world is loaded the item components are not bound
     * (no icon can be drawn), the loot is then listed by name.
     */
    public static final class Loot implements PreviewRenderer {

        private static final int CELL = 18;

        @Override
        public void render(GuiGraphicsExtractor g, int x, int y, int width, int height, Object value, float delta) {
            List<?> entries = (List<?>) value;
            line(g, tr("loot.title", entries.size()), x, y, width, TEXT);
            int columns = Math.max(1, width / CELL);
            int row = 0;
            int column = 0;
            int textY = y + 14;
            for (Object entry : entries) {
                Stack stack = Stack.parse(String.valueOf(entry));
                if (stack == null) {
                    continue;
                }
                if (stack.icon != null) {
                    int cx = x + column * CELL;
                    int cy = y + 14 + row * CELL;
                    g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0xFF1A1A1B);
                    g.item(stack.icon, cx, cy);
                    if (stack.count > 1) {
                        String count = String.valueOf(stack.count);
                        g.text(font(), count, cx + CELL - 1 - font().width(count), cy + CELL - 9, 0xFFFFFFFF);
                    }
                    if (++column == columns) {
                        column = 0;
                        row++;
                    }
                } else if (textY + 10 <= y + height) {
                    String name = stack.count > 1 ? stack.count + " x " + stack.name : stack.name;
                    line(g, name, x, textY, width, DIM);
                    textY += 10;
                }
            }
        }

        @Override
        public int height(int width) {
            return 14 + 10 * CELL;
        }

        private record Stack(String name, int count, ItemStack icon) {

            static Stack parse(String snbt) {
                try {
                    CompoundTag tag = TagParser.parseCompoundFully(snbt);
                    Identifier id = Identifier.tryParse(tag.getStringOr("id", ""));
                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                        return null;
                    }
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    ItemStack icon = item.builtInRegistryHolder().areComponentsBound() ? new ItemStack(item) : null;
                    String name = icon != null ? icon.getHoverName().getString()
                            : Component.translatable(item.getDescriptionId()).getString();
                    return new Stack(name, tag.getIntOr("count", 1), icon);
                } catch (CommandSyntaxException e) {
                    return null;
                }
            }
        }
    }
}
