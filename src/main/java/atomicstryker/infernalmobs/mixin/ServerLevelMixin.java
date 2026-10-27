package atomicstryker.infernalmobs.mixin;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * post entity tick hook, for both standalone entities and passengers
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void infernalmobs$afterTick(Entity entity, CallbackInfo ci) {
        InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingUpdate(entity);
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V", shift = At.Shift.AFTER))
    private void infernalmobs$afterRideTick(Entity vehicle, Entity entity, CallbackInfo ci) {
        InfernalMobsCore.instance().getEntityEventHandler().onEntityLivingUpdate(entity);
    }
}
