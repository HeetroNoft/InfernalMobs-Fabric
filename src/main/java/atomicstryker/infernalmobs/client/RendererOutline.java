package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.Config;
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Colored outline around infernal mobs, drawn by the vanilla glowing outline pass.
 * Called from {@link atomicstryker.infernalmobs.mixin.client.EntityRendererMixin}, settings in {@link Config.Outline}
 */
public class RendererOutline {

    // the green, yellow and red of the chat colors
    private static final int RARE_COLOR = 0x55FF55;
    private static final int ULTRA_COLOR = 0xFFFF55;
    private static final int INFERNAL_COLOR = 0xFF5555;

    /**
     * @return true if the entity gets an outline that must stay visible behind blocks
     */
    public static boolean isOutlinedThroughWalls(Entity entity) {
        return Config.get().outline.throughWalls && entity instanceof LivingEntity mob
                && getOutlineColor(mob) != EntityRenderState.NO_OUTLINE;
    }

    /**
     * @return the outline color for this mob, or {@link EntityRenderState#NO_OUTLINE}
     */
    public static int getOutlineColor(LivingEntity mob) {
        Config.Outline settings = Config.get().outline;
        if (!settings.enabled || !mob.isAlive()) {
            return EntityRenderState.NO_OUTLINE;
        }
        MobModifier mods = InfernalMobsCore.getMobModifiers(mob);
        if (mods == null) {
            return EntityRenderState.NO_OUTLINE;
        }

        // same rarity thresholds as the loot drops: Rare up to 5 modifiers, Ultra up to 10, Infernal above
        int size = mods.getModSize();
        boolean shown = size <= 5 ? settings.rare : size <= 10 ? settings.ultra : settings.infernal;
        int color = size <= 5 ? RARE_COLOR : size <= 10 ? ULTRA_COLOR : INFERNAL_COLOR;
        if (!shown) {
            return EntityRenderState.NO_OUTLINE;
        }

        Entity viewer = Minecraft.getInstance().getCameraEntity();
        if (viewer == null || viewer.distanceToSqr(mob) > (double) settings.maxDistance * settings.maxDistance) {
            return EntityRenderState.NO_OUTLINE;
        }
        if (!settings.throughWalls && !(viewer instanceof LivingEntity living && living.hasLineOfSight(mob))) {
            return EntityRenderState.NO_OUTLINE;
        }
        return ARGB.opaque(color);
    }
}
