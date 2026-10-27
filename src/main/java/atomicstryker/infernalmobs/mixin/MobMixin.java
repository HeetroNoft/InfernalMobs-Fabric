package atomicstryker.infernalmobs.mixin;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void infernalmobs$onSetTarget(LivingEntity target, CallbackInfo ci) {
        InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingSetAttackTarget((Mob) (Object) this, target);
    }
}
