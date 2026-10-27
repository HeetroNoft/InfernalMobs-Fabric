package atomicstryker.infernalmobs.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map.Entry;

public class EntityEventHandler {

    private final HashMap<Integer[], Float> damageMap = new HashMap<>();
    private long nextMapEvaluation;

    /**
     * Links the Fabric events and mixin hooks to the registered Entity MobModifier Events
     * (if present) Also keeps track of the anti mobfarm mechanic if enabled
     */
    public EntityEventHandler() {
        nextMapEvaluation = System.currentTimeMillis();
    }

    public void onEntityJoinedWorld(Entity entity) {
        // make sure we are not catching items or player entities in this
        if (entity instanceof LivingEntity livingEntity && entity instanceof Enemy) {
            String savedMods = InfernalMobsData.getSavedMods(livingEntity);
            if (!savedMods.isEmpty() && !savedMods.equals(InfernalMobsCore.instance().getNBTMarkerForNonInfernalEntities())) {
                InfernalMobsCore.instance().addEntityModifiersByString(livingEntity, savedMods);
            } else {
                InfernalMobsCore.instance().processEntitySpawn(livingEntity);
            }
        }
    }

    /**
     * @return true if the death should be aborted
     */
    public boolean onEntityLivingDeath(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                return mod.onDeath();
            }
        }
        return false;
    }

    public void onEntityLivingSetAttackTarget(LivingEntity entity, LivingEntity newTarget) {
        if (!entity.level().isClientSide()) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                mod.onSetAttackTarget(entity, newTarget);
            }
        }
    }

    /**
     * Hook into LivingEntity#actuallyHurt. Is always serverside, assured by mc itself
     *
     * @return damage to be applied after the modifiers processed it
     */
    public float onEntityLivingHurt(LivingEntity entity, DamageSource source, float damage) {
        // dont allow masochism
        if (source.getDirectEntity() != entity) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                // note we dont use original damage so mods can chain modify
                damage = mod.onHurt(entity, source, damage);
            }

            /*
             * We use the Hook two-sided, both with the Mob as possible target
             * and attacker
             */
            Entity attacker = source.getDirectEntity();
            if (attacker == null) {
                // if damage does not have a defined source, ignore it
                return damage;
            }
            if (attacker instanceof LivingEntity) {
                mod = InfernalMobsCore.getMobModifiers((LivingEntity) attacker);
                if (mod != null) {
                    damage = mod.onAttack(entity, source, damage);
                }
            }

            if (Config.get().antiMobFarm) {
                /*
                 * check for an environmental/automated damage type, aka mob farms. FakePlayer is gone so we check for
                 * Player i guess
                 */
                if (source == attacker.damageSources().cactus() || source == attacker.damageSources().drown() || source == attacker.damageSources().fall() || source == attacker.damageSources().inWall()
                        || source == attacker.damageSources().lava() || source.getDirectEntity() instanceof Player) {
                    Integer[] cpair = new Integer[]{(int) entity.getX(), (int) entity.getZ()};
                    Float value = damageMap.get(cpair);
                    if (value == null) {
                        for (Entry<Integer[], Float> e : damageMap.entrySet()) {
                            if (Math.abs(e.getKey()[0] - cpair[0]) < 3) {
                                if (Math.abs(e.getKey()[1] - cpair[1]) < 3) {
                                    e.setValue(e.getValue() + damage);
                                    break;
                                }
                            }
                        }
                    } else {
                        damageMap.put(cpair, value + damage);
                    }
                }
            }
        }
        return damage;
    }

    /**
     * Hook into LivingEntity#knockback
     *
     * @return true if the knockback should be canceled
     */
    public boolean onEntityLivingKnockback(LivingEntity entity) {
        MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
        return mod != null && mod.onKnockBack();
    }

    /**
     * @return true if the fall damage should be canceled
     */
    public boolean onEntityLivingFall(LivingEntity entity, double distance) {
        if (!entity.level().isClientSide()) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                return mod.onFall(distance);
            }
        }
        return false;
    }

    public void onEntityLivingJump(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                mod.onJump(entity);
            }
        }
    }

    public void onEntityLivingUpdate(Entity entity) {
        if (!entity.level().isClientSide() && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            // workaround to get save-loaded infernal entities working, init them on their first living tick
            if (entity.tickCount == 1) {
                String savedMods = InfernalMobsData.getSavedMods(livingEntity);
                if (!savedMods.isEmpty() && !savedMods.equals(InfernalMobsCore.instance().getNBTMarkerForNonInfernalEntities())) {
                    InfernalMobsCore.instance().addEntityModifiersByString(livingEntity, savedMods);
                }
            }

            MobModifier mod = InfernalMobsCore.getMobModifiers(livingEntity);
            if (mod != null) {
                mod.onUpdate(livingEntity);
            }

            if (Config.get().antiMobFarm && System.currentTimeMillis() > nextMapEvaluation) {
                if (!damageMap.isEmpty()) {
                    float maxDamage = 0f;
                    float val;
                    Integer[] maxC = null;
                    for (Entry<Integer[], Float> e : damageMap.entrySet()) {
                        val = e.getValue();
                        if (val > maxDamage) {
                            maxC = e.getKey();
                            maxDamage = val;
                        }
                    }

                    if (maxC != null) {
                        System.out.println("Infernal Mobs AntiMobFarm damage check, max detected chunk damage value " + maxDamage + " near coords " + maxC[0] + ", " + maxC[1]);
                        if (maxDamage > Config.get().mobFarmDamageTrigger) {
                            InfernalMobsEvents.MOB_FARM_DETECTED.invoker().onMobFarmDetected(entity.level().getChunk(maxC[0], maxC[1]), Config.get().mobFarmCheckIntervals, maxDamage);
                        }
                    }
                    damageMap.clear();
                }
                nextMapEvaluation = System.currentTimeMillis() + Config.get().mobFarmCheckIntervals;
            }
        }
    }

    public void onEntityLivingDrops(LivingEntity entity, DamageSource source, boolean recentlyHit) {
        if (!entity.level().isClientSide()) {
            MobModifier mod = InfernalMobsCore.getMobModifiers(entity);
            if (mod != null) {
                mod.onDropItems(entity, source, recentlyHit);
                InfernalMobsCore.removeEntFromElites(entity);
            }
        }
    }
}
