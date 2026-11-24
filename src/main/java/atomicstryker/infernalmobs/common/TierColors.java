package atomicstryker.infernalmobs.common;

import net.minecraft.world.BossEvent;

/**
 * The color of each tier (Rare, Ultra, Infernal), set in the config: used for the outline, the name and the boss bar.
 */
public final class TierColors {

    public static final int RARE = 0;
    public static final int ULTRA = 1;
    public static final int INFERNAL = 2;

    // the green, yellow and red of the chat colors
    public static final int DEFAULT_RARE = 0x55FF55;
    public static final int DEFAULT_ULTRA = 0xFFFF55;
    public static final int DEFAULT_INFERNAL = 0xFF5555;

    /** Hue (degrees) of the 7 boss bar colors, measured on their sprites. */
    private static final BossEvent.BossBarColor[] BAR_COLORS = {
            BossEvent.BossBarColor.RED, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.PINK};
    private static final float[] BAR_HUES = {13, 61, 113, 193, 291, 313};

    private TierColors() {
    }

    /** Tier of a mob from its number of modifiers: Rare up to 5, Ultra up to 10, Infernal above (as the loot). */
    public static int tier(int modifiers) {
        return modifiers <= 5 ? RARE : modifiers <= 10 ? ULTRA : INFERNAL;
    }

    /** RGB color of a tier, from the config. */
    public static int rgb(int tier) {
        Config config = Config.get();
        int color = tier == RARE ? config.rareColor : tier == ULTRA ? config.ultraColor : config.infernalColor;
        return color & 0xFFFFFF;
    }

    /**
     * The boss bar color closest to an RGB color: the nearest hue, white when the color is close to gray.
     */
    public static BossEvent.BossBarColor bossBar(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        if (max == 0 || (max - min) / max < 0.25f) {
            return BossEvent.BossBarColor.WHITE;
        }
        float delta = max - min;
        float hue;
        if (max == r) {
            hue = 60 * (((g - b) / delta) % 6);
        } else if (max == g) {
            hue = 60 * ((b - r) / delta + 2);
        } else {
            hue = 60 * ((r - g) / delta + 4);
        }
        if (hue < 0) {
            hue += 360;
        }
        BossEvent.BossBarColor best = BossEvent.BossBarColor.WHITE;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < BAR_HUES.length; i++) {
            float distance = Math.abs(hue - BAR_HUES[i]);
            distance = Math.min(distance, 360 - distance);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = BAR_COLORS[i];
            }
        }
        return best;
    }
}
