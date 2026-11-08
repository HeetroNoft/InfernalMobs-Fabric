package atomicstryker.infernalmobs.mixin.client;

import atomicstryker.infernalmobs.client.RendererOutline;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    /**
     * Entities in chunk sections hidden by the occlusion culling are not rendered at all, so their outline
     * disappears. Outlined infernal mobs skip that check when their outline is visible through walls.
     */
    @WrapOperation(method = "isEntityVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean infernalmobs$keepOutlinedMobs(LevelRenderer levelRenderer, BlockPos pos, Operation<Boolean> original, @Local(argsOnly = true) Entity entity) {
        return original.call(levelRenderer, pos) || RendererOutline.isOutlinedThroughWalls(entity);
    }
}
