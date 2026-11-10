package atomicstryker.infernalmobs.mixin.client;

import atomicstryker.infernalmobs.client.RendererOutline;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Colored outline around infernal mobs, drawn by the vanilla glowing outline pass.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean infernalmobs$outlineInfernalMobs(Minecraft minecraft, Entity entity, Operation<Boolean> original) {
        return original.call(minecraft, entity)
                || (entity instanceof LivingEntity mob && RendererOutline.getOutlineColor(mob) != RendererOutline.NO_OUTLINE);
    }

    /**
     * Mobs already glowing (Glowing effect, spectral arrow) keep their vanilla team color outline
     */
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int infernalmobs$outlineColor(Entity entity, Operation<Integer> original) {
        if (entity instanceof LivingEntity mob && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) {
            int color = RendererOutline.getOutlineColor(mob);
            if (color != RendererOutline.NO_OUTLINE) {
                return color;
            }
        }
        return original.call(entity);
    }
}
