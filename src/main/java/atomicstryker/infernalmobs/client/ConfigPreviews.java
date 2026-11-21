package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.Config;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.heetstudio.confee.api.PreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final int MOB = 0xFF3B5E3B;
    private static final int MOB_DARK = 0xFF2A452A;
    private static final int WALL = 0xFF6E6E70;
    private static final int WALL_DARK = 0xFF58585A;
    private static final String KEY = "infernalmobs.config.preview.";

    private ConfigPreviews() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(KEY + key, args).getString();
    }

    private static void line(GuiGraphics g, String text, int x, int y, int width, int color) {
        g.drawString(font(), font().plainSubstrByWidth(text, width), x, y, color);
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
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
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

        private static void tier(GuiGraphics g, int x, int y, int width, String name, double chance, int color) {
            line(g, name, x, y, width / 2, color);
            String share = percent(chance);
            g.drawString(font(), share, x + width - font().width(share), y, TEXT);
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
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
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

        private static void tier(GuiGraphics g, int x, int y, int width, String name, double min, double max,
                                 int scale, int color) {
            line(g, name, x, y, width / 2, color);
            String range = tr("health.range", Math.round(min), Math.round(max));
            g.drawString(font(), range, x + width - font().width(range), y, TEXT);
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

    /**
     * Three mobs, one per tier, with the outline they get. The "through walls" preview puts a wall in front of them.
     */
    public abstract static class Outline implements PreviewRenderer {

        /** enabled, throughWalls, rare, ultra, infernal, with the edited value applied */
        abstract boolean[] flags(Object value);

        boolean showWall() {
            return false;
        }

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            boolean[] f = flags(value);
            int mobW = 12;
            int third = width / 3;
            int[] colors = {RARE, ULTRA, INFERNAL};
            String[] names = {tr("rare"), tr("ultra"), tr("infernal")};
            int mobY = y + 6;
            for (int i = 0; i < 3; i++) {
                int mx = x + i * third + (third - mobW) / 2;
                boolean outlined = f[0] && f[2 + i];
                mob(g, mx, mobY, outlined ? colors[i] : 0);
                String name = names[i];
                g.drawString(font(), name, mx + mobW / 2 - font().width(name) / 2, mobY + 40, outlined ? colors[i] : DIM);
            }
            if (showWall()) {
                // a wall hiding the legs: the outline shows through it like the Glowing effect
                int wallY = mobY + 18;
                g.fill(x, wallY, x + width, wallY + 18, WALL);
                for (int bx = x; bx < x + width; bx += 16) {
                    g.fill(bx, wallY, bx + 1, wallY + 18, WALL_DARK);
                }
                g.fill(x, wallY + 8, x + width, wallY + 9, WALL_DARK);
                if (f[1]) {
                    for (int i = 0; i < 3; i++) {
                        if (f[0] && f[2 + i]) {
                            int mx = x + i * third + (third - mobW) / 2;
                            outlineOnly(g, mx, mobY, colors[i], wallY);
                        }
                    }
                }
            }
            line(g, f[0] ? footer(value) : tr("outline.off"), x, y + height - 10, width, DIM);
        }

        String footer(Object value) {
            return tr("outline.distance", Config.get().outline.maxDistance);
        }

        @Override
        public int height(int width) {
            return 72;
        }

        /** A small humanoid: head, body, legs. */
        private static void mob(GuiGraphics g, int x, int y, int outline) {
            if (outline != 0) {
                g.fill(x + 1, y - 1, x + 11, y + 37, outline);
                g.fill(x - 1, y + 9, x + 13, y + 23, outline);
            }
            g.fill(x + 2, y, x + 10, y + 8, MOB);
            g.fill(x, y + 10, x + 12, y + 22, MOB_DARK);
            g.fill(x + 2, y + 22, x + 10, y + 36, MOB);
            g.fill(x + 5, y + 22, x + 7, y + 36, MOB_DARK);
        }

        /** The outline of the hidden part, drawn over the wall. */
        private static void outlineOnly(GuiGraphics g, int x, int y, int color, int fromY) {
            int top = Math.max(fromY, y + 18);
            g.fill(x + 1, top, x + 2, y + 37, color);
            g.fill(x + 10, top, x + 11, y + 37, color);
            g.fill(x + 1, y + 36, x + 11, y + 37, color);
            g.fill(x - 1, top, x, y + 23, color);
            g.fill(x + 12, top, x + 13, y + 23, color);
        }
    }

    private static boolean[] savedOutline() {
        Config.Outline o = Config.get().outline;
        return new boolean[]{o.enabled, o.throughWalls, o.rare, o.ultra, o.infernal};
    }

    private static Outline outlineFlag(int index, boolean wall) {
        return new Outline() {
            @Override
            boolean[] flags(Object value) {
                boolean[] flags = savedOutline();
                flags[index] = (Boolean) value;
                return flags;
            }

            @Override
            boolean showWall() {
                return wall;
            }
        };
    }

    public static final class OutlineEnabled implements PreviewRenderer {
        private final Outline outline = outlineFlag(0, false);

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    public static final class OutlineThroughWalls implements PreviewRenderer {
        private final Outline outline = outlineFlag(1, true);

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    public static final class OutlineRare implements PreviewRenderer {
        private final Outline outline = outlineFlag(2, false);

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    public static final class OutlineUltra implements PreviewRenderer {
        private final Outline outline = outlineFlag(3, false);

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    public static final class OutlineInfernal implements PreviewRenderer {
        private final Outline outline = outlineFlag(4, false);

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    public static final class OutlineDistance implements PreviewRenderer {
        private final Outline outline = new Outline() {
            @Override
            boolean[] flags(Object value) {
                return savedOutline();
            }

            @Override
            String footer(Object value) {
                return tr("outline.distance", ((Number) value).intValue());
            }
        };

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
            outline.render(g, x, y, width, height, value, delta);
        }

        @Override
        public int height(int width) {
            return outline.height(width);
        }
    }

    // ------------------------------------------------------------------ loot

    /**
     * The loot as a grid of item icons with their count.
     */
    public static final class Loot implements PreviewRenderer {

        private static final int CELL = 18;

        @Override
        public void render(GuiGraphics g, int x, int y, int width, int height, Object value, float delta) {
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
                    g.renderItem(stack.icon, cx, cy);
                    if (stack.count > 1) {
                        String count = String.valueOf(stack.count);
                        g.drawString(font(), count, cx + CELL - 1 - font().width(count), cy + CELL - 9, 0xFFFFFFFF);
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
                    CompoundTag tag = TagParser.parseTag(snbt);
                    ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                        return null;
                    }
                    Item item = BuiltInRegistries.ITEM.get(id);
                    ItemStack icon = new ItemStack(item);
                    String name = icon != null ? icon.getHoverName().getString()
                            : Component.translatable(item.getDescriptionId()).getString();
                    return new Stack(name, tag.contains("count") ? tag.getInt("count") : 1, icon);
                } catch (CommandSyntaxException e) {
                    return null;
                }
            }
        }
    }
}
