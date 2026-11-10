package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.Config;
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Colored outline around infernal mobs, drawn by the vanilla glowing outline pass.
 * Called from {@link atomicstryker.infernalmobs.mixin.client.LevelRendererMixin}, settings in {@link Config.Outline}
 */
public class RendererOutline {

    /**
     * returned when the mob gets no outline
     */
    public static final int NO_OUTLINE = 0;

    // the green, yellow and red of the chat colors
    private static final int RARE_COLOR = 0x55FF55;
    private static final int ULTRA_COLOR = 0xFFFF55;
    private static final int INFERNAL_COLOR = 0xFF5555;

    /**
     * @return the outline color for this mob, or {@link #NO_OUTLINE}
     */
    public static int getOutlineColor(LivingEntity mob) {
        Config.Outline settings = Config.get().outline;
        if (!settings.enabled || !mob.isAlive()) {
            return NO_OUTLINE;
        }
        MobModifier mods = InfernalMobsCore.getMobModifiers(mob);
        if (mods == null) {
            return NO_OUTLINE;
        }

        // same rarity thresholds as the loot drops: Rare up to 5 modifiers, Ultra up to 10, Infernal above
        int size = mods.getModSize();
        boolean shown = size <= 5 ? settings.rare : size <= 10 ? settings.ultra : settings.infernal;
        int color = size <= 5 ? RARE_COLOR : size <= 10 ? ULTRA_COLOR : INFERNAL_COLOR;
        if (!shown) {
            return NO_OUTLINE;
        }

        Entity viewer = Minecraft.getInstance().getCameraEntity();
        if (viewer == null || viewer.distanceToSqr(mob) > (double) settings.maxDistance * settings.maxDistance) {
            return NO_OUTLINE;
        }
        if (!settings.throughWalls && !(viewer instanceof LivingEntity living && living.hasLineOfSight(mob))) {
            return NO_OUTLINE;
        }
        return 0xFF000000 | color;
    }
}
