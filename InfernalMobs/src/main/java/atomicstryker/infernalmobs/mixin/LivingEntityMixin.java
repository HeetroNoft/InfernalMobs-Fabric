package atomicstryker.infernalmobs.mixin;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
    private void infernalmobs$onKnockback(CallbackInfo ci) {
        if (InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingKnockback((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void infernalmobs$onFall(double fallDistance, float damageModifier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingFall((LivingEntity) (Object) this, fallDistance)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void infernalmobs$onJump(CallbackInfo ci) {
        InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingJump((LivingEntity) (Object) this);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void infernalmobs$onDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingDrops((LivingEntity) (Object) this, source, lastHurtByPlayerMemoryTime > 0);
    }

    /**
     * modifies the damage after armor, enchantments and absorption, right before it is applied to health
     */
    @ModifyVariable(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"), argsOnly = true)
    private float infernalmobs$onHurt(float damage, ServerLevel level, DamageSource source) {
        return InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingHurt((LivingEntity) (Object) this, source, damage);
    }
}
