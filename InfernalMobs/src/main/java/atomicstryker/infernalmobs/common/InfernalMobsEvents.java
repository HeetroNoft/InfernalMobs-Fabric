package atomicstryker.infernalmobs.common;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Events Infernal Mobs exposes to other mods
 */
public final class InfernalMobsEvents {

    /**
     * Fired when the AntiMobFarm check finds a chunk exceeding the configured damage trigger
     */
    public static final Event<MobFarmDetected> MOB_FARM_DETECTED = EventFactory.createArrayBacked(MobFarmDetected.class,
            listeners -> (chunk, triggeringInterval, triggeringDamage) -> {
                for (MobFarmDetected listener : listeners) {
                    listener.onMobFarmDetected(chunk, triggeringInterval, triggeringDamage);
                }
            });

    /**
     * Fired before an infernal mob modifier teleports a mob. Listeners may return a different target position,
     * or null to cancel the teleport
     */
    public static final Event<EntityTeleport> ENTITY_TELEPORT = EventFactory.createArrayBacked(EntityTeleport.class,
            listeners -> (mob, level, target) -> {
                for (EntityTeleport listener : listeners) {
                    target = listener.onTeleport(mob, level, target);
                    if (target == null) {
                        return null;
                    }
                }
                return target;
            });

    private InfernalMobsEvents() {
    }

    @FunctionalInterface
    public interface MobFarmDetected {
        void onMobFarmDetected(LevelChunk chunk, long triggeringInterval, float triggeringDamage);
    }

    @FunctionalInterface
    public interface EntityTeleport {
        /**
         * @return the position to teleport to, or null to cancel the teleport
         */
        Vec3 onTeleport(LivingEntity mob, ServerLevel level, Vec3 target);
    }
}
