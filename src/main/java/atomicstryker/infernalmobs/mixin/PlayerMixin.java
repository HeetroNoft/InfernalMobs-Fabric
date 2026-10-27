package atomicstryker.infernalmobs.mixin;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Player overrides LivingEntity#actuallyHurt, so the damage hook has to be applied here too
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"), argsOnly = true)
    private float infernalmobs$onHurt(float damage, ServerLevel level, DamageSource source) {
        return InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingHurt((Player) (Object) this, source, damage);
    }
}
