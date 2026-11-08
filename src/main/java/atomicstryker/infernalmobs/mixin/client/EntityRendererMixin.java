package atomicstryker.infernalmobs.mixin.client;

import atomicstryker.infernalmobs.client.RendererOutline;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    /**
     * Mobs already glowing (Glowing effect, spectral arrow) keep their vanilla team color outline
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void infernalmobs$addOutline(T entity, S state, float partialTick, CallbackInfo ci) {
        if (state.outlineColor == EntityRenderState.NO_OUTLINE && entity instanceof LivingEntity mob) {
            state.outlineColor = RendererOutline.getOutlineColor(mob);
        }
    }

    /**
     * Mobs outlined through walls are not culled, so the outline stays visible behind blocks.
     * Culling mods such as Entity Culling skip the entities their renderer reports as not affected by culling.
     */
    @Inject(method = "affectedByCulling", at = @At("HEAD"), cancellable = true)
    private void infernalmobs$keepOutlinedMobs(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (RendererOutline.isOutlinedThroughWalls(entity)) {
            cir.setReturnValue(false);
        }
    }
}
